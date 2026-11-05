package io.github.fatec.event;

import io.github.fatec.entity.Personagem;

// Interface (porta) do SEND: "avise que um personagem foi consultado".
// O domínio não sabe que existe RabbitMQ. Quem envia de verdade é o PersonagemPublicadorImpl, no módulo spring.
public interface PersonagemPublicador {

    void publicar(Personagem personagem);
}
