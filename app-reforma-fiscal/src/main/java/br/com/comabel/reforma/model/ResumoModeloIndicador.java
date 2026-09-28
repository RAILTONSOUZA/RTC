package br.com.comabel.reforma.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ResumoModeloIndicador {
    private String modelo;
    private String titulo;
    private String corTema = "#38bdf8";
    private int totalNotas;
    private BigDecimal valorTotalNotas = BigDecimal.ZERO;
    private BigDecimal totalIbsApurado = BigDecimal.ZERO;
    private BigDecimal totalCbsApurada = BigDecimal.ZERO;
    private List<LinhaDetalhamentoCst> linhasCst = new ArrayList<>();
    private List<LinhaDetalhamentoCClass> linhasCClass = new ArrayList<>();

    public ResumoModeloIndicador() {}

    public ResumoModeloIndicador(String modelo, String titulo, String corTema) {
        this.modelo = modelo;
        this.titulo = titulo;
        this.corTema = corTema;
    }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getCorTema() { return corTema; }
    public void setCorTema(String corTema) { this.corTema = corTema; }

    public int getTotalNotas() { return totalNotas; }
    public void setTotalNotas(int totalNotas) { this.totalNotas = totalNotas; }

    public BigDecimal getValorTotalNotas() { return valorTotalNotas; }
    public void setValorTotalNotas(BigDecimal valorTotalNotas) { this.valorTotalNotas = valorTotalNotas; }

    public BigDecimal getTotalIbsApurado() { return totalIbsApurado; }
    public void setTotalIbsApurado(BigDecimal totalIbsApurado) { this.totalIbsApurado = totalIbsApurado; }

    public BigDecimal getTotalCbsApurada() { return totalCbsApurada; }
    public void setTotalCbsApurada(BigDecimal totalCbsApurada) { this.totalCbsApurada = totalCbsApurada; }

    public List<LinhaDetalhamentoCst> getLinhasCst() { return linhasCst; }
    public void setLinhasCst(List<LinhaDetalhamentoCst> linhasCst) { this.linhasCst = linhasCst; }

    public List<LinhaDetalhamentoCClass> getLinhasCClass() { return linhasCClass; }
    public void setLinhasCClass(List<LinhaDetalhamentoCClass> linhasCClass) { this.linhasCClass = linhasCClass; }
}
