package br.com.comabel.reforma.controller;

import br.com.comabel.reforma.model.EmpresaAuditada;
import br.com.comabel.reforma.service.XmlAuditorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private XmlAuditorService auditorService;

    @ModelAttribute("empresaAuditada")
    public EmpresaAuditada addEmpresaAuditada() {
        return auditorService.getEmpresaAuditada();
    }

    @ModelAttribute("todasEmpresas")
    public List<EmpresaAuditada> addTodasEmpresas() {
        return auditorService.getEmpresasDetectadas();
    }

    @ModelAttribute("cnpjAtivo")
    public String addCnpjAtivo() {
        return auditorService.getCnpjAtivo();
    }

    @ModelAttribute("cnpjInformado")
    public String addCnpjInformado() {
        return auditorService.getCnpjInformado();
    }
}
