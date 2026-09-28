package br.com.comabel.reforma.controller;

import br.com.comabel.reforma.model.EmpresaAuditada;
import br.com.comabel.reforma.model.NotaFiscalResumo;
import br.com.comabel.reforma.model.RelatorioCarga;
import br.com.comabel.reforma.service.ExcelExportService;
import br.com.comabel.reforma.service.XmlAuditorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
public class AuditoriaXmlController {

    @Autowired
    private XmlAuditorService auditorService;

    @Autowired
    private ExcelExportService excelService;

    @Value("${app.default-scan-dir:c:/agente/agente}")
    private String defaultScanDir;

    @GetMapping("/auditoria-xml")
    public String listarNotas(Model model,
                              @RequestParam(value = "aba", defaultValue = "ativas") String aba,
                              @RequestParam(value = "filtro", required = false) String filtro,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "tipoFluxo", required = false) String tipoFluxo,
                              @RequestParam(value = "modelo", required = false) String modelo) {
        
        List<NotaFiscalResumo> todasAtivas = auditorService.getNotasAtivas();
        List<NotaFiscalResumo> todasCanceladas = auditorService.getNotasCanceladas();

        List<NotaFiscalResumo> notas;
        if ("canceladas".equalsIgnoreCase(aba)) {
            notas = todasCanceladas;
        } else {
            notas = todasAtivas;
        }

        if (status != null && !status.isEmpty() && !"TODOS".equalsIgnoreCase(status)) {
            notas = notas.stream().filter(n -> n.getStatus().equalsIgnoreCase(status)).toList();
        }

        if (modelo != null && !modelo.isEmpty() && !"TODOS".equalsIgnoreCase(modelo)) {
            notas = notas.stream().filter(n -> modelo.equalsIgnoreCase(n.getModelo())).toList();
        }

        if (tipoFluxo != null && !tipoFluxo.isEmpty() && !"TODOS".equalsIgnoreCase(tipoFluxo)) {
            notas = notas.stream().filter(n -> n.getTipoFluxo().equalsIgnoreCase(tipoFluxo)).toList();
        }

        if (filtro != null && !filtro.trim().isEmpty()) {
            String f = filtro.toLowerCase().trim();
            notas = notas.stream().filter(n ->
                    (n.getnNF() != null && n.getnNF().contains(f)) ||
                    (n.getEmitNome() != null && n.getEmitNome().toLowerCase().contains(f)) ||
                    (n.getEmitCnpj() != null && n.getEmitCnpj().contains(f)) ||
                    (n.getDestNome() != null && n.getDestNome().toLowerCase().contains(f)) ||
                    (n.getChNFe() != null && n.getChNFe().contains(f))
            ).toList();
        }

        model.addAttribute("notas", notas);
        model.addAttribute("abaAtual", aba);
        model.addAttribute("totalAtivas", todasAtivas.size());
        model.addAttribute("totalCanceladas", todasCanceladas.size());
        model.addAttribute("empresaAuditada", auditorService.getEmpresaAuditada());
        model.addAttribute("matrizUniformidade", auditorService.getMatrizUniformidade());
        model.addAttribute("kpis", auditorService.getKpiDashboard());
        model.addAttribute("pastaAtual", auditorService.getUltimaPastaEscaneada());
        model.addAttribute("defaultScanDir", defaultScanDir);
        model.addAttribute("filtroAtual", filtro);
        model.addAttribute("statusAtual", status);
        model.addAttribute("tipoFluxoAtual", tipoFluxo);
        model.addAttribute("modeloAtual", modelo);
        model.addAttribute("relatorioCarga", auditorService.getUltimoRelatorio());
        model.addAttribute("activeMenu", "auditoria-xml");
        return "auditoria-xml";
    }

    @PostMapping({"/auditoria-xml/definir-empresa-xml", "/definir-empresa-xml"})
    public String definirEmpresaPorXml(@RequestParam("arquivoXmlEmpresa") MultipartFile arquivoXml,
                                       HttpServletRequest request,
                                       RedirectAttributes redirect) {
        try {
            if (arquivoXml == null || arquivoXml.isEmpty()) {
                redirect.addFlashAttribute("mensagemErro", "Por favor, selecione um arquivo XML de nota fiscal emitida pela empresa.");
                return "redirect:/auditoria-xml";
            }
            EmpresaAuditada emp = auditorService.definirEmpresaPorXmlNota(arquivoXml.getInputStream(), arquivoXml.getOriginalFilename());
            redirect.addFlashAttribute("mensagemSucesso", "Empresa auditada cadastrada com sucesso: " + emp.getNomeCurto() + " (" + emp.getCnpjPrincipal() + ") - " + emp.getMunicipio() + "/" + emp.getUf());
        } catch (Exception e) {
            redirect.addFlashAttribute("mensagemErro", "Erro ao identificar empresa pelo XML: " + e.getMessage());
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isEmpty() ? referer : "/auditoria-xml");
    }

    @PostMapping("/auditoria-xml/escanear")
    public String escanearPasta(@RequestParam("caminhoPasta") String caminhoPasta,
                                @RequestParam(value = "cnpjAuditado", required = false) String cnpjAuditado,
                                RedirectAttributes redirect) {
        try {
            if (cnpjAuditado != null && !cnpjAuditado.trim().isEmpty()) {
                auditorService.definirCnpjAuditado(cnpjAuditado, null);
            }
            auditorService.auditarDiretorio(caminhoPasta);
            RelatorioCarga relatorio = auditorService.getUltimoRelatorio();
            String msg = (relatorio != null) ? relatorio.getMensagemFormatada() : auditorService.getNotasEmMemoria().size() + " notas auditadas.";
            redirect.addFlashAttribute("mensagemSucesso", "Varredura concluída com sucesso! " + msg);
        } catch (Exception e) {
            redirect.addFlashAttribute("mensagemErro", "Erro ao escanear pasta: " + e.getMessage());
        }
        return "redirect:/auditoria-xml";
    }

    @PostMapping("/auditoria-xml/upload")
    public String uploadArquivos(@RequestParam("arquivos") MultipartFile[] arquivos,
                                @RequestParam(value = "cnpjAuditado", required = false) String cnpjAuditado,
                                @RequestParam(value = "acumular", defaultValue = "false") boolean acumular,
                                RedirectAttributes redirect) {
        try {
            if (arquivos == null || arquivos.length == 0 || (arquivos.length == 1 && arquivos[0].isEmpty())) {
                redirect.addFlashAttribute("mensagemErro", "Nenhum arquivo selecionado.");
                return "redirect:/auditoria-xml";
            }
            if (cnpjAuditado != null && !cnpjAuditado.trim().isEmpty()) {
                auditorService.definirCnpjAuditado(cnpjAuditado, null);
            }
            auditorService.auditarUpload(Arrays.asList(arquivos), acumular);
            RelatorioCarga relatorio = auditorService.getUltimoRelatorio();
            String msg = (relatorio != null) ? relatorio.getMensagemFormatada() : auditorService.getNotasEmMemoria().size() + " notas auditadas.";
            redirect.addFlashAttribute("mensagemSucesso", "Upload concluído com sucesso! " + msg);
        } catch (Exception e) {
            redirect.addFlashAttribute("mensagemErro", "Erro ao processar upload: " + e.getMessage());
        }
        return "redirect:/auditoria-xml";
    }

    @GetMapping("/auditoria-xml/detalhe/{chNFe}")
    public String detalheNota(@PathVariable("chNFe") String chNFe, Model model, RedirectAttributes redirect) {
        NotaFiscalResumo nf = auditorService.getNotaPorChave(chNFe);
        if (nf == null) {
            redirect.addFlashAttribute("mensagemErro", "Nota fiscal não encontrada para a chave informada.");
            return "redirect:/auditoria-xml";
        }
        model.addAttribute("nf", nf);
        model.addAttribute("itens", nf.getItens());
        model.addAttribute("activeMenu", "auditoria-xml");
        return "detalhe-nota";
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel() {
        byte[] bytes = excelService.gerarExcelAuditoria(
                auditorService.getKpiDashboard(),
                auditorService.getNotasEmMemoria(),
                auditorService.getItensEmMemoria()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio_auditoria_reforma.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping({"/auditoria-xml/limpar-base", "/limpar-base"})
    public String limparBase(RedirectAttributes redirect) {
        try {
            auditorService.limparBaseDados();
            redirect.addFlashAttribute("mensagemSucesso", "Base de dados local (XMLs, notas e itens) e arquivos do SPED Fiscal foram 100% limpos com sucesso!");
        } catch (Exception e) {
            redirect.addFlashAttribute("mensagemErro", "Erro ao limpar base de dados: " + e.getMessage());
        }
        return "redirect:/auditoria-xml";
    }

    @GetMapping({"/auditoria-xml/exportar-divergencias", "/exportar/divergencias"})
    public ResponseEntity<byte[]> exportarDivergenciasExcel() {
        List<br.com.comabel.reforma.model.ItemAuditoria> divergentes = auditorService.getItensEmMemoria().stream()
                .filter(i -> !"CONFORME".equals(i.getStatus()))
                .toList();

        byte[] bytes = excelService.gerarExcelProdutosDivergentes(divergentes, auditorService.getEmpresaAuditada());

        String filename = "Produtos_Divergentes_ReformaFiscal.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
