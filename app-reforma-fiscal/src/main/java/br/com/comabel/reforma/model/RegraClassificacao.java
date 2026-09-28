package br.com.comabel.reforma.model;

import java.util.ArrayList;
import java.util.List;

public class RegraClassificacao {
    private String codigo;
    private String cstEsperado;
    private String nome;
    private Double percRedIbs;
    private Double percRedCbs;
    private Integer tipoAliq;
    private List<String> ncmsPermitidos = new ArrayList<>();

    public RegraClassificacao() {}

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getCstEsperado() { return cstEsperado; }
    public void setCstEsperado(String cstEsperado) { this.cstEsperado = cstEsperado; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Double getPercRedIbs() { return percRedIbs; }
    public void setPercRedIbs(Double percRedIbs) { this.percRedIbs = percRedIbs; }

    public Double getPercRedCbs() { return percRedCbs; }
    public void setPercRedCbs(Double percRedCbs) { this.percRedCbs = percRedCbs; }

    public Integer getTipoAliq() { return tipoAliq; }
    public void setTipoAliq(Integer tipoAliq) { this.tipoAliq = tipoAliq; }

    public List<String> getNcmsPermitidos() { return ncmsPermitidos; }
    public void setNcmsPermitidos(List<String> ncmsPermitidos) { this.ncmsPermitidos = ncmsPermitidos; }
}
