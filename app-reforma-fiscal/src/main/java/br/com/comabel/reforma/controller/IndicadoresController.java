package br.com.comabel.reforma.controller;

import br.com.comabel.reforma.model.ResumoModeloIndicador;
import br.com.comabel.reforma.service.IndicadoresService;
import br.com.comabel.reforma.service.XmlAuditorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class IndicadoresController {

    @Autowired
    private IndicadoresService indicadoresService;

    @Autowired
    private XmlAuditorService auditorService;

    @GetMapping({"/indicadores", "/kpis-avancados", "/indicadores-ibscbs"})
    public String exibirIndicadores(Model model,
                                    @RequestParam(value = "tipoFluxo", required = false, defaultValue = "TODOS") String tipoFluxo) {
        
        List<ResumoModeloIndicador> resumos = indicadoresService.calcularIndicadoresPorModelo(tipoFluxo);

        model.addAttribute("resumosModelos", resumos);
        model.addAttribute("empresaAuditada", auditorService.getEmpresaAuditada());
        model.addAttribute("kpis", auditorService.getKpiDashboard());
        model.addAttribute("filtroFluxoAtual", tipoFluxo);
        model.addAttribute("activeMenu", "indicadores");

        return "indicadores-ibscbs";
    }
}
