package br.com.comabel.reforma.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item_auditoria")
public class ItemAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 44)
    private String chNFe;
    
    private String modelo = "55";
    private String nNF;
    private String dhEmi;
    private String nItem;
    private String cProd;
    
    @Column(length = 600)
    private String xProd;
    
    private String ncm;
    private String cfop;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal vProd = BigDecimal.ZERO;
    
    // ICMS
    private String cstIcms = "";
    
    @Column(precision = 15, scale = 2)
    private BigDecimal vIcms = BigDecimal.ZERO;
    
    // IBS / CBS
    private boolean hasIbsCbs;
    private String cstIbsCbs = "";
    private String cClassTrib = "";
    
    @Column(precision = 15, scale = 2)
    private BigDecimal vBcIbsCbs = BigDecimal.ZERO;
    
    @Column(precision = 8, scale = 2)
    private BigDecimal pRedAliqIbs = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal vIbs = BigDecimal.ZERO;
    
    @Column(precision = 8, scale = 2)
    private BigDecimal pRedAliqCbs = BigDecimal.ZERO;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal vCbs = BigDecimal.ZERO;
    
    // Auditoria & Correção Sugerida
    private String status = "CONFORME";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "item_apontamento", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "apontamento", length = 1000)
    private List<String> apontamentos = new ArrayList<>();

    @Column(length = 2000)
    private String correcaoSugerida = "";
    
    private String cClassTribSugerido = "";
    private String cstIbsCbsSugerido = "";
    
    @Column(precision = 8, scale = 2)
    private BigDecimal pRedSugerido = BigDecimal.ZERO;
    
    @Column(length = 600)
    private String fundamentacaoSugerida = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ch_nfe_fk")
    @JsonIgnore
    private NotaFiscalResumo notaFiscal;

    public ItemAuditoria() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NotaFiscalResumo getNotaFiscal() { return notaFiscal; }
    public void setNotaFiscal(NotaFiscalResumo notaFiscal) { this.notaFiscal = notaFiscal; }

    public String getChNFe() { return chNFe; }
    public void setChNFe(String chNFe) { this.chNFe = chNFe; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getnNF() { return nNF; }
    public void setnNF(String nNF) { this.nNF = nNF; }

    public String getDhEmi() { return dhEmi; }
    public void setDhEmi(String dhEmi) { this.dhEmi = dhEmi; }

    public String getnItem() { return nItem; }
    public void setnItem(String nItem) { this.nItem = nItem; }

    public String getcProd() { return cProd; }
    public void setcProd(String cProd) { this.cProd = cProd; }

    public String getxProd() { return xProd; }
    public void setxProd(String xProd) { this.xProd = xProd; }

    public String getNcm() { return ncm; }
    public void setNcm(String ncm) { this.ncm = ncm; }

    public String getCfop() { return cfop; }
    public void setCfop(String cfop) { this.cfop = cfop; }

    public BigDecimal getvProd() { return vProd; }
    public void setvProd(BigDecimal vProd) { this.vProd = vProd; }

    public String getCstIcms() { return cstIcms; }
    public void setCstIcms(String cstIcms) { this.cstIcms = cstIcms; }

    public BigDecimal getvIcms() { return vIcms; }
    public void setvIcms(BigDecimal vIcms) { this.vIcms = vIcms; }

    public boolean isHasIbsCbs() { return hasIbsCbs; }
    public void setHasIbsCbs(boolean hasIbsCbs) { this.hasIbsCbs = hasIbsCbs; }

    public String getCstIbsCbs() { return cstIbsCbs; }
    public void setCstIbsCbs(String cstIbsCbs) { this.cstIbsCbs = cstIbsCbs; }

    public String getcClassTrib() { return cClassTrib; }
    public void setcClassTrib(String cClassTrib) { this.cClassTrib = cClassTrib; }

    public BigDecimal getvBcIbsCbs() { return vBcIbsCbs; }
    public void setvBcIbsCbs(BigDecimal vBcIbsCbs) { this.vBcIbsCbs = vBcIbsCbs; }

    public BigDecimal getpRedAliqIbs() { return pRedAliqIbs; }
    public void setpRedAliqIbs(BigDecimal pRedAliqIbs) { this.pRedAliqIbs = pRedAliqIbs; }

    public BigDecimal getvIbs() { return vIbs; }
    public void setvIbs(BigDecimal vIbs) { this.vIbs = vIbs; }

    public BigDecimal getpRedAliqCbs() { return pRedAliqCbs; }
    public void setpRedAliqCbs(BigDecimal pRedAliqCbs) { this.pRedAliqCbs = pRedAliqCbs; }

    public BigDecimal getvCbs() { return vCbs; }
    public void setvCbs(BigDecimal vCbs) { this.vCbs = vCbs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getApontamentos() { return apontamentos; }
    public void setApontamentos(List<String> apontamentos) { this.apontamentos = apontamentos; }

    public String getCorrecaoSugerida() { return correcaoSugerida; }
    public void setCorrecaoSugerida(String correcaoSugerida) { this.correcaoSugerida = correcaoSugerida; }

    public String getcClassTribSugerido() { return cClassTribSugerido; }
    public void setcClassTribSugerido(String cClassTribSugerido) { this.cClassTribSugerido = cClassTribSugerido; }

    public String getCstIbsCbsSugerido() { return cstIbsCbsSugerido; }
    public void setCstIbsCbsSugerido(String cstIbsCbsSugerido) { this.cstIbsCbsSugerido = cstIbsCbsSugerido; }

    public BigDecimal getpRedSugerido() { return pRedSugerido; }
    public void setpRedSugerido(BigDecimal pRedSugerido) { this.pRedSugerido = pRedSugerido; }

    public String getFundamentacaoSugerida() { return fundamentacaoSugerida; }
    public void setFundamentacaoSugerida(String fundamentacaoSugerida) { this.fundamentacaoSugerida = fundamentacaoSugerida; }

    public String getOcorrenciasFormatadas() {
        if (apontamentos == null || apontamentos.isEmpty()) {
            return "Nenhuma divergência identificada";
        }
        return String.join("; ", apontamentos);
    }
}
