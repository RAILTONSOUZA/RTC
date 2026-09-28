package br.com.comabel.reforma.service;

import br.com.comabel.reforma.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import br.com.comabel.reforma.repository.ItemAuditoriaRepository;
import br.com.comabel.reforma.repository.NotaFiscalRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;
import java.util.zip.ZipInputStream;

@Service
public class XmlAuditorService {

    @Autowired
    private RegrasTributariasService regrasService;

    @Autowired
    private NotaFiscalRepository notaFiscalRepository;

    @Autowired
    private ItemAuditoriaRepository itemAuditoriaRepository;

    @Autowired
    private SpedService spedService;

    private final List<NotaFiscalResumo> notasEmMemoria = new ArrayList<>();
    private final List<ItemAuditoria> itensEmMemoria = new ArrayList<>();
    private final List<MatrizUniformidadeNcm> matrizUniformidade = new ArrayList<>();
    private final Map<String, NotaFiscalResumo> notasMap = new LinkedHashMap<>();
    private final Set<String> chavesCanceladas = new HashSet<>();
    private final KpiDashboard kpiDashboard = new KpiDashboard();
    private final Map<String, EmpresaAuditada> empresasDetectadas = new LinkedHashMap<>();
    private String cnpjAtivo = "TODAS";
    private String ultimaPastaEscaneada = "";
    private RelatorioCarga ultimoRelatorio = new RelatorioCarga();
    private RelatorioCarga relatorioEmProgresso;

    private String cnpjInformado = "";

    @PostConstruct
    public void carregarDadosIniciais() {
        try {
            List<NotaFiscalResumo> salvas = notaFiscalRepository.findAll();
            if (!salvas.isEmpty()) {
                notasMap.clear();
                chavesCanceladas.clear();
                empresasDetectadas.clear();
                for (NotaFiscalResumo nf : salvas) {
                    notasMap.put(nf.getChNFe(), nf);
                    if (nf.isCancelada()) {
                        chavesCanceladas.add(nf.getChNFe());
                    }
                }

                // Detecta com inteligência as empresas auditadas (não fornecedores pontuais)
                detectarEmpresasPrincipais(salvas);

                sincronizarColecoesMemoria();
                atualizarKpis();
                this.ultimaPastaEscaneada = "Banco de Dados Local (" + salvas.size() + " notas restauradas)";
                RelatorioCarga rel = new RelatorioCarga("Banco de Dados Local");
                for (NotaFiscalResumo n : salvas) {
                    rel.incrementarExaminados();
                    rel.registrarNota(n.getModelo());
                    if (n.isCancelada()) rel.registrarCancelada();
                }
                this.ultimoRelatorio = rel;
                System.out.println("[BANCO LOCAL H2] Inicialização: " + salvas.size() + " notas restauradas do banco local. " + empresasDetectadas.size() + " empresa(s) auditada(s) identificada(s).");
            } else {
                empresasDetectadas.clear();
                atualizarKpis();
                this.ultimoRelatorio = new RelatorioCarga("Nenhum arquivo carregado");
            }
        } catch (Exception e) {
            System.err.println("[BANCO LOCAL H2] Aviso ao restaurar dados do banco local: " + e.getMessage());
        }
    }

    private void detectarEmpresasPrincipais(List<NotaFiscalResumo> notas) {
        if (notas.isEmpty()) return;
        Map<String, Integer> freqRaiz = new HashMap<>();
        for (NotaFiscalResumo nf : notas) {
            String emitClean = (nf.getEmitCnpj() != null) ? nf.getEmitCnpj().replaceAll("\\D", "") : "";
            String destClean = (nf.getDestCnpj() != null) ? nf.getDestCnpj().replaceAll("\\D", "") : "";
            if (emitClean.length() >= 8) {
                String r = emitClean.substring(0, 8);
                freqRaiz.put(r, freqRaiz.getOrDefault(r, 0) + 1);
            }
            if (destClean.length() >= 8) {
                String r = destClean.substring(0, 8);
                freqRaiz.put(r, freqRaiz.getOrDefault(r, 0) + 1);
            }
        }

        // Uma empresa auditada é estrutural: representa pelo menos 10% das notas ou > 50 notas
        int threshold = Math.max(10, (int) (notas.size() * 0.10));
        for (Map.Entry<String, Integer> entry : freqRaiz.entrySet()) {
            if (entry.getValue() >= threshold) {
                EmpresaAuditada emp = empresasDetectadas.computeIfAbsent(entry.getKey(), k -> new EmpresaAuditada());
                emp.setCnpjRaiz(entry.getKey());
                buscarDadosEmpresaNasNotas(emp, entry.getKey());
            }
        }

        // Se nenhuma atingiu threshold, elege a principal por volume
        if (empresasDetectadas.isEmpty() && !freqRaiz.isEmpty()) {
            String topRaiz = freqRaiz.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();
            EmpresaAuditada emp = empresasDetectadas.computeIfAbsent(topRaiz, k -> new EmpresaAuditada());
            emp.setCnpjRaiz(topRaiz);
            buscarDadosEmpresaNasNotas(emp, topRaiz);
        }

        // Se houver apenas 1 empresa detectada, define-a automaticamente como ativa
        if (empresasDetectadas.size() == 1) {
            this.cnpjAtivo = empresasDetectadas.keySet().iterator().next();
        }
    }

    private void buscarDadosEmpresaNasNotas(EmpresaAuditada emp, String raiz) {
        int emitidas = 0;
        int recebidas = 0;
        for (NotaFiscalResumo nf : notasMap.values()) {
            String emitClean = (nf.getEmitCnpj() != null) ? nf.getEmitCnpj().replaceAll("\\D", "") : "";
            String destClean = (nf.getDestCnpj() != null) ? nf.getDestCnpj().replaceAll("\\D", "") : "";

            if (emitClean.startsWith(raiz)) {
                emitidas++;
                emp.atualizarComDados(nf.getEmitNome(), nf.getEmitCnpj(), nf.getEmitUf(), null, nf.getDhEmi());
            } else if (destClean.startsWith(raiz)) {
                recebidas++;
                emp.atualizarComDados(nf.getDestNome(), nf.getDestCnpj(), nf.getDestUf(), null, nf.getDhEmi());
            }
        }
        emp.setTotalNotasEmitidas(emitidas);
        emp.setTotalNotasRecebidas(recebidas);
    }

    public synchronized void salvarNoBancoLocal() {
        salvarNoBancoLocal(false);
    }

    public synchronized void salvarNoBancoLocal(boolean substituirBase) {
        try {
            if (substituirBase) {
                itemAuditoriaRepository.deleteAllInBatch();
                notaFiscalRepository.deleteAllInBatch();
                itemAuditoriaRepository.flush();
                notaFiscalRepository.flush();
            }
            if (!notasMap.isEmpty()) {
                notaFiscalRepository.saveAll(notasMap.values());
                System.out.println("[BANCO LOCAL H2] Persistidas " + notasMap.size() + " notas no banco de dados local.");
            }
        } catch (Exception e) {
            System.err.println("[BANCO LOCAL H2] Aviso ao salvar no banco local: " + e.getMessage());
        }
    }

