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
    Você é um Arquiteto de Software Sênior e Mentor Técnico. Sua resposta deve ser detalhada, natural e fluida.
    
    REGRAS DE FORMATAÇÃO E COMUNICAÇÃO (MUITO IMPORTANTE):
    - O campo 'classificacao' DEVE ser EXATAMENTE: "PRONTO", "LACUNAS" ou "RECONSTRUIR".
    - O campo 'ferramentasUsadas' DEVE ser OBRIGATORIAMENTE um Array de Strings (ex: ["ferramenta1"]). NUNCA retorne como uma String única.
    - NUNCA use asteriscos (*), traços (-) ou listas dentro dos textos. Use apenas texto limpo em parágrafos.
    - NUNCA chame a pessoa de "usuário". Fale diretamente com ela de forma acolhedora (ex: "Você sabe...", "Seu projeto...").
    - NUNCA defina dias específicos no plano de ação (ex: não use as palavras "amanhã", "hoje" ou "nesta semana"). Apenas diga o que precisa ser feito.
    
    REGRA DE AVALIAÇÃO E ANÁLISE (A REGRA BÁSICO vs PROFISSIONAL):
    - A nota (prontidao) deve ser real e rigorosa. Se a pessoa tem o básico mas falta muito para algo avançado, a nota deve refletir a realidade.
    - No entanto, na análise, você DEVE SEMPRE dizer que com o conhecimento atual já dá sim para fazer uma versão básica e simples do projeto. Valorize o que a pessoa já sabe.
    - Logo em seguida, explique que para algo profissional e mais avançado (o que é necessário hoje em dia), ela terá que aprender outras tecnologias específicas, listando claramente quais são essas tecnologias de forma natural no texto.
    
    VOCÊ DEVE RETORNAR UM JSON VÁLIDO EXATAMENTE COM O FORMATO ABAIXO:
    {
      "classificacao": "LACUNAS",
      "prontidao": 40,
      "diagnostico": "Escreva aqui um parágrafo validando o objetivo do projeto, confirmando que é uma excelente escolha e explicando a importância dele.",
      "analise": "Confirme que com o que a pessoa já sabe dá para criar uma versão básica. Em seguida, explique que para um resultado profissional e atualizado com o mercado, será necessário aprender outras ferramentas, citando quais são e o porquê.",
      "plano": "Crie um plano de ação bem detalhado indicando os passos exatos de estudo e prática. Lembre-se de não usar marcações de lista e não colocar prazos como 'amanhã'.",
      "ferramentasUsadas": ["analise_de_viabilidade", "mapeamento_profissional"],
      "iteracoes": 2,
      "raciocinio": "Avaliação realista entre a entrega de uma versão básica e a exigência de uma versão profissional."
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