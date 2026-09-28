package br.com.comabel.reforma.service;

import br.com.comabel.reforma.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class IndicadoresService {

    @Autowired
    private XmlAuditorService auditorService;

    @Autowired
    private RegrasTributariasService regrasService;

    public List<ResumoModeloIndicador> calcularIndicadoresPorModelo(String filtroFluxo) {
        List<NotaFiscalResumo> ativas = auditorService.getNotasAtivas();

        if (filtroFluxo != null && !filtroFluxo.isEmpty() && !"TODOS".equalsIgnoreCase(filtroFluxo)) {
            ativas = ativas.stream()
                    .filter(n -> filtroFluxo.equalsIgnoreCase(n.getTipoFluxo()))
                    .toList();
        }

        List<ResumoModeloIndicador> lista = new ArrayList<>();

        // 1. Modelo 55 (NF-e)
        List<NotaFiscalResumo> nfeNotas = ativas.stream()
                .filter(n -> "55".equals(n.getModelo()))
                .toList();
        lista.add(construirResumo("55", "NF-e (Modelo 55 - Emissão Normal)", "#38bdf8", nfeNotas));

        // 2. Modelo 65 (NFC-e)
        List<NotaFiscalResumo> nfceNotas = ativas.stream()
                .filter(n -> "65".equals(n.getModelo()))
                .toList();
        lista.add(construirResumo("65", "NFC-e (Modelo 65 - Consumidor Final)", "#f59e0b", nfceNotas));

        return lista;
    }

    private ResumoModeloIndicador construirResumo(String modelo, String titulo, String corTema, List<NotaFiscalResumo> notas) {
        ResumoModeloIndicador resumo = new ResumoModeloIndicador(modelo, titulo, corTema);
        resumo.setTotalNotas(notas.size());

        BigDecimal vTotalNotas = BigDecimal.ZERO;
        BigDecimal vTotalIbs = BigDecimal.ZERO;
        BigDecimal vTotalCbs = BigDecimal.ZERO;

        Map<String, LinhaDetalhamentoCst> mapCst = new LinkedHashMap<>();
        Map<String, LinhaDetalhamentoCClass> mapCClass = new LinkedHashMap<>();

        for (NotaFiscalResumo nf : notas) {
            if (nf.getvNF() != null) {
                vTotalNotas = vTotalNotas.add(nf.getvNF());
            }

            if (nf.getItens() != null) {
                for (ItemAuditoria it : nf.getItens()) {
                    BigDecimal vBc = it.getvBcIbsCbs() != null ? it.getvBcIbsCbs() : BigDecimal.ZERO;
                    BigDecimal vIbs = it.getvIbs() != null ? it.getvIbs() : BigDecimal.ZERO;
                    BigDecimal vCbs = it.getvCbs() != null ? it.getvCbs() : BigDecimal.ZERO;

                    vTotalIbs = vTotalIbs.add(vIbs);
                    vTotalCbs = vTotalCbs.add(vCbs);

                    // Agrupamento por CST
                    String cstKey = (it.getCstIbsCbs() != null && !it.getCstIbsCbs().trim().isEmpty())
                            ? "CST " + it.getCstIbsCbs().trim()
                            : "Sem CST";
                    
                    LinhaDetalhamentoCst linhaCst = mapCst.computeIfAbsent(cstKey, k -> new LinhaDetalhamentoCst(k, obterDescricaoCst(k)));
                    linhaCst.somarItem(vBc, vIbs, vCbs);

                    // Agrupamento por cClassTrib
                    String cClassKey = (it.getcClassTrib() != null && !it.getcClassTrib().trim().isEmpty())
                            ? "cClass " + it.getcClassTrib().trim()
                            : "Sem cClassTrib";

                    LinhaDetalhamentoCClass linhaCClass = mapCClass.computeIfAbsent(cClassKey, k -> new LinhaDetalhamentoCClass(k, obterDescricaoCClass(k)));
                    linhaCClass.somarItem(vBc, vIbs, vCbs);
                }
            }
        }

        resumo.setValorTotalNotas(vTotalNotas);
        resumo.setTotalIbsApurado(vTotalIbs);
        resumo.setTotalCbsApurada(vTotalCbs);

        // Ordenar CSTs: Sem CST primeiro, depois pela quantidade de itens decrescente
        List<LinhaDetalhamentoCst> listaCst = new ArrayList<>(mapCst.values());
        listaCst.sort((a, b) -> {
            if ("Sem CST".equals(a.getCst())) return -1;
            if ("Sem CST".equals(b.getCst())) return 1;
            return Integer.compare(b.getQtdItens(), a.getQtdItens());
        });
        resumo.setLinhasCst(listaCst);

        // Ordenar cClass: Sem cClassTrib primeiro, depois pela quantidade de itens decrescente
        List<LinhaDetalhamentoCClass> listaCClass = new ArrayList<>(mapCClass.values());
        listaCClass.sort((a, b) -> {
            if ("Sem cClassTrib".equals(a.getcClassTrib())) return -1;
            if ("Sem cClassTrib".equals(b.getcClassTrib())) return 1;
            return Integer.compare(b.getQtdItens(), a.getQtdItens());
        });
        resumo.setLinhasCClass(listaCClass);

        return resumo;
    }

    private String obterDescricaoCst(String cstKey) {
        if ("Sem CST".equalsIgnoreCase(cstKey)) {
            return "Sem CST";
        }
        String cod = cstKey.replace("CST", "").trim();
        return switch (cod) {
            case "000", "001" -> "Tributação integral";
            case "010" -> "Regime específico / financeiro";
            case "200" -> "Alíquota reduzida";
            case "410" -> "Imunidade e não incidência";
            case "620" -> "Tributação monofásica";
            case "900" -> "Outras situações tributárias";
            default -> "Operação tributada / CST " + cod;
        };
    }

    private String obterDescricaoCClass(String cClassKey) {
        if ("Sem cClassTrib".equalsIgnoreCase(cClassKey)) {
            return "Sem Classificação";
        }
        String cod = cClassKey.replace("cClass", "").trim();
        RegraClassificacao regra = regrasService.getRegra(cod);
        if (regra != null && regra.getNome() != null && !regra.getNome().isEmpty()) {
            return regra.getNome();
        }
        return "Classificação SVRS " + cod;
    }
}