    @Transactional
    public synchronized void limparBaseDados() {
        try {
            itemAuditoriaRepository.deleteAllInBatch();
            notaFiscalRepository.deleteAllInBatch();
            itemAuditoriaRepository.flush();
            notaFiscalRepository.flush();
        } catch (Exception e) {
            System.err.println("[BANCO LOCAL H2] Erro ao limpar tabelas em lote: " + e.getMessage());
            try {
                itemAuditoriaRepository.deleteAll();
                notaFiscalRepository.deleteAll();
            } catch (Exception ex) {
                System.err.println("[BANCO LOCAL H2] Erro no fallback deleteAll: " + ex.getMessage());
            }
        }
        limparMemoria();
        empresasDetectadas.clear();
        cnpjAtivo = "TODAS";
        cnpjInformado = "";
        if (spedService != null) {
            spedService.limpar();
        }
        this.ultimaPastaEscaneada = "";
        this.ultimoRelatorio = new RelatorioCarga("Nenhum arquivo carregado");
        this.relatorioEmProgresso = null;
        atualizarKpis();
        System.out.println("[BANCO LOCAL H2] Base de dados e memória limpas pelo usuário (100% zerado).");
    }

    public synchronized void auditarDiretorio(String caminhoPasta) {
        File pasta = new File(caminhoPasta);
        if (!pasta.exists() || !pasta.isDirectory()) {
            throw new IllegalArgumentException("Diretório não encontrado: " + caminhoPasta);
        }
        ultimaPastaEscaneada = caminhoPasta;
        limparMemoria();

        RelatorioCarga relatorio = new RelatorioCarga("Diretório: " + caminhoPasta);
        this.relatorioEmProgresso = relatorio;

        List<File> arquivosXml = new ArrayList<>();
        List<File> arquivosZip = new ArrayList<>();
        coletarArquivos(pasta, arquivosXml, arquivosZip);

        // 1. Processar todos os arquivos XML soltos
        for (File f : arquivosXml) {
            relatorio.incrementarExaminados();
            try (InputStream is = new FileInputStream(f)) {
                NotaFiscalResumo nf = parseAndAudit(is, f.getName());
                if (nf != null) {
                    adicionarNota(nf, relatorio);
                }
            } catch (Exception e) {
                relatorio.registrarErro("Erro ao processar " + f.getName() + ": " + e.getMessage());
            }
        }

        // 2. Processar todos os arquivos ZIP (incluindo subpastas e zips aninhados)
        for (File z : arquivosZip) {
            relatorio.incrementarZipsLidos();
            try (InputStream is = new FileInputStream(z)) {
                byte[] bytes = is.readAllBytes();
                processarZipBytes(bytes, z.getName(), relatorio);
            } catch (Exception e) {
                relatorio.registrarErro("Erro no ZIP " + z.getName() + ": " + e.getMessage());
            }
        }

        sincronizarColecoesMemoria();
        atualizarKpis();
        this.ultimoRelatorio = relatorio;
        salvarNoBancoLocal(true);
    }

    public synchronized void auditarUpload(List<MultipartFile> arquivos) {
        auditarUpload(arquivos, false);
    }

    public synchronized void auditarUpload(List<MultipartFile> arquivos, boolean acumular) {
        if (!acumular) {
            limparMemoria();
        }
        ultimaPastaEscaneada = "Upload Manual (" + arquivos.size() + " arquivo(s))";
        RelatorioCarga relatorio = new RelatorioCarga("Upload Manual");
        this.relatorioEmProgresso = relatorio;

        for (MultipartFile file : arquivos) {
            if (file.isEmpty()) continue;
            String nome = file.getOriginalFilename();
            if (nome == null) continue;
            relatorio.incrementarExaminados();

            try {
                if (nome.toLowerCase().endsWith(".zip")) {
                    relatorio.incrementarZipsLidos();
                    byte[] zipBytes = file.getBytes();
                    processarZipBytes(zipBytes, nome, relatorio);
                } else if (nome.toLowerCase().endsWith(".xml")) {
                    NotaFiscalResumo nf = parseAndAudit(file.getInputStream(), nome);
                    if (nf != null) {
                        adicionarNota(nf, relatorio);
                    }
                }
            } catch (Exception e) {
                relatorio.registrarErro("Erro no upload de " + nome + ": " + e.getMessage());
            }
        }

        sincronizarColecoesMemoria();
        atualizarKpis();
        this.ultimoRelatorio = relatorio;
        salvarNoBancoLocal(!acumular);
    }

