package br.com.comabel.reforma.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "nota_fiscal_resumo")
public class NotaFiscalResumo {

    @Id
    @Column(length = 44)
    private String chNFe;

    private String nNF;
    private String serie;
    private String dhEmi;
    private String tpNF;
    private String emitCnpj;

    @Column(length = 300)
    private String emitNome;

    private String emitUf;
    private String destCnpj;

    @Column(length = 300)
    private String destNome;

    private String destUf;
    private String tipoFluxo = "ENTRADA_FORNECEDOR"; // ENTRADA_FORNECEDOR, SAIDA_COMABEL, TRANSFERENCIA_INTERNA, OUTROS

    @Column(precision = 15, scale = 2)
    private BigDecimal vNF = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal vIcms = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal vBcIbsCbs = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal vIbs = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal vCbs = BigDecimal.ZERO;

    private String modelo = "55"; // 55 = NF-e, 65 = NFC-e
    private boolean cancelada = false;
    private int totalItens;
    private int itensDivergentes;
    private String status = "CONFORME";

    @Column(length = 500)
    private String obsTotais = "Totais conciliados";

    @Column(length = 2000)
    private String correcaoSugerida = "";

    @OneToMany(mappedBy = "notaFiscal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemAuditoria> itens = new ArrayList<>();

    public NotaFiscalResumo() {}

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public boolean isCancelada() { return cancelada; }
    public void setCancelada(boolean cancelada) { this.cancelada = cancelada; }

    public String getChNFe() { return chNFe; }
    public void setChNFe(String chNFe) { this.chNFe = chNFe; }

    public String getnNF() { return nNF; }
    public void setnNF(String nNF) { this.nNF = nNF; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getDhEmi() { return dhEmi; }
    public void setDhEmi(String dhEmi) { this.dhEmi = dhEmi; }

    public String getTpNF() { return tpNF; }
    public void setTpNF(String tpNF) { this.tpNF = tpNF; }

    public String getEmitCnpj() { return emitCnpj; }
    public void setEmitCnpj(String emitCnpj) { this.emitCnpj = emitCnpj; }

    public String getEmitNome() { return emitNome; }
    public void setEmitNome(String emitNome) { this.emitNome = emitNome; }

    public String getEmitUf() { return emitUf; }
    public void setEmitUf(String emitUf) { this.emitUf = emitUf; }

    public String getDestCnpj() { return destCnpj; }
    public void setDestCnpj(String destCnpj) { this.destCnpj = destCnpj; }

    public String getDestNome() { return destNome; }
    public void setDestNome(String destNome) { this.destNome = destNome; }

    public String getDestUf() { return destUf; }
    public void setDestUf(String destUf) { this.destUf = destUf; }

    public String getTipoFluxo() { return tipoFluxo; }
    public void setTipoFluxo(String tipoFluxo) { this.tipoFluxo = tipoFluxo; }

    public String getTipoFluxoDescricao() {
        if ("ENTRADA_FORNECEDOR".equals(tipoFluxo)) return "Entrada (Fornecedor)";
        if ("SAIDA_COMABEL".equals(tipoFluxo)) return "Saída (Empresa)";
        if ("TRANSFERENCIA_INTERNA".equals(tipoFluxo)) return "Transferência (Matriz/Filial)";
        return "Outros Fluxos";
    }

    public BigDecimal getvNF() { return vNF; }
    public void setvNF(BigDecimal vNF) { this.vNF = vNF; }

    public BigDecimal getvIcms() { return vIcms; }
    public void setvIcms(BigDecimal vIcms) { this.vIcms = vIcms; }

    public BigDecimal getvBcIbsCbs() { return vBcIbsCbs; }
    public void setvBcIbsCbs(BigDecimal vBcIbsCbs) { this.vBcIbsCbs = vBcIbsCbs; }

    public BigDecimal getvIbs() { return vIbs; }
    public void setvIbs(BigDecimal vIbs) { this.vIbs = vIbs; }

    public BigDecimal getvCbs() { return vCbs; }
    public void setvCbs(BigDecimal vCbs) { this.vCbs = vCbs; }

    public int getTotalItens() { return totalItens; }
    public void setTotalItens(int totalItens) { this.totalItens = totalItens; }

    public int getItensDivergentes() { return itensDivergentes; }
    public void setItensDivergentes(int itensDivergentes) { this.itensDivergentes = itensDivergentes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObsTotais() { return obsTotais; }
    public void setObsTotais(String obsTotais) { this.obsTotais = obsTotais; }

    public String getCorrecaoSugerida() { return correcaoSugerida; }
    public void setCorrecaoSugerida(String correcaoSugerida) { this.correcaoSugerida = correcaoSugerida; }

    public List<ItemAuditoria> getItens() { return itens; }
    public void setItens(List<ItemAuditoria> itens) {
        this.itens = itens;
        if (itens != null) {
            for (ItemAuditoria it : itens) {
                it.setNotaFiscal(this);
            }
        }
    }

    public void addItem(ItemAuditoria item) {
        if (item != null) {
            this.itens.add(item);
            item.setNotaFiscal(this);
        }
    }
}
