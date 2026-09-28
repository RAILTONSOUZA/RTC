package br.com.comabel.reforma.service;

import br.com.comabel.reforma.model.RegraClassificacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class RegrasTributariasService {

    private final Map<String, RegraClassificacao> regrasPorCodigo = new HashMap<>();
    private final Map<String, List<String>> ncmParaCodigos = new HashMap<>();
    private final List<RegraClassificacao> listaCompleta = new ArrayList<>();

    @PostConstruct
    public void carregarRegras() {
        try {
            ClassPathResource resource = new ClassPathResource("regras_cclass_svrs.json");
            ObjectMapper mapper = new ObjectMapper();
            try (InputStream is = resource.getInputStream()) {
                JsonNode root = mapper.readTree(is);
                JsonNode classifs = root.get("classificacoes");
                if (classifs != null && classifs.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> fields = classifs.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        String cod = entry.getKey();
                        JsonNode node = entry.getValue();

                        RegraClassificacao r = new RegraClassificacao();
                        r.setCodigo(cod);
                        r.setCstEsperado(node.has("cst_esperado") ? node.get("cst_esperado").asText() : "");
                        r.setNome(node.has("nome") ? node.get("nome").asText() : "");
                        r.setPercRedIbs(node.has("perc_red_ibs") ? node.get("perc_red_ibs").asDouble() : 0.0);
                        r.setPercRedCbs(node.has("perc_red_cbs") ? node.get("perc_red_cbs").asDouble() : 0.0);
                        r.setTipoAliq(node.has("tipo_aliq") ? node.get("tipo_aliq").asInt() : 2);

                        List<String> ncms = new ArrayList<>();
                        JsonNode ncmsNode = node.get("ncms_permitidos");
                        if (ncmsNode != null && ncmsNode.isArray()) {
                            for (JsonNode n : ncmsNode) {
                                ncms.add(n.asText().trim());
                            }
                        }
                        r.setNcmsPermitidos(ncms);

                        regrasPorCodigo.put(cod, r);
                        listaCompleta.add(r);
                    }
                }

                JsonNode ncmMapNode = root.get("ncm_map");
                if (ncmMapNode != null && ncmMapNode.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> ncmFields = ncmMapNode.fields();
                    while (ncmFields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = ncmFields.next();
                        String ncm = entry.getKey();
                        List<String> cods = new ArrayList<>();
                        for (JsonNode c : entry.getValue()) {
                            cods.add(c.asText());
                        }
                        ncmParaCodigos.put(ncm, cods);
                    }
                }
            }
            listaCompleta.sort(Comparator.comparing(RegraClassificacao::getCodigo));
            System.out.println("[REGRAS SVRS] Carregadas " + regrasPorCodigo.size() + " classificações e " + ncmParaCodigos.size() + " mapeamentos de NCM.");
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao carregar regras_cclass_svrs.json: " + e.getMessage());
        }
    }

    public RegraClassificacao getRegra(String codigo) {
        if (codigo == null) return null;
        String codFormatado = codigo.trim();
        if (codFormatado.length() < 6 && codFormatado.matches("\\d+")) {
            codFormatado = String.format("%06d", Integer.parseInt(codFormatado));
        }
        return regrasPorCodigo.get(codFormatado);
    }

    public List<RegraClassificacao> getListaCompleta() {
        return listaCompleta;
    }

    public List<RegraClassificacao> buscar(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return listaCompleta;
        }
        String t = termo.toLowerCase().trim();
        List<RegraClassificacao> filtrados = new ArrayList<>();
        for (RegraClassificacao r : listaCompleta) {
            if (r.getCodigo().contains(t) || 
                (r.getNome() != null && r.getNome().toLowerCase().contains(t)) ||
                (r.getCstEsperado() != null && r.getCstEsperado().contains(t)) ||
                r.getNcmsPermitidos().stream().anyMatch(n -> n.contains(t))) {
                filtrados.add(r);
            }
        }
        return filtrados;
    }

    public boolean isNcmPermitido(String cClassTrib, String ncm) {
        RegraClassificacao r = getRegra(cClassTrib);
        if (r == null || r.getNcmsPermitidos().isEmpty()) {
            return true; // Se a classificação não possui anexo taxativo de NCMs, aceita geral
        }
        if (ncm == null) return false;
        String ncmLimpo = ncm.replaceAll("\\D", "").trim();
        if (r.getNcmsPermitidos().contains(ncmLimpo)) {
            return true;
        }
        // Valida se o prefixo de 4 dígitos (posição SH) bate
        if (ncmLimpo.length() >= 4) {
            String p4 = ncmLimpo.substring(0, 4);
            return r.getNcmsPermitidos().stream().anyMatch(n -> n.startsWith(p4));
        }
        return false;
    }

    public List<String> getCodigosPorNcm(String ncm) {
        if (ncm == null) return Collections.emptyList();
        String ncmLimpo = ncm.replaceAll("\\D", "").trim();
        List<String> diretos = ncmParaCodigos.get(ncmLimpo);
        if (diretos != null && !diretos.isEmpty()) {
            return diretos;
        }
        // Tenta por prefixo de 4 dígitos
        if (ncmLimpo.length() >= 4) {
            String p4 = ncmLimpo.substring(0, 4);
            for (Map.Entry<String, List<String>> entry : ncmParaCodigos.entrySet()) {
                if (entry.getKey().startsWith(p4)) {
                    return entry.getValue();
                }
            }
        }
        return Collections.emptyList();
    }
}
