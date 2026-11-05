package io.github.fatec.listener;

import io.github.fatec.event.dto.PersonagemEvento;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Processa a mensagem recebida, com RETRY (regras em resilience4j.retry.instances.personagemListener no yaml).
@Component
public class PersonagemProcessador {

    private static final Logger log = LoggerFactory.getLogger(PersonagemProcessador.class);

    // Liga/desliga uma falha de mentira, só para demonstrar o retry (rabbit.simular-falha no yaml)
    private final boolean simularFalha;

    public PersonagemProcessador(@Value("${rabbit.simular-falha:false}") boolean simularFalha) {
        this.simularFalha = simularFalha;
    }

    @Retry(name = "personagemListener", fallbackMethod = "fallback")
    public void processar(PersonagemEvento evento) {
        log.info("[LISTEN-TENTATIVA] Processando '{}'", evento.nome());
        if (simularFalha) {
            throw new IllegalStateException("Falha simulada ao processar a mensagem");
        }
        log.info("[LISTEN] Processada com sucesso: nome={}, status={}", evento.nome(), evento.status());
    }

    // Chamado quando as tentativas acabam. Registra o problema e a mensagem sai da fila,
    // em vez de voltar para a fila e ficar falhando para sempre.
    private void fallback(PersonagemEvento evento, Throwable erro) {
        log.error("[LISTEN-FALLBACK] Desistiu de processar '{}' depois das tentativas: {}", evento.nome(), erro.getMessage());
    }
}
