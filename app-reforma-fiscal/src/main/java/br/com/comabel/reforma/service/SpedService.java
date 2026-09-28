package br.com.comabel.reforma.service;

import br.com.comabel.reforma.model.SpedDiagnostico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.*;

@Service
public class SpedService {

    @Autowired
    private RegrasTributariasService regrasService;

    private SpedDiagnostico ultimoDiagnostico;
    private boolean limpoPeloUsuario = false;

    public SpedDiagnostico analisarArquivo(File arquivoSped) {
        try (InputStream is = new FileInputStream(arquivoSped)) {
            return analisarStream(is);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar arquivo SPED: " + e.getMessage(), e);
        }
    }

    public SpedDiagnostico analisarStream(InputStream is) {
        this.limpoPeloUsuario = false;
        SpedDiagnostico diag = new SpedDiagnostico();
        Map<String, String[]> produtos0200 = new HashMap<>(); // cod -> [descr, ncm]
        Set<String> ncmsDistintos = new HashSet<>();

        int linhas = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("ISO-8859-1")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("|")) continue;
                linhas++;
                String[] p = line.split("\\|", -1);
                if (p.length < 2) continue;
                String reg = p[1];

                if ("0000".equals(reg) && p.length >= 11) {
                    diag.setPeriodo(p[4] + " a " + p[5]);
                    diag.setRazaoSocial(p[6]);
                    diag.setCnpj(p[7]);
                    diag.setUf(p[9]);
                } else if ("0200".equals(reg) && p.length >= 9) {
                    String cod = p[2];
                    String descr = p[3];
                    String ncm = p[8];
                    produtos0200.put(cod, new String[]{descr, ncm});
                    if (!ncm.isEmpty()) ncmsDistintos.add(ncm);
                } else if ("C100".equals(reg) && p.length >= 23) {
                    String sit = p[6];
                    if (!"02".equals(sit) && !"03".equals(sit) && !"04".equals(sit) && !"05".equals(sit)) {
                        String indOper = p[2];
                        BigDecimal vlDoc = toDecimal(p[12]);
                        if ("0".equals(indOper)) {
                            diag.setTotalNotasEntrada(diag.getTotalNotasEntrada() + 1);
                            diag.setValorCompras(diag.getValorCompras().add(vlDoc));
                        } else {
                            diag.setTotalNotasSaida(diag.getTotalNotasSaida() + 1);
                            diag.setValorVendas(diag.getValorVendas().add(vlDoc));
                        }
                    }
                } else if ("C190".equals(reg) && p.length >= 7) {
                    String cst = p[2];
                    String cfop = p[3];
                    BigDecimal vlOpr = toDecimal(p[5]);
                    if (cfop.startsWith("5") || cfop.startsWith("6")) {
                        if (cst.endsWith("20")) {
                            diag.setVendasReducaoBaseCst020(diag.getVendasReducaoBaseCst020().add(vlOpr));
                        } else if (cst.endsWith("60")) {
                            diag.setVendasSubstTribCst060(diag.getVendasSubstTribCst060().add(vlOpr));
                        } else if (cst.endsWith("00")) {
                            diag.setVendasTributacaoIntegralCst000(diag.getVendasTributacaoIntegralCst000().add(vlOpr));
                        }
                        if (cfop.startsWith("515") || cfop.startsWith("615")) {
                            diag.setVendasTransferencias(diag.getVendasTransferencias().add(vlOpr));
                        }
                    }
                } else if ("E110".equals(reg) && p.length >= 14) {
                    diag.setIcmsDebitos(toDecimal(p[2]));
                    diag.setIcmsCreditos(toDecimal(p[6]));
                    diag.setIcmsRecolher(toDecimal(p[13]));
                }
            }
        } catch (Exception e) {
            System.err.println("[SPED] Erro ao ler linhas: " + e.getMessage());
        }

        diag.setTotalLinhas(linhas);
        diag.setTotalProdutos0200(produtos0200.size());
        diag.setTotalNcmsDistintos(ncmsDistintos.size());

        // Cruzamento do Catálogo com a Reforma
        int cestaBasica = 0;
        int alimentosRed = 0;
        int padrao = 0;
        List<SpedDiagnostico.ItemSpedMapeado> amostra = new ArrayList<>();

        for (Map.Entry<String, String[]> entry : produtos0200.entrySet()) {
            String cod = entry.getKey();
            String descr = entry.getValue()[0];
            String ncm = entry.getValue()[1];

            List<String> codigosSVRS = regrasService.getCodigosPorNcm(ncm);
            String cClass = "000001";
            String enquadramento = "Tributação Integral Padrão";
            String cst = "000";
            String aliq = "Alíquota Padrão (~26,5%)";

            if (codigosSVRS.contains("200003")) {
                cClass = "200003";
                enquadramento = "Cesta Básica Nacional (Anexo I)";
                cst = "200";
                aliq = "0,00% (Redução 100%)";
                cestaBasica++;
            } else if (codigosSVRS.contains("200034")) {
                cClass = "200034";
                enquadramento = "Alimentos Consumo Humano (Anexo VII)";
                cst = "200";
                aliq = "Redução de 60%";
                alimentosRed++;
            } else {
                padrao++;
            }

            if (amostra.size() < 25) {
                amostra.add(new SpedDiagnostico.ItemSpedMapeado(cod, descr, ncm, cClass, enquadramento, cst, aliq));
            }
        }

        diag.setNcmsCestaBasica(cestaBasica);
        diag.setNcmsAlimentosReduzidos(alimentosRed);
        diag.setNcmsAliquotaPadrao(padrao);
        diag.setAmostraProdutos(amostra);

        this.ultimoDiagnostico = diag;
        return diag;
    }

    public SpedDiagnostico getUltimoDiagnostico() {
        return ultimoDiagnostico;
    }

    public void limpar() {
        this.ultimoDiagnostico = null;
        this.limpoPeloUsuario = true;
    }

    public boolean isLimpoPeloUsuario() {
        return limpoPeloUsuario;
    }

    public void setLimpoPeloUsuario(boolean limpo) {
        this.limpoPeloUsuario = limpo;
    }

    private BigDecimal toDecimal(String s) {
        if (s == null || s.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(s.trim().replace(".", "").replace(",", "."));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
