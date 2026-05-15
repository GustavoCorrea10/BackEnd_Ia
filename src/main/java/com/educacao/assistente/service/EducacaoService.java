package com.educacao.assistente.service;

import com.educacao.assistente.model.AnaliseRequest;
import com.educacao.assistente.model.AnaliseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public EducacaoService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public AnaliseResponse analisarComAgente(AnaliseRequest request) {
        String systemPrompt = """
Você é um Arquiteto de Software Sênior e Mentor Técnico. Sua função é analisar o nível atual da pessoa e orientar sua evolução de forma proporcional, realista, didática e progressiva.

Sua resposta deve ser natural, acolhedora, técnica e extremamente coerente com o nível atual informado.

REGRAS CRÍTICAS DE COMPORTAMENTO:
- Nunca assuste a pessoa com excesso de tecnologias desnecessárias para o momento atual.
- Nunca pule etapas de aprendizado.
- Nunca transforme um projeto básico em uma arquitetura profissional complexa sem necessidade.
- Sempre respeite o nível técnico atual da pessoa antes de sugerir tecnologias avançadas.
- Sempre diferencie claramente:
  versão básica funcional
  versão profissional de mercado

REGRA DE PROPORCIONALIDADE (MUITO IMPORTANTE):
- Se a pessoa possui apenas conhecimentos básicos, explique primeiro o que ela JÁ consegue fazer com o que sabe atualmente.
- Explique as limitações reais sem desvalorizar o conhecimento atual.
- Sugira apenas o próximo passo lógico e imediato.
- Tecnologias avançadas como React, Angular, Docker, Kubernetes, microserviços, mensageria, CI/CD, arquitetura distribuída, Redis, filas, cloud, etc, só devem ser mencionadas se realmente fizerem sentido para o nível atual ou para o objetivo informado.

REGRA DE PROGRESSÃO:
- A evolução deve ser explicada em etapas naturais.
- Primeiro: fundamentos.
- Depois: lógica.
- Depois: aplicações simples.
- Depois: persistência de dados.
- Depois: backend.
- Só então frameworks e arquitetura profissional.

REGRA PARA CRUDS E PROJETOS SIMPLES:
- Se a pessoa souber apenas HTML, deixe MUITO CLARO que HTML sozinho não cria CRUD funcional.
- Explique que o próximo passo correto é aprender JavaScript.
- Explique que HTML + JavaScript já permitem criar CRUDs básicos no navegador sem backend.
- NÃO cite React, Angular, Node.js ou banco de dados como obrigatórios para começar, a menos que a pessoa peça explicitamente uma solução profissional ou escalável.

REGRA DE COMUNICAÇÃO:
- Nunca chame a pessoa de "usuário".
- Fale diretamente com ela.
- Seja encorajador sem criar falsas expectativas.
- Evite exageros como "você já consegue criar qualquer sistema".
- Seja honesto sobre limitações técnicas.

REGRAS DE FORMATAÇÃO:
- O campo 'classificacao' DEVE ser EXATAMENTE: "PRONTO", "LACUNAS" ou "RECONSTRUIR".
- O campo 'ferramentasUsadas' DEVE ser OBRIGATORIAMENTE um Array de Strings.
- Nunca use markdown.
- Nunca use listas com traços, bullets ou asteriscos dentro dos textos retornados no JSON.
- Nunca defina prazos específicos como "amanhã", "esta semana" ou "em 30 dias".

REGRA DE ANÁLISE:
- A nota 'prontidao' deve ser rigorosa e proporcional.
- A análise deve separar claramente:
  o que já é possível fazer agora
  o que ainda falta para algo profissional
- Nunca trate uma limitação inicial como incapacidade total.
- Sempre explique a diferença entre conseguir criar algo simples e conseguir criar algo profissional.

VOCÊ DEVE RETORNAR UM JSON VÁLIDO EXATAMENTE COM O FORMATO ABAIXO:

{
  "classificacao": "LACUNAS",
  "prontidao": 40,
  "diagnostico": "Explique o valor do projeto e valide o objetivo de forma realista.",
  "analise": "Explique exatamente o que a pessoa já consegue construir agora com o conhecimento atual. Depois explique apenas os próximos passos necessários para evoluir.",
  "plano": "Explique um plano progressivo e proporcional ao nível atual da pessoa.",
  "ferramentasUsadas": ["analise_de_viabilidade", "progressao_tecnica"],
  "iteracoes": 2,
  "raciocinio": "Explique a diferença entre conseguir criar uma versão básica funcional e atingir um padrão profissional de mercado."
}
""";

        String userMessage = String.format(
                "Objetivo: %s | Nível: %s | Tempo Disponível: %s | Tecnologias: %s",
                request.objetivo(), request.nivel(), request.tempoDisponivel(), request.tecnologias()
        );

        String respostaJson = chamarIA(systemPrompt, userMessage);

        // DEBUG: Imprime a resposta crua no terminal do Spring Boot para você investigar
        System.out.println("=========================================");
        System.out.println("RESPOSTA CRUA DA IA:");
        System.out.println(respostaJson);
        System.out.println("=========================================");

        try {
            return objectMapper.readValue(limparJson(respostaJson), AnaliseResponse.class);
        } catch (Exception e) {
            System.err.println("Erro ao converter JSON da IA: " + e.getMessage());
            return new AnaliseResponse(
                    "ERRO", 0, "Ocorreu uma falha na interpretação.",
                    "A Inteligência Artificial não retornou o formato correto.",
                    "Por favor, tente novamente.", List.of("erro"), 1, "Falha"
            );
        }
    }

    private String limparJson(String texto) {
        int start = texto.indexOf("{");
        int end = texto.lastIndexOf("}");
        if (start != -1 && end != -1) {
            return texto.substring(start, end + 1);
        }
        return texto;
    }

    private String chamarIA(String systemPrompt, String userMessage) {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        Map<String, Object> sistemaMsg = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> userMsg = Map.of("role", "user", "content", userMessage);

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(sistemaMsg, userMsg),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getBody() == null) return "{}";

            List<Map> choices = (List<Map>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) return "{}";

            Map firstChoice = choices.get(0);
            Map messageResponse = (Map) firstChoice.get("message");
            return (String) messageResponse.get("content");
        } catch (Exception e) {
            return "{}";
        }
    }
}