package com.educacao.assistente.model;

import java.util.List;

public record AnaliseResponse(
        String classificacao,
        int prontidao,
        String diagnostico,
        String analise,
        String plano,
        List<String> ferramentasUsadas,
        int iteracoes,
        String raciocinio
) {}