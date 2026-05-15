package com.educacao.assistente.controller;

import com.educacao.assistente.model.AnaliseRequest;
import com.educacao.assistente.model.AnaliseResponse;
import com.educacao.assistente.service.EducacaoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/educacao")
public class EducacaoController {

    private final EducacaoService service;

    public EducacaoController(EducacaoService service) {
        this.service = service;
    }

    // NOVA ROTA: Escuta o POST enviando o JSON e devolve a resposta estruturada
    @PostMapping("/analisar")
    public AnaliseResponse analisar(@RequestBody AnaliseRequest request) {
        return service.analisarComAgente(request);
    }

    @GetMapping("/teste")
    public String teste() {
        return "API funcionando!";
    }
}