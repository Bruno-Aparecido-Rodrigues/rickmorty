package io.github.fatec.listener;

import io.github.fatec.event.dto.PersonagemEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

// LISTEN: fica "escutando" a fila. Cada mensagem que chega chama o método receber.
// Ele só recebe e passa para o PersonagemProcessador, que é onde está o retry.
// (O retry fica em outra classe porque o @Retry só funciona quando o método é chamado de fora da classe.)
@Component
public class PersonagemEscutador {

    private static final Logger log = LoggerFactory.getLogger(PersonagemEscutador.class);

    private final PersonagemProcessador personagemProcessador;

    public PersonagemEscutador(PersonagemProcessador personagemProcessador) {
        this.personagemProcessador = personagemProcessador;
    }

    @RabbitListener(queues = "${rabbit.fila}")
    public void receber(PersonagemEvento evento) {
        log.info("[LISTEN] Mensagem recebida: {}", evento);
        personagemProcessador.processar(evento);
    }
}
