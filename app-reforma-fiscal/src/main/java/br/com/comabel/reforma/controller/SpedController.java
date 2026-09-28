package br.com.comabel.reforma.controller;

import br.com.comabel.reforma.model.SpedDiagnostico;
import br.com.comabel.reforma.service.SpedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;

@Controller
public class SpedController {

    @Autowired
    private SpedService spedService;

    @Value("${app.default-scan-dir:c:/agente/agente}")
    private String defaultScanDir;

    @GetMapping("/sped")
    public String viewSped(Model model) {
        SpedDiagnostico diag = spedService.getUltimoDiagnostico();
        if (diag == null && !spedService.isLimpoPeloUsuario()) {
            // Tenta carregar o arquivo SPED padrão da pasta caso exista e não tenha sido limpo
            File dir = new File(defaultScanDir);
            if (dir.exists()) {
                File[] txts = dir.listFiles((d, name) -> name.toLowerCase().startsWith("spedefd") && name.toLowerCase().endsWith(".txt"));
                if (txts != null && txts.length > 0) {
                    try {
                        diag = spedService.analisarArquivo(txts[0]);
                    } catch (Exception e) {
                        System.err.println("Erro ao carregar SPED padrão: " + e.getMessage());
                    }
                }
            }
        }

        model.addAttribute("diag", diag);
        model.addAttribute("activeMenu", "sped");
        return "sped-analise";
    }

    @PostMapping("/sped/limpar")
    public String limparSped(RedirectAttributes redirect) {
        spedService.limpar();
        redirect.addFlashAttribute("mensagemSucesso", "Diagnóstico do SPED Fiscal limpo com sucesso!");
        return "redirect:/sped";
    }

    @PostMapping("/sped/analisar-padrao")
    public String analisarPadrao(RedirectAttributes redirect) {
        try {
            File dir = new File(defaultScanDir);
            File[] txts = dir.listFiles((d, name) -> name.toLowerCase().startsWith("spedefd") && name.toLowerCase().endsWith(".txt"));
            if (txts == null || txts.length == 0) {
                redirect.addFlashAttribute("mensagemErro", "Nenhum arquivo SPED encontrado em: " + defaultScanDir);
                return "redirect:/sped";
            }
            spedService.analisarArquivo(txts[0]);
            redirect.addFlashAttribute("mensagemSucesso", "SPED Fiscal (" + txts[0].getName() + ") diagnosticado com sucesso!");
        } catch (Exception e) {
            redirect.addFlashAttribute("mensagemErro", "Erro ao processar SPED: " + e.getMessage());
        }
        return "redirect:/sped";
    }

    @PostMapping("/sped/upload")
    public String uploadSped(@RequestParam("arquivoSped") MultipartFile file, RedirectAttributes redirect) {
        if (file.isEmpty()) {
            redirect.addFlashAttribute("mensagemErro", "Selecione um arquivo SPED Fiscal válido.");
            return "redirect:/sped";
        }
        try {
            spedService.analisarStream(file.getInputStream());
            redirect.addFlashAttribute("mensagemSucesso", "Arquivo SPED (" + file.getOriginalFilename() + ") processado com sucesso!");
        } catch (Exception e) {
            redirect.addFlashAttribute("mensagemErro", "Falha ao analisar arquivo SPED: " + e.getMessage());
        }
        return "redirect:/sped";
    }
}
