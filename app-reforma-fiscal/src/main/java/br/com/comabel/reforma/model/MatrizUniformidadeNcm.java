package br.com.comabel.reforma.model;

public class MatrizUniformidadeNcm {
    private String ncm;
    private String descricao;
    private String cClassEntrada;
    private String cstEntrada;
    private String cClassSaida;
    private String cstSaida;
    private String status; // UNIFORME, DIVERGENTE, APENAS_ENTRADA, APENAS_SAIDA
    private String observacao;

    public MatrizUniformidadeNcm() {}

    public MatrizUniformidadeNcm(String ncm, String descricao, String cClassEntrada, String cstEntrada, String cClassSaida, String cstSaida, String status, String observacao) {
        this.ncm = ncm;
        this.descricao = descricao;
        this.cClassEntrada = cClassEntrada;
        this.cstEntrada = cstEntrada;
        this.cClassSaida = cClassSaida;
        this.cstSaida = cstSaida;
        this.status = status;
        this.observacao = observacao;
    }

    public String getNcm() { return ncm; }
    public void setNcm(String ncm) { this.ncm = ncm; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getcClassEntrada() { return cClassEntrada; }
    public void setcClassEntrada(String cClassEntrada) { this.cClassEntrada = cClassEntrada; }

    public String getCstEntrada() { return cstEntrada; }
    public void setCstEntrada(String cstEntrada) { this.cstEntrada = cstEntrada; }

    public String getcClassSaida() { return cClassSaida; }
    public void setcClassSaida(String cClassSaida) { this.cClassSaida = cClassSaida; }

    public String getCstSaida() { return cstSaida; }
    public void setCstSaida(String cstSaida) { this.cstSaida = cstSaida; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}
