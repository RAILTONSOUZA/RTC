package br.com.comabel.reforma.controller;

import br.com.comabel.reforma.model.EmpresaAuditada;
import br.com.comabel.reforma.model.ItemAuditoria;
import br.com.comabel.reforma.model.KpiDashboard;
import br.com.comabel.reforma.model.NotaFiscalResumo;
import br.com.comabel.reforma.service.PdfExportService;
import br.com.comabel.reforma.service.XmlAuditorService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class RelatorioExecutivoController {

    @Autowired
    private XmlAuditorService auditorService;

    @Autowired
    private PdfExportService pdfExportService;

    @GetMapping("/selecionar-empresa")
    public String selecionarEmpresa(@RequestParam("cnpj") String cnpj, HttpServletRequest request) {
        auditorService.setCnpjAtivo(cnpj);
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isEmpty() ? referer : "/dashboard");
    }

    @org.springframework.web.bind.annotation.PostMapping("/definir-empresa")
    public String definirEmpresa(@RequestParam("cnpj") String cnpj,
                                 @RequestParam(value = "razaoSocial", required = false) String razaoSocial,
                                 HttpServletRequest request,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        try {
            EmpresaAuditada emp = auditorService.definirCnpjAuditado(cnpj, razaoSocial);
            if (emp != null && emp.isIdentificada()) {
                redirect.addFlashAttribute("mensagemSucesso", "Empresa auditada definida com sucesso: " + emp.getNomeCurto() + " (" + emp.getCnpjPrincipal() + ")");
            } else {
                redirect.addFlashAttribute("mensagemSucesso", "CNPJ " + cnpj + " definido como foco da auditoria.");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("mensagemErro", "Erro ao definir empresa: " + e.getMessage());
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isEmpty() ? referer : "/dashboard");
    }

    @GetMapping({"/exportar/relatorio-executivo-pdf", "/relatorio-pdf", "/exportar/pdf"})
    public ResponseEntity<byte[]> baixarRelatorioExecutivoPdf() {
        EmpresaAuditada empresa = auditorService.getEmpresaAuditada();
        KpiDashboard kpi = auditorService.getKpiDashboard();
        List<NotaFiscalResumo> notas = auditorService.getNotasEmMemoria();
        List<ItemAuditoria> divergentes = auditorService.getItensEmMemoria().stream()
                .filter(i -> !"CONFORME".equals(i.getStatus()))
                .toList();

        byte[] pdfBytes = pdfExportService.gerarRelatorioExecutivoPdf(empresa, kpi, notas, divergentes);

        String nomeArquivo = "Relatorio_Executivo_Reforma_Fiscal";
        if (empresa != null && empresa.getCnpjPrincipal() != null && !empresa.getCnpjPrincipal().isEmpty()) {
            nomeArquivo += "_" + empresa.getCnpjPrincipal().replaceAll("\\D", "");
        }
        nomeArquivo += ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nomeArquivo)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
