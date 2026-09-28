package br.com.comabel.reforma.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SpedDiagnostico {
    private String razaoSocial;
    private String cnpj;
    private String uf;
    private String periodo;
    private int totalLinhas;
    private int totalProdutos0200;
    private int totalNcmsDistintos;
    
    // Categorização dos Produtos na Reforma
    private int ncmsCestaBasica; // Anexo I (Alíquota Zero - 200003)
    private int ncmsAlimentosReduzidos; // Anexo VII (Redução 60% - 200034)
    private int ncmsAliquotaPadrao;
    
    // Movimento Fiscal do Mês
    private int totalNotasEntrada;
    private BigDecimal valorCompras = BigDecimal.ZERO;
    private int totalNotasSaida;
    private BigDecimal valorVendas = BigDecimal.ZERO;
    
    // ICMS Apuração
    private BigDecimal icmsDebitos = BigDecimal.ZERO;
    private BigDecimal icmsCreditos = BigDecimal.ZERO;
    private BigDecimal icmsRecolher = BigDecimal.ZERO;
    
    // Vendas por Regime Atual
    private BigDecimal vendasReducaoBaseCst020 = BigDecimal.ZERO;
    private BigDecimal vendasSubstTribCst060 = BigDecimal.ZERO;
    private BigDecimal vendasTributacaoIntegralCst000 = BigDecimal.ZERO;
    private BigDecimal vendasTransferencias = BigDecimal.ZERO;
    
    // Amostra de itens mapeados
    private List<ItemSpedMapeado> amostraProdutos = new ArrayList<>();

    public SpedDiagnostico() {}

    public static class ItemSpedMapeado {
        private String codItem;
        private String descricao;
        private String ncm;
        private String cClassTribSugerido;
        private String enquadramentoReforma;
        private String cstSugerido;
        private String aliquotaEfetiva;

        public ItemSpedMapeado(String codItem, String descricao, String ncm, String cClassTribSugerido, String enquadramentoReforma, String cstSugerido, String aliquotaEfetiva) {
            this.codItem = codItem;
            this.descricao = descricao;
            this.ncm = ncm;
            this.cClassTribSugerido = cClassTribSugerido;
            this.enquadramentoReforma = enquadramentoReforma;
            this.cstSugerido = cstSugerido;
            this.aliquotaEfetiva = aliquotaEfetiva;
        }

        public String getCodItem() { return codItem; }
        public String getDescricao() { return descricao; }
        public String getNcm() { return ncm; }
        public String getcClassTribSugerido() { return cClassTribSugerido; }
        public String getEnquadramentoReforma() { return enquadramentoReforma; }
        public String getCstSugerido() { return cstSugerido; }
        public String getAliquotaEfetiva() { return aliquotaEfetiva; }
    }

    public String getRazaoSocial() { return razaoSocial; }
    public void setRazaoSocial(String razaoSocial) { this.razaoSocial = razaoSocial; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }

    public String getPeriodo() { return periodo; }
    public void setPeriodo(String periodo) { this.periodo = periodo; }

    public int getTotalLinhas() { return totalLinhas; }
    public void setTotalLinhas(int totalLinhas) { this.totalLinhas = totalLinhas; }

    public int getTotalProdutos0200() { return totalProdutos0200; }
    public void setTotalProdutos0200(int totalProdutos0200) { this.totalProdutos0200 = totalProdutos0200; }

    public int getTotalNcmsDistintos() { return totalNcmsDistintos; }
    public void setTotalNcmsDistintos(int totalNcmsDistintos) { this.totalNcmsDistintos = totalNcmsDistintos; }

    public int getNcmsCestaBasica() { return ncmsCestaBasica; }
    public void setNcmsCestaBasica(int ncmsCestaBasica) { this.ncmsCestaBasica = ncmsCestaBasica; }

    public int getNcmsAlimentosReduzidos() { return ncmsAlimentosReduzidos; }
    public void setNcmsAlimentosReduzidos(int ncmsAlimentosReduzidos) { this.ncmsAlimentosReduzidos = ncmsAlimentosReduzidos; }

    public int getNcmsAliquotaPadrao() { return ncmsAliquotaPadrao; }
    public void setNcmsAliquotaPadrao(int ncmsAliquotaPadrao) { this.ncmsAliquotaPadrao = ncmsAliquotaPadrao; }

    public int getTotalNotasEntrada() { return totalNotasEntrada; }
    public void setTotalNotasEntrada(int totalNotasEntrada) { this.totalNotasEntrada = totalNotasEntrada; }

    public BigDecimal getValorCompras() { return valorCompras; }
    public void setValorCompras(BigDecimal valorCompras) { this.valorCompras = valorCompras; }

    public int getTotalNotasSaida() { return totalNotasSaida; }
    public void setTotalNotasSaida(int totalNotasSaida) { this.totalNotasSaida = totalNotasSaida; }

    public BigDecimal getValorVendas() { return valorVendas; }
    public void setValorVendas(BigDecimal valorVendas) { this.valorVendas = valorVendas; }

    public BigDecimal getIcmsDebitos() { return icmsDebitos; }
    public void setIcmsDebitos(BigDecimal icmsDebitos) { this.icmsDebitos = icmsDebitos; }

    public BigDecimal getIcmsCreditos() { return icmsCreditos; }
    public void setIcmsCreditos(BigDecimal icmsCreditos) { this.icmsCreditos = icmsCreditos; }

    public BigDecimal getIcmsRecolher() { return icmsRecolher; }
    public void setIcmsRecolher(BigDecimal icmsRecolher) { this.icmsRecolher = icmsRecolher; }

    public BigDecimal getVendasReducaoBaseCst020() { return vendasReducaoBaseCst020; }
    public void setVendasReducaoBaseCst020(BigDecimal vendasReducaoBaseCst020) { this.vendasReducaoBaseCst020 = vendasReducaoBaseCst020; }

    public BigDecimal getVendasSubstTribCst060() { return vendasSubstTribCst060; }
    public void setVendasSubstTribCst060(BigDecimal vendasSubstTribCst060) { this.vendasSubstTribCst060 = vendasSubstTribCst060; }

    public BigDecimal getVendasTributacaoIntegralCst000() { return vendasTributacaoIntegralCst000; }
    public void setVendasTributacaoIntegralCst000(BigDecimal vendasTributacaoIntegralCst000) { this.vendasTributacaoIntegralCst000 = vendasTributacaoIntegralCst000; }

    public BigDecimal getVendasTransferencias() { return vendasTransferencias; }
    public void setVendasTransferencias(BigDecimal vendasTransferencias) { this.vendasTransferencias = vendasTransferencias; }

    public List<ItemSpedMapeado> getAmostraProdutos() { return amostraProdutos; }
    public void setAmostraProdutos(List<ItemSpedMapeado> amostraProdutos) { this.amostraProdutos = amostraProdutos; }
}
