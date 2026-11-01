package io.github.fatec.integration;

import io.github.fatec.entity.Personagem;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PersonagemIntegrationImpl implements PersonagemIntegration {

    private static final Logger log = LoggerFactory.getLogger(PersonagemIntegrationImpl.class);

    private final RestClient restClient;

    public PersonagemIntegrationImpl(@Value("${rick-and-morty.url}") String url) {
        // Timeout: sem ele, uma API travada prende a requisição para sempre e o retry nunca entra
        SimpleClientHttpRequestFactory timeout = new SimpleClientHttpRequestFactory();
        timeout.setConnectTimeout(Duration.ofSeconds(2));
        timeout.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(url).requestFactory(timeout).build();
    }

    @Override
    @Retry(name = "personagemApi", fallbackMethod = "fallback")
    public Optional<Personagem> buscarPorNome(String nome) {
        log.info("[TENTATIVA] Buscando '{}' na API", nome);
        try {
            // Busca por nome é um FILTRO: GET /character/?name=rick
            // A API devolve uma lista: { "info": {...}, "results": [ {personagem}, {personagem}, ... ] }
            Map<?, ?> resposta = restClient.get()
                    .uri("/character/?name={nome}", nome)
                    .retrieve()
                    .body(Map.class);

            // Pegamos o primeiro da lista (a API ordena por id, então "rick" traz o Rick Sanchez)
            List<?> resultados = (List<?>) resposta.get("results");
            Map<?, ?> primeiro = (Map<?, ?>) resultados.get(0);

            return Optional.of(new Personagem(
                    ((Number) primeiro.get("id")).longValue(),
                    (String) primeiro.get("name"),
                    (String) primeiro.get("status")));
        } catch (HttpClientErrorException.NotFound e) {
            // Nenhum personagem com esse nome: a API responde 404. Não é falha, então não tem retry.
            return Optional.empty();
        }
    }

    // FALLBACK: mesmos parâmetros do método original + o erro no final.
    // Em vez de devolver erro 500 para quem chamou, devolve uma resposta padrão.
    private Optional<Personagem> fallback(String nome, Throwable erro) {
        log.warn("[FALLBACK] API indisponível ao buscar '{}': {}", nome, erro.getMessage());
        return Optional.of(new Personagem(null, nome, "Indisponível no momento"));
    }
}
