package br.com.comabel.reforma.model;

import java.util.Set;
import java.util.TreeSet;

public class EmpresaAuditada {
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpjPrincipal;
    private String cnpjRaiz;
    private String uf;
    private String municipio;
    private String regimeTributario = "Regime Geral (Lucro Real / Presumido) - Transição IBS/CBS 2026";
    private String periodoAuditado;
    private Set<String> estabelecimentos = new TreeSet<>();
    private int totalNotasEmitidas;
    private int totalNotasRecebidas;

    public EmpresaAuditada() {
        limpar();
    }

    public void limpar() {
        this.razaoSocial = null;
        this.nomeFantasia = null;
        this.cnpjPrincipal = null;
        this.cnpjRaiz = null;
        this.uf = null;
        this.municipio = null;
        this.periodoAuditado = null;
        this.estabelecimentos.clear();
        this.totalNotasEmitidas = 0;
        this.totalNotasRecebidas = 0;
    }

    public boolean isIdentificada() {
        return razaoSocial != null && !razaoSocial.trim().isEmpty() && cnpjPrincipal != null && !cnpjPrincipal.trim().isEmpty();
    }

    public String getNomeExibicao() {
        if (nomeFantasia != null && !nomeFantasia.trim().isEmpty()) {
            return nomeFantasia;
        }
        if (razaoSocial != null && !razaoSocial.trim().isEmpty()) {
            return razaoSocial;
        }
        return "Empresa Auditada";
    }

    public String getNomeCurto() {
        if (nomeFantasia != null && !nomeFantasia.trim().isEmpty()) return nomeFantasia;
        if (razaoSocial != null && !razaoSocial.trim().isEmpty()) {
            String s = razaoSocial.split("-")[0].trim();
            if (s.length() > 28) {
                return s.substring(0, 25) + "...";
            }
            return s;
        }
        return "Empresa";
    }

    public String getCnpjChave() {
        if (cnpjPrincipal != null && !cnpjPrincipal.isEmpty()) {
            return cnpjPrincipal.replaceAll("\\D", "");
        }
        return cnpjRaiz != null ? cnpjRaiz : "TODAS";
    }

    public void atualizarComDados(String razao, String cnpj, String uf, String mun, String dhEmi) {
        if (razao != null && !razao.isEmpty()) {
            this.razaoSocial = razao;
        }
        if (cnpj != null && !cnpj.isEmpty()) {
            String clean = cnpj.replaceAll("\\D", "");
            if (clean.length() >= 8 && (this.cnpjRaiz == null || this.cnpjRaiz.isEmpty())) {
                this.cnpjRaiz = clean.substring(0, 8);
            }
            String formatado = formatarCnpj(cnpj);
            if (this.cnpjPrincipal == null || this.cnpjPrincipal.isEmpty()) {
                this.cnpjPrincipal = formatado;
            }
            if (mun != null && !mun.isEmpty()) {
                this.estabelecimentos.add(formatado + " (" + mun + "/" + (uf != null ? uf : "MA") + ")");
            } else {
                this.estabelecimentos.add(formatado);
            }
        }
        if (uf != null && !uf.isEmpty()) {
            this.uf = uf;
        }
        if (mun != null && !mun.isEmpty()) {
            this.municipio = mun;
        }
        if (dhEmi != null && dhEmi.length() >= 7) {
            String anoMes = dhEmi.substring(0, 7); // "2026-08"
            if (anoMes.contains("-")) {
                String[] p = anoMes.split("-");
                if (p.length == 2) {
                    this.periodoAuditado = formatarMesAno(p[1], p[0]);
                }
            }
        }
    }

    private String formatarMesAno(String mes, String ano) {
        String m;
        switch (mes) {
            case "01": m = "Janeiro"; break;
            case "02": m = "Fevereiro"; break;
            case "03": m = "Março"; break;
            case "04": m = "Abril"; break;
            case "05": m = "Maio"; break;
            case "06": m = "Junho"; break;
            case "07": m = "Julho"; break;
            case "08": m = "Agosto"; break;
            case "09": m = "Setembro"; break;
            case "10": m = "Outubro"; break;
            case "11": m = "Novembro"; break;
            case "12": m = "Dezembro"; break;
            default: m = mes;
        }
        return m + " / " + ano;
    }

    public static String formatarCnpj(String cnpj) {
        if (cnpj == null) return "";
        String c = cnpj.replaceAll("\\D", "");
        if (c.length() == 14) {
            return String.format("%s.%s.%s/%s-%s",
                    c.substring(0, 2), c.substring(2, 5), c.substring(5, 8),
                    c.substring(8, 12), c.substring(12, 14));
        }
        return cnpj;
    }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getNomeFantasia() { return nomeFantasia; }
    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }

    public String getCnpjPrincipal() { return cnpjPrincipal; }
    public void setCnpjPrincipal(String cnpjPrincipal) { this.cnpjPrincipal = cnpjPrincipal; }

    public String getCnpjRaiz() { return cnpjRaiz; }
    public void setCnpjRaiz(String cnpjRaiz) { this.cnpjRaiz = cnpjRaiz; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }

    public String getRegimeTributario() { return regimeTributario; }
    public void setRegimeTributario(String regimeTributario) { this.regimeTributario = regimeTributario; }

    public String getPeriodoAuditado() { return periodoAuditado; }
    public void setPeriodoAuditado(String periodoAuditado) { this.periodoAuditado = periodoAuditado; }

    public Set<String> getEstabelecimentos() { return estabelecimentos; }
    public void setEstabelecimentos(Set<String> estabelecimentos) { this.estabelecimentos = estabelecimentos; }

    public int getTotalNotasEmitidas() { return totalNotasEmitidas; }
    public void setTotalNotasEmitidas(int totalNotasEmitidas) { this.totalNotasEmitidas = totalNotasEmitidas; }

    public int getTotalNotasRecebidas() { return totalNotasRecebidas; }
    public void setTotalNotasRecebidas(int totalNotasRecebidas) { this.totalNotasRecebidas = totalNotasRecebidas; }
}
