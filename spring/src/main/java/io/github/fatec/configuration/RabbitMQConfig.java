package io.github.fatec.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Declara exchange, fila e binding. Quando a aplicação conecta, o Spring cria os três no RabbitMQ
// se ainda não existirem. Se já existirem com as MESMAS características, só reaproveita.
@Configuration
public class RabbitMQConfig {

    // Exchange do tipo DIRECT: entrega a mensagem na fila cuja routing key bate exatamente.
    // durable = true (sobrevive a reinício), autoDelete = false (não some quando ninguém está usando)
    @Bean
    public DirectExchange personagemExchange(@Value("${rabbit.exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    // Fila durável: as mensagens não se perdem se o RabbitMQ reiniciar
    @Bean
    public Queue personagemFila(@Value("${rabbit.fila}") String fila) {
        return new Queue(fila, true);
    }

    // Binding: liga a exchange à fila. "Mensagem com esta routing key vai para esta fila."
    @Bean
    public Binding personagemBinding(Queue personagemFila, DirectExchange personagemExchange,
                                     @Value("${rabbit.routing-key}") String routingKey) {
        return BindingBuilder.bind(personagemFila).to(personagemExchange).with(routingKey);
    }

    // Converte PersonagemEvento <-> JSON no send e no listen.
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
