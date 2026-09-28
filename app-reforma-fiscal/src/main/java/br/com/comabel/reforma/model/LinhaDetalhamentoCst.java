package br.com.comabel.reforma.model;

import java.math.BigDecimal;

public class LinhaDetalhamentoCst {
    private String cst;
    private String descricao;
    private int qtdItens;
    private BigDecimal vBc = BigDecimal.ZERO;
    private BigDecimal vIbs = BigDecimal.ZERO;
    private BigDecimal vCbs = BigDecimal.ZERO;

    public LinhaDetalhamentoCst() {}

    public LinhaDetalhamentoCst(String cst, String descricao) {
        this.cst = cst;
        this.descricao = descricao;
    }

    public void somarItem(BigDecimal vBcItem, BigDecimal vIbsItem, BigDecimal vCbsItem) {
        this.qtdItens++;
        if (vBcItem != null) this.vBc = this.vBc.add(vBcItem);
        if (vIbsItem != null) this.vIbs = this.vIbs.add(vIbsItem);
        if (vCbsItem != null) this.vCbs = this.vCbs.add(vCbsItem);
    }

    public String getCst() { return cst; }
    public void setCst(String cst) { this.cst = cst; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public int getQtdItens() { return qtdItens; }
    public void setQtdItens(int qtdItens) { this.qtdItens = qtdItens; }

    public BigDecimal getvBc() { return vBc; }
    public void setvBc(BigDecimal vBc) { this.vBc = vBc; }

    public BigDecimal getvIbs() { return vIbs; }
    public void setvIbs(BigDecimal vIbs) { this.vIbs = vIbs; }

    public BigDecimal getvCbs() { return vCbs; }
    public void setvCbs(BigDecimal vCbs) { this.vCbs = vCbs; }
}
