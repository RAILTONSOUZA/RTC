package br.com.comabel.reforma.controller;

import br.com.comabel.reforma.service.XmlAuditorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired
    private XmlAuditorService auditorService;

    @Value("${app.default-scan-dir:c:/agente/agente}")
    private String defaultScanDir;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("kpis", auditorService.getKpiDashboard());
        model.addAttribute("empresaAuditada", auditorService.getEmpresaAuditada());
        model.addAttribute("ultimasNotas", auditorService.getNotasEmMemoria().stream().limit(10).toList());
        model.addAttribute("matrizUniformidade", auditorService.getMatrizUniformidade().stream().limit(15).toList());
        model.addAttribute("totalNotas", auditorService.getNotasEmMemoria().size());
        model.addAttribute("pastaAtual", auditorService.getUltimaPastaEscaneada());
        model.addAttribute("activeMenu", "dashboard");
        return "dashboard";
    }
}
