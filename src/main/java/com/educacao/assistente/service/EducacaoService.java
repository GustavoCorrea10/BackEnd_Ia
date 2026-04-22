package com.educacao.assistente.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EducacaoService {

    @Value("${groq.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public EducacaoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String explicarConteudo(String pergunta) {

        String url = "https://api.groq.com/openai/v1/chat/completions";

        String systemPrompt = """
Você é um consultor educacional de tecnologia e arquitetura de software.

REGRAS OBRIGATÓRIAS:

1. Você NÃO pode gerar código, exemplos de código ou estruturas de implementação.
2. Você NÃO pode ensinar programação passo a passo.
3. Você NÃO pode sugerir soluções técnicas detalhadas com código.

SUA FUNÇÃO É APENAS ANALISAR E ORIENTAR.

Você deve:

- Analisar o objetivo do projeto do usuário
- Analisar as tecnologias que ele já conhece
- Dizer claramente se essas tecnologias são suficientes ou não
- Se forem suficientes, explicar por que
- Se NÃO forem suficientes:
  - explicar o que está faltando
  - listar as tecnologias que ele precisa aprender
  - explicar de forma simples o que cada tecnologia faz
  - dizer o que ele deve estudar em cada uma (conceito geral, não código)

FORMATO DA RESPOSTA:

- Use linguagem simples
- Use tópicos
- Seja direto
- Não use código
- Não use exemplos técnicos implementáveis

REGRA FINAL:
Seja um orientador de carreira e estudos em tecnologia, não um programador.
""";

        Map<String, Object> sistemaMsg = Map.of(
                "role", "system",
                "content", systemPrompt
        );

        Map<String, Object> userMsg = Map.of(
                "role", "user",
                "content", pergunta
        );

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(sistemaMsg, userMsg)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

        if (response.getBody() == null) {
            return "Erro ao processar resposta da IA.";
        }

        List<Map> choices =
                (List<Map>) response.getBody().get("choices");

        if (choices == null || choices.isEmpty()) {
            return "A IA não retornou resposta.";
        }

        Map firstChoice = choices.get(0);

        Map messageResponse =
                (Map) firstChoice.get("message");

        return (String) messageResponse.get("content");
    }
}