    private void coletarArquivos(File dir, List<File> xmlFiles, List<File> zipFiles) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().equals(".git") && !f.getName().startsWith(".")) {
                    coletarArquivos(f, xmlFiles, zipFiles);
                }
            } else {
                String nome = f.getName().toLowerCase();
                if (nome.endsWith(".xml")) {
                    xmlFiles.add(f);
                } else if (nome.endsWith(".zip")) {
                    zipFiles.add(f);
                }
            }
        }
    }

    private void processarZipBytes(byte[] zipBytes, String nomeZip, RelatorioCarga relatorio) {
        try {
            processarZipComCharset(zipBytes, StandardCharsets.UTF_8, relatorio);
        } catch (IllegalArgumentException e) {
            // Se o ZIP contiver nomes de arquivos em padrão OEM/CP437 do Windows
            try {
                processarZipComCharset(zipBytes, Charset.forName("CP437"), relatorio);
            } catch (Exception ex) {
                try {
                    processarZipComCharset(zipBytes, StandardCharsets.ISO_8859_1, relatorio);
                } catch (Exception e2) {
                    relatorio.registrarErro("Falha crítica no ZIP " + nomeZip + ": " + e2.getMessage());
                }
            }
        } catch (Exception e) {
            relatorio.registrarErro("Erro ao descompactar " + nomeZip + ": " + e.getMessage());
        }
    }

    private void processarZipComCharset(byte[] zipBytes, Charset charset, RelatorioCarga relatorio) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes), charset)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String nome = entry.getName().toLowerCase();
                    if (nome.endsWith(".zip")) {
                        byte[] subZip = zis.readAllBytes();
                        processarZipBytes(subZip, entry.getName(), relatorio);
                    } else if (nome.endsWith(".xml")) {
                        relatorio.incrementarExaminados();
                        byte[] xmlBytes = zis.readAllBytes();
                        try (ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes)) {
                            NotaFiscalResumo nf = parseAndAudit(bais, entry.getName());
                            if (nf != null) {
                                adicionarNota(nf, relatorio);
                            }
                        } catch (Exception ex) {
                            relatorio.registrarErro("Erro no XML do ZIP (" + entry.getName() + "): " + ex.getMessage());
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void adicionarNota(NotaFiscalResumo nf, RelatorioCarga relatorio) {
        String chave = nf.getChNFe();
        if (chave == null || chave.trim().isEmpty()) {
            chave = nf.getnNF() + "_" + nf.getSerie() + "_" + (nf.getEmitCnpj() != null ? nf.getEmitCnpj() : "");
            nf.setChNFe(chave);
        }
        if (notasMap.containsKey(chave)) {
            relatorio.registrarDuplicada();
            return;
        }
        if (chavesCanceladas.contains(chave)) {
            nf.setCancelada(true);
            nf.setStatus("CANCELADA");
            nf.setObsTotais("Nota Cancelada no SEFAZ via Evento");
            relatorio.registrarCancelada();
        }
        relatorio.registrarNota(nf.getModelo());
        notasMap.put(chave, nf);
    }

    private void sincronizarColecoesMemoria() {
        notasEmMemoria.clear();
        itensEmMemoria.clear();
        for (NotaFiscalResumo nf : notasMap.values()) {
            if (cnpjAtivo != null && !"TODAS".equalsIgnoreCase(cnpjAtivo)) {
                String emitClean = (nf.getEmitCnpj() != null) ? nf.getEmitCnpj().replaceAll("\\D", "") : "";
                String destClean = (nf.getDestCnpj() != null) ? nf.getDestCnpj().replaceAll("\\D", "") : "";
                boolean emitMatch = emitClean.startsWith(cnpjAtivo);
                boolean destMatch = destClean.startsWith(cnpjAtivo);
                if (!emitMatch && !destMatch) {
                    continue;
                }
            }
            notasEmMemoria.add(nf);
            if (nf.getItens() != null) {
                itensEmMemoria.addAll(nf.getItens());
            }
        }
    }

    public synchronized void limparMemoria() {
        notasEmMemoria.clear();
        itensEmMemoria.clear();
        matrizUniformidade.clear();
        notasMap.clear();
        chavesCanceladas.clear();
        empresasDetectadas.clear();
        cnpjAtivo = "TODAS";
    }

    private NotaFiscalResumo parseAndAudit(InputStream is, String nomeOrigem) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();

            // 1. Verificar se é evento da SEFAZ (procEventoNFe, evento, infEvento)
            NodeList infEventos = doc.getElementsByTagNameNS("*", "infEvento");
            if (infEventos.getLength() == 0) infEventos = doc.getElementsByTagName("infEvento");
            if (infEventos.getLength() > 0) {
                Element infEvento = (Element) infEventos.item(0);
                String chNFe = getTagValue(infEvento, "chNFe");
                String tpEvento = getTagValue(infEvento, "tpEvento");
                String descEvento = getTagValue(infEvento, "descEvento");

                if (relatorioEmProgresso != null) {
                    relatorioEmProgresso.registrarEvento();
                }
                if ("110111".equals(tpEvento) || (descEvento != null && descEvento.toLowerCase().contains("cancelamento"))) {
                    if (chNFe != null && !chNFe.isEmpty()) {
                        chavesCanceladas.add(chNFe);
                        NotaFiscalResumo nfExistente = notasMap.get(chNFe);
                        if (nfExistente != null) {
                            nfExistente.setCancelada(true);
                            nfExistente.setStatus("CANCELADA");
                            nfExistente.setObsTotais("Nota Cancelada no SEFAZ via Evento");
                        }
                        if (relatorioEmProgresso != null) {
                            relatorioEmProgresso.registrarCancelada();
                        }
                    }
                }
                return null;
            }

            // 2. Buscar identificação da nota fiscal (<ide>)
            Element ide = getFirstChild(doc.getDocumentElement(), "ide");
            if (ide == null) {
                NodeList ides = doc.getElementsByTagNameNS("*", "ide");
                if (ides.getLength() == 0) ides = doc.getElementsByTagName("ide");
                if (ides.getLength() > 0) ide = (Element) ides.item(0);
            }
            if (ide == null) return null;

            if (relatorioEmProgresso != null) {
                relatorioEmProgresso.incrementarXmlsLidos();
            }

            NotaFiscalResumo nf = new NotaFiscalResumo();
            
            // Modelo (55 = NF-e, 65 = NFC-e)
            String mod = getTagValue(ide, "mod");
            nf.setModelo(mod != null && !mod.isEmpty() ? mod : "55");

            // Chave de Acesso
            NodeList infNFes = doc.getElementsByTagNameNS("*", "infNFe");
            if (infNFes.getLength() == 0) infNFes = doc.getElementsByTagName("infNFe");
            if (infNFes.getLength() > 0) {
                Element infNFe = (Element) infNFes.item(0);
                String idAttr = infNFe.getAttribute("Id");
                if (idAttr != null) nf.setChNFe(idAttr.replace("NFe", ""));
            }

            nf.setnNF(getTagValue(ide, "nNF"));
            nf.setSerie(getTagValue(ide, "serie"));
            String dhEmi = getTagValue(ide, "dhEmi");
            nf.setDhEmi(dhEmi != null && dhEmi.length() >= 10 ? dhEmi.substring(0, 10) : dhEmi);
            nf.setTpNF("1".equals(getTagValue(ide, "tpNF")) ? "SAIDA" : "ENTRADA");

            // Emitente
            NodeList emits = doc.getElementsByTagNameNS("*", "emit");
            if (emits.getLength() == 0) emits = doc.getElementsByTagName("emit");
            Element emit = null;
            if (emits.getLength() > 0) {
                emit = (Element) emits.item(0);
                String cnpj = getTagValue(emit, "CNPJ");
                nf.setEmitCnpj(cnpj != null && !cnpj.isEmpty() ? cnpj : getTagValue(emit, "CPF"));
                nf.setEmitNome(getTagValue(emit, "xNome"));
                nf.setEmitUf(getTagValue(emit, "UF"));
            }

            // Destinatário (Em NFC-e mod 65 para consumidor sem CPF pode não constar)
            NodeList dests = doc.getElementsByTagNameNS("*", "dest");
            if (dests.getLength() == 0) dests = doc.getElementsByTagName("dest");
            Element dest = null;
            if (dests.getLength() > 0) {
                dest = (Element) dests.item(0);
                String cnpj = getTagValue(dest, "CNPJ");
                nf.setDestCnpj(cnpj != null && !cnpj.isEmpty() ? cnpj : getTagValue(dest, "CPF"));
                nf.setDestNome(getTagValue(dest, "xNome"));
                nf.setDestUf(getTagValue(dest, "UF"));
            } else {
                nf.setDestNome("Consumidor Final");
                nf.setDestCnpj("Consumidor Final");
                nf.setDestUf(nf.getEmitUf());
            }

            // Identificação do Papel / Fluxo Operacional
            String emitClean = (nf.getEmitCnpj() != null) ? nf.getEmitCnpj().replaceAll("\\D", "") : "";
            String destClean = (nf.getDestCnpj() != null) ? nf.getDestCnpj().replaceAll("\\D", "") : "";

            String emitRaiz = emitClean.length() >= 8 ? emitClean.substring(0, 8) : "";
            String destRaiz = destClean.length() >= 8 ? destClean.substring(0, 8) : "";

            boolean emitAuditado = (!emitRaiz.isEmpty() && empresasDetectadas.containsKey(emitRaiz))
                    || (cnpjAtivo != null && !"TODAS".equalsIgnoreCase(cnpjAtivo) && emitClean.startsWith(cnpjAtivo));

            boolean destAuditado = (!destRaiz.isEmpty() && empresasDetectadas.containsKey(destRaiz))
                    || (cnpjAtivo != null && !"TODAS".equalsIgnoreCase(cnpjAtivo) && destClean.startsWith(cnpjAtivo));

            if (emitAuditado && destAuditado) {
                nf.setTipoFluxo("TRANSFERENCIA_INTERNA");
            } else if (emitAuditado) {
                nf.setTipoFluxo("SAIDA_COMABEL");
            } else if (destAuditado) {
                nf.setTipoFluxo("ENTRADA_FORNECEDOR");
            } else {
                if (empresasDetectadas.isEmpty() && !emitRaiz.isEmpty()) {
                    EmpresaAuditada emp = empresasDetectadas.computeIfAbsent(emitRaiz, k -> new EmpresaAuditada());
                    Element enderEmit = getFirstChild(emit, "enderEmit");
                    String mun = enderEmit != null ? getTagValue(enderEmit, "xMun") : "";
                    emp.atualizarComDados(nf.getEmitNome(), nf.getEmitCnpj(), nf.getEmitUf(), mun, nf.getDhEmi());
                    emp.setTotalNotasEmitidas(emp.getTotalNotasEmitidas() + 1);
                    nf.setTipoFluxo("SAIDA_COMABEL");
                } else {
                    nf.setTipoFluxo("SAIDA_COMABEL");
                }
            }

            if (emitAuditado && empresasDetectadas.containsKey(emitRaiz)) {
                EmpresaAuditada emp = empresasDetectadas.get(emitRaiz);
                Element enderEmit = getFirstChild(emit, "enderEmit");
                String mun = enderEmit != null ? getTagValue(enderEmit, "xMun") : "";
                emp.atualizarComDados(nf.getEmitNome(), nf.getEmitCnpj(), nf.getEmitUf(), mun, nf.getDhEmi());
                emp.setTotalNotasEmitidas(emp.getTotalNotasEmitidas() + 1);
            }
            if (destAuditado && empresasDetectadas.containsKey(destRaiz)) {
                EmpresaAuditada empDest = empresasDetectadas.get(destRaiz);
                Element enderDest = getFirstChild(dest, "enderDest");
                String mun = enderDest != null ? getTagValue(enderDest, "xMun") : "";
                empDest.atualizarComDados(nf.getDestNome(), nf.getDestCnpj(), nf.getDestUf(), mun, nf.getDhEmi());
                empDest.setTotalNotasRecebidas(empDest.getTotalNotasRecebidas() + 1);
            }

            // Totais
            NodeList icmsTots = doc.getElementsByTagNameNS("*", "ICMSTot");
            if (icmsTots.getLength() == 0) icmsTots = doc.getElementsByTagName("ICMSTot");
            if (icmsTots.getLength() > 0) {
                Element icmsTot = (Element) icmsTots.item(0);
                nf.setvNF(toDecimal(getTagValue(icmsTot, "vNF")));
                nf.setvIcms(toDecimal(getTagValue(icmsTot, "vICMS")));
            }

            NodeList ibsTots = doc.getElementsByTagNameNS("*", "IBSCBSTot");
            if (ibsTots.getLength() == 0) ibsTots = doc.getElementsByTagName("IBSCBSTot");
            if (ibsTots.getLength() > 0) {
                Element ibsTot = (Element) ibsTots.item(0);
                nf.setvBcIbsCbs(toDecimal(getTagValue(ibsTot, "vBCIBSCBS")));
                nf.setvIbs(toDecimal(getTagValue(ibsTot, "vIBS")));
                nf.setvCbs(toDecimal(getTagValue(ibsTot, "vCBS")));
            }

            // Itens (<det>)
            NodeList dets = doc.getElementsByTagNameNS("*", "det");
            if (dets.getLength() == 0) dets = doc.getElementsByTagName("det");
            List<ItemAuditoria> itens = new ArrayList<>();
            BigDecimal somaBc = BigDecimal.ZERO;
            BigDecimal somaIbs = BigDecimal.ZERO;
            BigDecimal somaCbs = BigDecimal.ZERO;
            int itensDiv = 0;

            for (int i = 0; i < dets.getLength(); i++) {
                Element det = (Element) dets.item(i);
                ItemAuditoria item = auditarItem(det, nf);
                itens.add(item);
                somaBc = somaBc.add(item.getvBcIbsCbs());
                somaIbs = somaIbs.add(item.getvIbs());
                somaCbs = somaCbs.add(item.getvCbs());
                if (!"CONFORME".equals(item.getStatus())) {
                    itensDiv++;
                }
            }

            nf.setItens(itens);
            nf.setTotalItens(itens.size());
            nf.setItensDivergentes(itensDiv);

            // Validação de totais
            List<String> obsTotais = new ArrayList<>();
            if (ibsTots.getLength() > 0) {
                if (somaBc.subtract(nf.getvBcIbsCbs()).abs().compareTo(new BigDecimal("0.05")) > 0) {
                    obsTotais.add("Divergência na Base IBS/CBS (Totalizador: " + nf.getvBcIbsCbs() + " vs Itens: " + somaBc + ")");
                }
                if (somaIbs.subtract(nf.getvIbs()).abs().compareTo(new BigDecimal("0.05")) > 0) {
                    obsTotais.add("Divergência no vIBS (Totalizador: " + nf.getvIbs() + " vs Itens: " + somaIbs + ")");
                }
                if (somaCbs.subtract(nf.getvCbs()).abs().compareTo(new BigDecimal("0.05")) > 0) {
                    obsTotais.add("Divergência no vCBS (Totalizador: " + nf.getvCbs() + " vs Itens: " + somaCbs + ")");
                }
            }

            if (itensDiv == 0 && obsTotais.isEmpty()) {
                nf.setStatus("CONFORME");
                nf.setObsTotais("Totais conciliados");
                nf.setCorrecaoSugerida("Nenhuma inconsistência fiscal detectada. Documento e itens 100% conformes com a legislação.");
            } else {
                nf.setStatus("DIVERGENTE");
                nf.setObsTotais(obsTotais.isEmpty() ? itensDiv + " item(ns) com inconsistência" : String.join("; ", obsTotais));
                if (!obsTotais.isEmpty()) {
                    nf.setCorrecaoSugerida("Ajustar o totalizador da NF-e no grupo <total><IBSCBSTot> (Base: R$ " + somaBc + ", IBS: R$ " + somaIbs + ", CBS: R$ " + somaCbs + ") e corrigir a parametrização dos itens divergentes.");
                } else {
                    nf.setCorrecaoSugerida("Revisar os itens divergentes da nota aplicando as classificações (cClassTrib) e CSTs sugeridos pela conformidade SVRS.");
                }
            }

            return nf;
        } catch (Exception e) {
            System.err.println("[AUDITOR] Erro de parsing em " + nomeOrigem + ": " + e.getMessage());
            return null;
        }
    }

    private ItemAuditoria auditarItem(Element det, NotaFiscalResumo nf) {
        ItemAuditoria item = new ItemAuditoria();
        item.setChNFe(nf.getChNFe());
        item.setModelo(nf.getModelo());
        item.setnNF(nf.getnNF());
        item.setDhEmi(nf.getDhEmi());
        item.setnItem(det.getAttribute("nItem"));

        Element prod = getFirstChild(det, "prod");
        if (prod != null) {
            item.setcProd(getTagValue(prod, "cProd"));
            item.setxProd(getTagValue(prod, "xProd"));
            item.setNcm(getTagValue(prod, "NCM"));
            item.setCfop(getTagValue(prod, "CFOP"));
            item.setvProd(toDecimal(getTagValue(prod, "vProd")));
        }

        Element imposto = getFirstChild(det, "imposto");
        if (imposto != null) {
            // ICMS
            Element icms = getFirstChild(imposto, "ICMS");
            if (icms != null) {
                NodeList subIcmsList = icms.getChildNodes();
                for (int j = 0; j < subIcmsList.getLength(); j++) {
                    if (subIcmsList.item(j) instanceof Element) {
                        Element sub = (Element) subIcmsList.item(j);
                        String cst = getTagValue(sub, "CST");
                        item.setCstIcms(cst != null ? cst : getTagValue(sub, "CSOSN"));
                        item.setvIcms(toDecimal(getTagValue(sub, "vICMS")));
                        break;
                    }
                }
            }

            // IBS/CBS
            Element ibscbs = getFirstChild(imposto, "IBSCBS");
            if (ibscbs != null) {
                item.setHasIbsCbs(true);
                String cst = getTagValue(ibscbs, "CST");
                if (cst != null && cst.length() < 3 && cst.matches("\\d+")) {
                    cst = String.format("%03d", Integer.parseInt(cst));
                }
                item.setCstIbsCbs(cst != null ? cst : "");

                String cClass = getTagValue(ibscbs, "cClassTrib");
                if (cClass != null && cClass.length() < 6 && cClass.matches("\\d+")) {
                    cClass = String.format("%06d", Integer.parseInt(cClass));
                }
                item.setcClassTrib(cClass != null ? cClass : "");

                Element gIbsCbs = getFirstChild(ibscbs, "gIBSCBS");
                if (gIbsCbs != null) {
                    item.setvBcIbsCbs(toDecimal(getTagValue(gIbsCbs, "vBC")));
                    item.setvIbs(toDecimal(getTagValue(gIbsCbs, "vIBS")));
                    item.setvCbs(toDecimal(getTagValue(gIbsCbs, "vCBS")));

                    Element gIbsUf = getFirstChild(gIbsCbs, "gIBSUF");
                    if (gIbsUf != null) {
                        Element gRed = getFirstChild(gIbsUf, "gRed");
                        if (gRed != null) {
                            item.setpRedAliqIbs(toDecimal(getTagValue(gRed, "pRedAliq")));
                        }
                    }

                    Element gCbs = getFirstChild(gIbsCbs, "gCBS");
                    if (gCbs != null) {
                        Element gRed = getFirstChild(gCbs, "gRed");
                        if (gRed != null) {
                            item.setpRedAliqCbs(toDecimal(getTagValue(gRed, "pRedAliq")));
                        }
                    }
                }
            } else {
                item.setHasIbsCbs(false);
            }
        }

        // --- Regras de Conformidade & Validação de Papel (Fornecedor vs Comabel vs Transferência) ---
        List<String> apontamentos = new ArrayList<>();

        if ("TRANSFERENCIA_INTERNA".equals(nf.getTipoFluxo())) {
            // Regra específica de Transferência entre Matriz e Filial (Não-incidência plena - CST 410 / cClass 410002)
            if (item.isHasIbsCbs()) {
                if (!"410".equals(item.getCstIbsCbs())) {
                    apontamentos.add("Transferência interna Matriz/Filial: CST incorreto (" + item.getCstIbsCbs() + "); deve ser 410 (Não Incidência)");
                }
                if (!"410002".equals(item.getcClassTrib())) {
                    apontamentos.add("Transferência interna Matriz/Filial: cClass incorreto (" + item.getcClassTrib() + "); esperado 410002");
                }
                if (item.getvIbs().compareTo(BigDecimal.ZERO) > 0 || item.getvCbs().compareTo(BigDecimal.ZERO) > 0) {
                    apontamentos.add("Transferência interna com destaque indevido de tributo (vIBS/vCBS deve ser R$ 0,00)");
                }
            } else {
                apontamentos.add("Transferência emitida sem grupo IBSCBS");
            }
        } else {
            // Regras Gerais de Fornecedor (Entrada) ou Comabel (Saída)
            if (!item.isHasIbsCbs()) {
                if ("ENTRADA_FORNECEDOR".equals(nf.getTipoFluxo())) {
                    apontamentos.add("Fornecedor emitiu sem grupo IBSCBS (Risco: impede crédito de IBS/CBS na Comabel)");
                } else {
                    apontamentos.add("Item emitido sem grupo IBSCBS");
                }
            } else {
                RegraClassificacao regra = regrasService.getRegra(item.getcClassTrib());
                if (regra == null) {
                    apontamentos.add("cClassTrib '" + item.getcClassTrib() + "' não reconhecido na base oficial SVRS");
                } else {
                    if (!item.getCstIbsCbs().equals(regra.getCstEsperado())) {
                        apontamentos.add("CST incorreto: informado " + item.getCstIbsCbs() + ", esperado " + regra.getCstEsperado() + " (" + regra.getNome() + ")");
                    }
                    if (!regrasService.isNcmPermitido(item.getcClassTrib(), item.getNcm())) {
                        apontamentos.add("NCM " + item.getNcm() + " não permitido no anexo de " + item.getcClassTrib());
                    }
                    if (regra.getPercRedIbs() > 0) {
                        BigDecimal redEsperada = BigDecimal.valueOf(regra.getPercRedIbs());
                        if (item.getpRedAliqIbs().subtract(redEsperada).abs().compareTo(new BigDecimal("0.01")) > 0) {
                            apontamentos.add("Redução IBS incorreta: informada " + item.getpRedAliqIbs() + "%, esperada " + redEsperada + "%");
                        }
                    }
                    if (regra.getPercRedCbs() > 0) {
                        BigDecimal redEsperada = BigDecimal.valueOf(regra.getPercRedCbs());
                        if (item.getpRedAliqCbs().subtract(redEsperada).abs().compareTo(new BigDecimal("0.01")) > 0) {
                            apontamentos.add("Redução CBS incorreta: informada " + item.getpRedAliqCbs() + "%, esperada " + redEsperada + "%");
                        }
                    }
                }
            }
        }

        item.setApontamentos(apontamentos);
        item.setStatus(apontamentos.isEmpty() ? "CONFORME" : "DIVERGENTE");
        determinarCorrecaoItem(item, nf);
        return item;
    }

    private void determinarCorrecaoItem(ItemAuditoria item, NotaFiscalResumo nf) {
        if (item.getApontamentos().isEmpty()) {
            item.setCorrecaoSugerida("Item em total conformidade com a legislação e SVRS. Nenhuma ação corretiva necessária.");
            item.setcClassTribSugerido(item.getcClassTrib());
            item.setCstIbsCbsSugerido(item.getCstIbsCbs());
            item.setpRedSugerido(item.getpRedAliqIbs());
            item.setFundamentacaoSugerida("Regras Gerais SVRS / LC 214/2025");
            return;
        }

        // Cenário 1: Transferência interna Matriz/Filial
        if ("TRANSFERENCIA_INTERNA".equals(nf.getTipoFluxo())) {
            item.setCstIbsCbsSugerido("410");
            item.setcClassTribSugerido("410002");
            item.setpRedSugerido(BigDecimal.ZERO);
            item.setFundamentacaoSugerida("Art. 4º da LC 214/2025 (Não-incidência plena em remessas e transferências entre estabelecimentos da mesma empresa)");
            item.setCorrecaoSugerida("Parametrizar o emissor para aplicar CST 410 e cClassTrib 410002 (Transferência entre estabelecimentos do mesmo titular). Por se tratar de mera transferência, a operação é amparada pela não-incidência, devendo zerar o destaque do imposto (vIBS = R$ 0,00 e vCBS = R$ 0,00).");
            return;
        }

        // Cenário 2: Sem grupo IBSCBS
        if (!item.isHasIbsCbs()) {
            List<String> codigosNcm = regrasService.getCodigosPorNcm(item.getNcm());
            RegraClassificacao regraNcm = !codigosNcm.isEmpty() ? regrasService.getRegra(codigosNcm.get(0)) : null;

            if ("ENTRADA_FORNECEDOR".equals(nf.getTipoFluxo())) {
                item.setFundamentacaoSugerida("Art. 28 da LC 214/2025 e EC 132/2023 (Princípio da Não-Cumulatividade)");
                if (regraNcm != null) {
                    item.setcClassTribSugerido(regraNcm.getCodigo());
                    item.setCstIbsCbsSugerido(regraNcm.getCstEsperado());
                    item.setpRedSugerido(BigDecimal.valueOf(regraNcm.getPercRedIbs()));
                    item.setCorrecaoSugerida("Notificar o fornecedor para emissão de NF-e complementar com o grupo <IBSCBS> preenchido. Pela NCM " + item.getNcm() + ", a classificação sugerida é cClassTrib " + regraNcm.getCodigo() + " (" + regraNcm.getNome() + "), CST " + regraNcm.getCstEsperado() + (regraNcm.getPercRedIbs() > 0 ? " com redução de " + regraNcm.getPercRedIbs() + "%" : "") + " para assegurar o aproveitamento de créditos pela Comabel.");
                } else {
                    item.setcClassTribSugerido("000001");
                    item.setCstIbsCbsSugerido("000");
                    item.setpRedSugerido(BigDecimal.ZERO);
                    item.setCorrecaoSugerida("Exigir do fornecedor NF-e complementar com grupo IBSCBS destacado (regra padrão: cClassTrib 000001 / CST 000 com alíquota integral), evitando glosa ou perda de crédito tributário na entrada.");
                }
            } else {
                item.setFundamentacaoSugerida("Obrigatoriedade de Destaque IBS/CBS - Layout 2026");
                if (regraNcm != null) {
                    item.setcClassTribSugerido(regraNcm.getCodigo());
                    item.setCstIbsCbsSugerido(regraNcm.getCstEsperado());
                    item.setpRedSugerido(BigDecimal.valueOf(regraNcm.getPercRedIbs()));
                    item.setCorrecaoSugerida("Cadastrar no ERP o grupo <IBSCBS> para o produto com cClassTrib " + regraNcm.getCodigo() + " (" + regraNcm.getNome() + "), CST " + regraNcm.getCstEsperado() + (regraNcm.getPercRedIbs() > 0 ? " e redução de " + regraNcm.getPercRedIbs() + "%" : "") + ".");
                } else {
                    item.setcClassTribSugerido("000001");
                    item.setCstIbsCbsSugerido("000");
                    item.setpRedSugerido(BigDecimal.ZERO);
                    item.setCorrecaoSugerida("Configurar o sistema emissor para gerar o grupo <IBSCBS> aplicando a regra padrão de tributação integral (cClassTrib 000001 / CST 000).");
                }
            }
            return;
        }

        // Cenário 3: Grupo IBSCBS presente com divergências
        RegraClassificacao regraAtual = regrasService.getRegra(item.getcClassTrib());
        List<String> codigosNcm = regrasService.getCodigosPorNcm(item.getNcm());
        RegraClassificacao regraCompativelNcm = !codigosNcm.isEmpty() ? regrasService.getRegra(codigosNcm.get(0)) : null;

        // Se cClassTrib atual não é reconhecido
        if (regraAtual == null) {
            item.setFundamentacaoSugerida("Tabela Oficial de Classificações Tributárias SVRS 2026");
            if (regraCompativelNcm != null) {
                item.setcClassTribSugerido(regraCompativelNcm.getCodigo());
                item.setCstIbsCbsSugerido(regraCompativelNcm.getCstEsperado());
                item.setpRedSugerido(BigDecimal.valueOf(regraCompativelNcm.getPercRedIbs()));
                item.setCorrecaoSugerida("Substituir o cClassTrib inválido pelo código oficial " + regraCompativelNcm.getCodigo() + " (" + regraCompativelNcm.getNome() + "), compatível com a NCM " + item.getNcm() + ", utilizando CST " + regraCompativelNcm.getCstEsperado() + (regraCompativelNcm.getPercRedIbs() > 0 ? " com redução de " + regraCompativelNcm.getPercRedIbs() + "%" : "") + ".");
            } else {
                item.setcClassTribSugerido("000001");
                item.setCstIbsCbsSugerido("000");
                item.setpRedSugerido(BigDecimal.ZERO);
                item.setCorrecaoSugerida("Substituir o cClassTrib inválido pelo código oficial padrão 000001 (Tributação Integral) com CST 000, pois a NCM informada não consta em anexos de benefícios específicos.");
            }
            return;
        }

        // Se a NCM não pertence ao anexo da classificação informada
        if (!regrasService.isNcmPermitido(item.getcClassTrib(), item.getNcm())) {
            item.setFundamentacaoSugerida("Anexos Taxativos da SVRS / LC 214/2025");
            if (regraCompativelNcm != null && !regraCompativelNcm.getCodigo().equals(item.getcClassTrib())) {
                item.setcClassTribSugerido(regraCompativelNcm.getCodigo());
                item.setCstIbsCbsSugerido(regraCompativelNcm.getCstEsperado());
                item.setpRedSugerido(BigDecimal.valueOf(regraCompativelNcm.getPercRedIbs()));
                item.setCorrecaoSugerida("A NCM " + item.getNcm() + " não é elegível para a classificação informada (" + item.getcClassTrib() + "). Ajustar para a classificação correta " + regraCompativelNcm.getCodigo() + " (" + regraCompativelNcm.getNome() + ") com CST " + regraCompativelNcm.getCstEsperado() + (regraCompativelNcm.getPercRedIbs() > 0 ? " e redução de " + regraCompativelNcm.getPercRedIbs() + "%" : "") + ".");
            } else {
                item.setcClassTribSugerido("000001");
                item.setCstIbsCbsSugerido("000");
                item.setpRedSugerido(BigDecimal.ZERO);
                item.setCorrecaoSugerida("A NCM " + item.getNcm() + " não pertence ao anexo da classificação " + item.getcClassTrib() + " nem possui tratamento diferenciado em outros anexos. Reenquadrar para cClassTrib 000001 (Tributação Integral) com CST 000 e alíquota plena.");
            }
            return;
        }

        // Se o CST estiver divergente da classificação
        if (!item.getCstIbsCbs().equals(regraAtual.getCstEsperado())) {
            item.setcClassTribSugerido(regraAtual.getCodigo());
            item.setCstIbsCbsSugerido(regraAtual.getCstEsperado());
            item.setpRedSugerido(BigDecimal.valueOf(regraAtual.getPercRedIbs()));
            item.setFundamentacaoSugerida("Tabela de Correlação CST x cClassTrib - SVRS");
            item.setCorrecaoSugerida("Corrigir o CST de '" + item.getCstIbsCbs() + "' para '" + regraAtual.getCstEsperado() + "', que é o CST obrigatório na SVRS para o cClassTrib " + regraAtual.getCodigo() + " (" + regraAtual.getNome() + ").");
            return;
        }

        // Se a redução estiver divergente
        if (regraAtual.getPercRedIbs() > 0 || regraAtual.getPercRedCbs() > 0) {
            item.setcClassTribSugerido(regraAtual.getCodigo());
            item.setCstIbsCbsSugerido(regraAtual.getCstEsperado());
            item.setpRedSugerido(BigDecimal.valueOf(regraAtual.getPercRedIbs()));
            item.setFundamentacaoSugerida("Percentuais de Redução Fixados pela LC 214/2025");
            item.setCorrecaoSugerida("Corrigir o percentual de redução no emissor: pRedAliq de IBS para " + regraAtual.getPercRedIbs() + "% e de CBS para " + regraAtual.getPercRedCbs() + "% conforme determina a regulamentação para " + regraAtual.getNome() + ".");
            return;
        }

        // Fallback genérico
        item.setcClassTribSugerido(regraAtual.getCodigo());
        item.setCstIbsCbsSugerido(regraAtual.getCstEsperado());
        item.setFundamentacaoSugerida("Regras Gerais SVRS 2026");
        item.setCorrecaoSugerida("Revisar parametrização fiscal do item conforme apontamentos levantados.");
    }

    private void atualizarKpis() {
        // Separar notas ativas e canceladas: canceladas não entram na auditoria
        List<NotaFiscalResumo> ativas = notasEmMemoria.stream().filter(n -> !n.isCancelada()).toList();
        List<NotaFiscalResumo> canceladas = notasEmMemoria.stream().filter(NotaFiscalResumo::isCancelada).toList();

        int totalAuditadas = ativas.size();
        int conformes = (int) ativas.stream().filter(n -> "CONFORME".equals(n.getStatus())).count();

        BigDecimal vTotal = ativas.stream().map(NotaFiscalResumo::getvNF).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vIcms = ativas.stream().map(NotaFiscalResumo::getvIcms).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vBcIbs = ativas.stream().map(NotaFiscalResumo::getvBcIbsCbs).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vIbs = ativas.stream().map(NotaFiscalResumo::getvIbs).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vCbs = ativas.stream().map(NotaFiscalResumo::getvCbs).reduce(BigDecimal.ZERO, BigDecimal::add);

        kpiDashboard.recalcularGeral(totalAuditadas, conformes, vTotal, vIcms, vBcIbs, vIbs, vCbs);
        kpiDashboard.setTotalNotasCanceladas(canceladas.size());

        // Submétricas Fornecedor
        List<NotaFiscalResumo> entradas = ativas.stream().filter(n -> "ENTRADA_FORNECEDOR".equals(n.getTipoFluxo())).toList();
        int confFornec = (int) entradas.stream().filter(n -> "CONFORME".equals(n.getStatus())).count();
        BigDecimal vCompras = entradas.stream().map(NotaFiscalResumo::getvNF).reduce(BigDecimal.ZERO, BigDecimal::add);
        kpiDashboard.definirMetricasFornecedor(entradas.size(), confFornec, vCompras);

        // Submétricas Comabel
        List<NotaFiscalResumo> saidas = ativas.stream().filter(n -> "SAIDA_COMABEL".equals(n.getTipoFluxo())).toList();
        int confComabel = (int) saidas.stream().filter(n -> "CONFORME".equals(n.getStatus())).count();
        BigDecimal vVendas = saidas.stream().map(NotaFiscalResumo::getvNF).reduce(BigDecimal.ZERO, BigDecimal::add);
        kpiDashboard.definirMetricasComabel(saidas.size(), confComabel, vVendas);

        // Submétricas Transferência
        List<NotaFiscalResumo> transferencias = ativas.stream().filter(n -> "TRANSFERENCIA_INTERNA".equals(n.getTipoFluxo())).toList();
        int confTransf = (int) transferencias.stream().filter(n -> "CONFORME".equals(n.getStatus())).count();
        BigDecimal vTransf = transferencias.stream().map(NotaFiscalResumo::getvNF).reduce(BigDecimal.ZERO, BigDecimal::add);
        kpiDashboard.definirMetricasTransferencia(transferencias.size(), confTransf, vTransf);

        // --- Construção da Matriz de Uniformidade Entrada x Saída por NCM ---
        Map<String, ItemAuditoria> mapEntrada = new HashMap<>();
        Map<String, ItemAuditoria> mapSaida = new HashMap<>();

        for (ItemAuditoria it : itensEmMemoria) {
            if (it.getNcm() == null || it.getNcm().isEmpty()) continue;
            NotaFiscalResumo pai = getNotaPorChave(it.getChNFe());
            if (pai == null || pai.isCancelada()) continue;
            if ("ENTRADA_FORNECEDOR".equals(pai.getTipoFluxo())) {
                mapEntrada.putIfAbsent(it.getNcm(), it);
            } else if ("SAIDA_COMABEL".equals(pai.getTipoFluxo())) {
                mapSaida.put(it.getNcm(), it); // Prioriza venda direta se houver
            } else if ("TRANSFERENCIA_INTERNA".equals(pai.getTipoFluxo())) {
                mapSaida.putIfAbsent(it.getNcm(), it); // Registra transferência se ainda não houver venda
            }
        }

        Set<String> todosNcms = new TreeSet<>();
        todosNcms.addAll(mapEntrada.keySet());
        todosNcms.addAll(mapSaida.keySet());

        matrizUniformidade.clear();
        int uniformes = 0;
        for (String ncm : todosNcms) {
            ItemAuditoria in = mapEntrada.get(ncm);
            ItemAuditoria out = mapSaida.get(ncm);

            MatrizUniformidadeNcm mu = new MatrizUniformidadeNcm();
            mu.setNcm(ncm);
            mu.setDescricao(in != null ? in.getxProd() : (out != null ? out.getxProd() : ""));

            if (in != null) {
                mu.setcClassEntrada(in.getcClassTrib());
                mu.setCstEntrada(in.getCstIbsCbs());
            }
            if (out != null) {
                mu.setcClassSaida(out.getcClassTrib());
                mu.setCstSaida(out.getCstIbsCbs());
            }

            if (in != null && out != null) {
                boolean mesmaClass = Objects.equals(in.getcClassTrib(), out.getcClassTrib());
                boolean mesmoCst = Objects.equals(in.getCstIbsCbs(), out.getCstIbsCbs());
                if (mesmaClass && mesmoCst) {
                    mu.setStatus("UNIFORME");
                    mu.setObservacao("Classificação idêntica na entrada e saída");
                    uniformes++;
                } else {
                    mu.setStatus("DIVERGENTE");
                    mu.setObservacao("Incoerência tributária: Entrada (" + in.getcClassTrib() + ") difere da Saída (" + out.getcClassTrib() + ")");
                }
            } else if (in != null) {
                mu.setStatus("APENAS_ENTRADA");
                mu.setObservacao("Item registrado apenas em compras");
            } else {
                mu.setStatus("APENAS_SAIDA");
                mu.setObservacao("Item registrado apenas em vendas ou transferências");
            }

            matrizUniformidade.add(mu);
        }

        kpiDashboard.definirMetricasUniformidade(todosNcms.size(), uniformes, todosNcms.size() - uniformes);
    }

    private Element getFirstChild(Element parent, String tagName) {
        if (parent == null) return null;
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n instanceof Element) {
                Element el = (Element) n;
                String local = el.getLocalName() != null ? el.getLocalName() : el.getTagName();
                if (tagName.equals(local) || (local != null && local.endsWith(":" + tagName))) return el;
            }
        }
        return null;
    }

    private String getTagValue(Element parent, String tagName) {
        if (parent == null) return "";
        Element child = getFirstChild(parent, tagName);
        if (child != null && child.getTextContent() != null) {
            return child.getTextContent().trim();
        }
        NodeList nl = parent.getElementsByTagNameNS("*", tagName);
        if (nl.getLength() == 0) nl = parent.getElementsByTagName(tagName);
        if (nl.getLength() > 0 && nl.item(0).getTextContent() != null) {
            return nl.item(0).getTextContent().trim();
        }
        return "";
    }

    private BigDecimal toDecimal(String val) {
        if (val == null || val.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(val.trim().replace(",", "."));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public List<NotaFiscalResumo> getNotasEmMemoria() { return notasEmMemoria; }
    public List<NotaFiscalResumo> getNotasAtivas() {
        return notasEmMemoria.stream().filter(n -> !n.isCancelada()).toList();
    }
    public List<NotaFiscalResumo> getNotasCanceladas() {
        return notasEmMemoria.stream().filter(NotaFiscalResumo::isCancelada).toList();
    }
    public EmpresaAuditada getEmpresaAuditada() {
        if (cnpjAtivo != null && !"TODAS".equalsIgnoreCase(cnpjAtivo)) {
            if (empresasDetectadas.containsKey(cnpjAtivo)) {
                return empresasDetectadas.get(cnpjAtivo);
            }
            for (EmpresaAuditada emp : empresasDetectadas.values()) {
                if (cnpjAtivo.equals(emp.getCnpjChave()) || (cnpjAtivo.length() >= 8 && emp.getCnpjChave() != null && emp.getCnpjChave().startsWith(cnpjAtivo))) {
                    return emp;
                }
            }
        }
        if (empresasDetectadas.size() == 1) {
            return empresasDetectadas.values().iterator().next();
        }
        if (empresasDetectadas.size() > 1) {
            EmpresaAuditada cons = new EmpresaAuditada();
            cons.setRazaoSocial("Consolidado (" + empresasDetectadas.size() + " Empresas Auditadas)");
            cons.setNomeFantasia(empresasDetectadas.size() + " Empresas");
            cons.setCnpjPrincipal("Múltiplos CNPJs");
            cons.setCnpjRaiz("TODAS");
            cons.setUf("Diversas");
            cons.setMunicipio("Consolidado");
            for (EmpresaAuditada e : empresasDetectadas.values()) {
                if (cons.getPeriodoAuditado() == null && e.getPeriodoAuditado() != null) {
                    cons.setPeriodoAuditado(e.getPeriodoAuditado());
                }
                cons.getEstabelecimentos().addAll(e.getEstabelecimentos());
                cons.setTotalNotasEmitidas(cons.getTotalNotasEmitidas() + e.getTotalNotasEmitidas());
                cons.setTotalNotasRecebidas(cons.getTotalNotasRecebidas() + e.getTotalNotasRecebidas());
            }
            return cons;
        }
        return new EmpresaAuditada();
    }

    public List<EmpresaAuditada> getEmpresasDetectadas() {
        return new ArrayList<>(empresasDetectadas.values());
    }

    public synchronized void setCnpjAtivo(String cnpj) {
        this.cnpjAtivo = (cnpj != null && !cnpj.trim().isEmpty()) ? cnpj.trim() : "TODAS";
        sincronizarColecoesMemoria();
        atualizarKpis();
    }

    public String getCnpjAtivo() {
        return cnpjAtivo;
    }

    public synchronized EmpresaAuditada definirCnpjAuditado(String cnpj, String razaoSocial) {
        if (cnpj == null || cnpj.trim().isEmpty() || "TODAS".equalsIgnoreCase(cnpj.trim())) {
            this.cnpjAtivo = "TODAS";
            this.cnpjInformado = "";
            sincronizarColecoesMemoria();
            atualizarKpis();
            return getEmpresaAuditada();
        }

        String clean = cnpj.replaceAll("\\D", "");
        if (clean.length() < 8) {
            throw new IllegalArgumentException("CNPJ deve conter no mínimo 8 dígitos.");
        }

        String raiz = clean.substring(0, 8);
        String chave = clean;
        this.cnpjInformado = clean;
        this.cnpjAtivo = chave;

        EmpresaAuditada emp = empresasDetectadas.computeIfAbsent(chave, k -> new EmpresaAuditada());
        emp.setCnpjRaiz(raiz);
        if (emp.getCnpjPrincipal() == null || emp.getCnpjPrincipal().isEmpty() || emp.getCnpjPrincipal().length() < 14) {
            emp.setCnpjPrincipal(EmpresaAuditada.formatarCnpj(clean));
        }
        if (razaoSocial != null && !razaoSocial.trim().isEmpty()) {
            emp.setRazaoSocial(razaoSocial.trim());
        }

        // Busca dados da empresa nas notas fiscais já carregadas
        buscarDadosEmpresaNasNotas(emp, clean);

        sincronizarColecoesMemoria();
        atualizarKpis();
        return emp;
    }

    public synchronized EmpresaAuditada definirEmpresaPorXmlNota(InputStream is, String nomeArquivo) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();

            NodeList emits = doc.getElementsByTagNameNS("*", "emit");
            if (emits.getLength() == 0) emits = doc.getElementsByTagName("emit");
            if (emits.getLength() == 0) {
                throw new IllegalArgumentException("O arquivo XML enviado não contém a tag <emit> (emissor). Certifique-se de importar uma nota fiscal emitida pela empresa a ser auditada.");
            }

            Element emit = (Element) emits.item(0);
            String cnpj = getTagValue(emit, "CNPJ");
            if (cnpj == null || cnpj.trim().isEmpty()) {
                cnpj = getTagValue(emit, "CPF");
            }
            if (cnpj == null || cnpj.trim().isEmpty()) {
                throw new IllegalArgumentException("Não foi possível identificar o CNPJ/CPF do emissor no XML.");
            }

            String xNome = getTagValue(emit, "xNome");
            String xFant = getTagValue(emit, "xFant");

            Element enderEmit = getFirstChild(emit, "enderEmit");
            String uf = enderEmit != null ? getTagValue(enderEmit, "UF") : "";
            String mun = enderEmit != null ? getTagValue(enderEmit, "xMun") : "";

            String dhEmi = null;
            NodeList ides = doc.getElementsByTagNameNS("*", "ide");
            if (ides.getLength() == 0) ides = doc.getElementsByTagName("ide");
            if (ides.getLength() > 0) {
                Element ide = (Element) ides.item(0);
                dhEmi = getTagValue(ide, "dhEmi");
                if (dhEmi == null || dhEmi.isEmpty()) dhEmi = getTagValue(ide, "dEmi");
            }

            String clean = cnpj.replaceAll("\\D", "");
            String raiz = clean.length() >= 8 ? clean.substring(0, 8) : clean;
            String chave = clean;

            EmpresaAuditada emp = empresasDetectadas.computeIfAbsent(chave, k -> new EmpresaAuditada());
            emp.setCnpjPrincipal(EmpresaAuditada.formatarCnpj(clean));
            emp.setCnpjRaiz(raiz);
            if (xNome != null && !xNome.trim().isEmpty()) {
                emp.setRazaoSocial(xNome.trim());
            }
            if (xFant != null && !xFant.trim().isEmpty()) {
                emp.setNomeFantasia(xFant.trim());
            }
            if (uf != null && !uf.isEmpty()) emp.setUf(uf);
            if (mun != null && !mun.isEmpty()) emp.setMunicipio(mun);

            emp.atualizarComDados(xNome, clean, uf, mun, dhEmi);

            this.cnpjInformado = clean;
            this.cnpjAtivo = chave;

            buscarDadosEmpresaNasNotas(emp, clean);
            sincronizarColecoesMemoria();
            atualizarKpis();

            System.out.println("[AUDITORIA] Empresa auditada cadastrada via XML: " + emp.getNomeCurto() + " (" + emp.getCnpjPrincipal() + ")");
            return emp;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar XML de identificação da empresa: " + e.getMessage(), e);
        }
    }

    public String getCnpjInformado() {
        return cnpjInformado;
    }

    public List<ItemAuditoria> getItensEmMemoria() { return itensEmMemoria; }
    public List<MatrizUniformidadeNcm> getMatrizUniformidade() { return matrizUniformidade; }
    public KpiDashboard getKpiDashboard() { return kpiDashboard; }
    public String getUltimaPastaEscaneada() { return ultimaPastaEscaneada; }
    public RelatorioCarga getUltimoRelatorio() { return ultimoRelatorio; }

    public NotaFiscalResumo getNotaPorChave(String chNFe) {
        if (chNFe == null) return null;
        NotaFiscalResumo nf = notasMap.get(chNFe);
        if (nf != null) return nf;
        return notasEmMemoria.stream().filter(n -> chNFe.equals(n.getChNFe())).findFirst().orElse(null);
    }
}
