package com.educacao.assistente.model;

public record AnaliseRequest(
        String objetivo,
        String nivel,
        String tempoDisponivel,
        String tecnologias
) {}