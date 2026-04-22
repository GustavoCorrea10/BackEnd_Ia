package com.educacao.assistente.controller;

import com.educacao.assistente.service.EducacaoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/educacao")
@CrossOrigin
public class EducacaoController {

    private final EducacaoService service;

    public EducacaoController(EducacaoService service) {
        this.service = service;
    }

    @PostMapping("/explicar")
    public String explicar(@RequestBody String pergunta) {
        return service.explicarConteudo(pergunta);
    }

    @GetMapping("/teste")
    public String teste() {
        return "API funcionando!";
    }

    @GetMapping("/explicar")
    public String explicarGet() {
        return service.explicarConteudo("Teste da IA");
    }
}