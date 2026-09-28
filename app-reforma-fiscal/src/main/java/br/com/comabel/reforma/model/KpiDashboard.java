package br.com.comabel.reforma.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class KpiDashboard {
    private int totalNotas;
    private int notasConformes;
    private int notasDivergentes;
    private int totalNotasCanceladas;
    private BigDecimal taxaConformidade = BigDecimal.ZERO;
    private BigDecimal valorTotalNotas = BigDecimal.ZERO;
    private BigDecimal valorTotalIcms = BigDecimal.ZERO;
    private BigDecimal baseCalculoIbsCbs = BigDecimal.ZERO;
    private BigDecimal valorTotalIbs = BigDecimal.ZERO;
    private BigDecimal valorTotalCbs = BigDecimal.ZERO;

    // Métricas Fornecedores (Entradas)
    private int totalEntradasFornecedor;
    private int conformesFornecedor;
    private BigDecimal taxaConformidadeFornecedor = BigDecimal.ZERO;
    private BigDecimal valorComprasFornecedor = BigDecimal.ZERO;

    // Métricas Comabel (Saídas)
    private int totalSaidasComabel;
    private int conformesComabel;
    private BigDecimal taxaConformidadeComabel = BigDecimal.ZERO;
    private BigDecimal valorVendasComabel = BigDecimal.ZERO;

    // Métricas Transferências (Matriz/Filial)
    private int totalTransferencias;
    private int conformesTransferencias;
    private BigDecimal valorTransferencias = BigDecimal.ZERO;

    // Métricas Uniformidade de Catálogo (NCMs)
    private int totalNcmsCruzados;
    private int ncmsUniformes;
    private int ncmsDivergentes;

    public KpiDashboard() {}

    public void recalcularGeral(int total, int conformes, BigDecimal vTotal, BigDecimal vIcms, BigDecimal vBcIbs, BigDecimal vIbs, BigDecimal vCbs) {
        this.totalNotas = total;
        this.notasConformes = conformes;
        this.notasDivergentes = total - conformes;
        if (total > 0) {
            this.taxaConformidade = BigDecimal.valueOf(conformes)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        } else {
            this.taxaConformidade = BigDecimal.ZERO;
        }
        this.valorTotalNotas = vTotal != null ? vTotal : BigDecimal.ZERO;
        this.valorTotalIcms = vIcms != null ? vIcms : BigDecimal.ZERO;
        this.baseCalculoIbsCbs = vBcIbs != null ? vBcIbs : BigDecimal.ZERO;
        this.valorTotalIbs = vIbs != null ? vIbs : BigDecimal.ZERO;
        this.valorTotalCbs = vCbs != null ? vCbs : BigDecimal.ZERO;
    }

    public void definirMetricasFornecedor(int total, int conformes, BigDecimal valor) {
        this.totalEntradasFornecedor = total;
        this.conformesFornecedor = conformes;
        this.valorComprasFornecedor = valor != null ? valor : BigDecimal.ZERO;
        if (total > 0) {
            this.taxaConformidadeFornecedor = BigDecimal.valueOf(conformes)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        } else {
            this.taxaConformidadeFornecedor = BigDecimal.ZERO;
        }
    }

    public void definirMetricasComabel(int total, int conformes, BigDecimal valor) {
        this.totalSaidasComabel = total;
        this.conformesComabel = conformes;
        this.valorVendasComabel = valor != null ? valor : BigDecimal.ZERO;
        if (total > 0) {
            this.taxaConformidadeComabel = BigDecimal.valueOf(conformes)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        } else {
            this.taxaConformidadeComabel = BigDecimal.ZERO;
        }
    }

    public void definirMetricasTransferencia(int total, int conformes, BigDecimal valor) {
        this.totalTransferencias = total;
        this.conformesTransferencias = conformes;
        this.valorTransferencias = valor != null ? valor : BigDecimal.ZERO;
    }

    public void definirMetricasUniformidade(int totalNcms, int uniformes, int divergentes) {
        this.totalNcmsCruzados = totalNcms;
        this.ncmsUniformes = uniformes;
        this.ncmsDivergentes = divergentes;
    }

    public int getTotalNotas() { return totalNotas; }
    public int getNotasConformes() { return notasConformes; }
    public int getNotasDivergentes() { return notasDivergentes; }
    public int getTotalNotasCanceladas() { return totalNotasCanceladas; }
    public void setTotalNotasCanceladas(int totalNotasCanceladas) { this.totalNotasCanceladas = totalNotasCanceladas; }
    public BigDecimal getTaxaConformidade() { return taxaConformidade; }
    public BigDecimal getValorTotalNotas() { return valorTotalNotas; }
    public BigDecimal getValorTotalIcms() { return valorTotalIcms; }
    public BigDecimal getBaseCalculoIbsCbs() { return baseCalculoIbsCbs; }
    public BigDecimal getValorTotalIbs() { return valorTotalIbs; }
    public BigDecimal getValorTotalCbs() { return valorTotalCbs; }
    public BigDecimal getValorTotalIbsCbs() { return valorTotalIbs.add(valorTotalCbs); }

    public int getTotalEntradasFornecedor() { return totalEntradasFornecedor; }
    public int getConformesFornecedor() { return conformesFornecedor; }
    public BigDecimal getTaxaConformidadeFornecedor() { return taxaConformidadeFornecedor; }
    public BigDecimal getValorComprasFornecedor() { return valorComprasFornecedor; }

    public int getTotalSaidasComabel() { return totalSaidasComabel; }
    public int getConformesComabel() { return conformesComabel; }
    public BigDecimal getTaxaConformidadeComabel() { return taxaConformidadeComabel; }
    public BigDecimal getValorVendasComabel() { return valorVendasComabel; }

    public int getTotalTransferencias() { return totalTransferencias; }
    public int getConformesTransferencias() { return conformesTransferencias; }
    public BigDecimal getValorTransferencias() { return valorTransferencias; }

    public int getTotalNcmsCruzados() { return totalNcmsCruzados; }
    public int getNcmsUniformes() { return ncmsUniformes; }
    public int getNcmsDivergentes() { return ncmsDivergentes; }
}
