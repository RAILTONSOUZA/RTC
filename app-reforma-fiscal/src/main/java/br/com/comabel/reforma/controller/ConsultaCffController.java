package br.com.comabel.reforma.controller;

import br.com.comabel.reforma.model.RegraClassificacao;
import br.com.comabel.reforma.service.RegrasTributariasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ConsultaCffController {

    @Autowired
    private RegrasTributariasService regrasService;

    @GetMapping("/consulta-cff")
    public String consultaCff(Model model, @RequestParam(value = "q", required = false) String query) {
        List<RegraClassificacao> regras = regrasService.buscar(query);

        model.addAttribute("regras", regras);
        model.addAttribute("totalRegras", regras.size());
        model.addAttribute("query", query);
        model.addAttribute("activeMenu", "cff");
        return "consulta-cff";
    }
}
