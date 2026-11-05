package io.github.fatec.event;

import io.github.fatec.entity.Personagem;
import io.github.fatec.event.dto.PersonagemEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// SEND: monta a mensagem com os 2 atributos e envia para a fila.
@Component
public class PersonagemPublicadorImpl implements PersonagemPublicador {

    private static final Logger log = LoggerFactory.getLogger(PersonagemPublicadorImpl.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public PersonagemPublicadorImpl(RabbitTemplate rabbitTemplate,
                                    @Value("${rabbit.exchange}") String exchange,
                                    @Value("${rabbit.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publicar(Personagem personagem) {
        PersonagemEvento evento = new PersonagemEvento(personagem.nome(), personagem.status());
        // Envia para a exchange com a routing key. O binding faz a exchange entregar na fila personagem.consultado
        rabbitTemplate.convertAndSend(exchange, routingKey, evento);
        log.info("[SEND] Mensagem enviada para a exchange '{}' (routing key '{}'): {}", exchange, routingKey, evento);
    }
}
