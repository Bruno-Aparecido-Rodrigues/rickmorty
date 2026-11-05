package io.github.fatec.service;

import io.github.fatec.entity.Personagem;
import io.github.fatec.event.PersonagemPublicador;
import io.github.fatec.integration.PersonagemIntegration;

import java.util.Optional;

// Caso de uso: buscar o personagem e avisar (via mensagem) que ele foi consultado.
// Junta as duas interfaces sem conhecer nenhuma implementação. Quem cria esta classe é o ServiceConfig.
public class PersonagemService {

    private final PersonagemIntegration personagemIntegration;
    private final PersonagemPublicador personagemPublicador;

    public PersonagemService(PersonagemIntegration personagemIntegration, PersonagemPublicador personagemPublicador) {
        this.personagemIntegration = personagemIntegration;
        this.personagemPublicador = personagemPublicador;
    }

    public Optional<Personagem> buscarPorNome(String nome) {
        Optional<Personagem> personagem = personagemIntegration.buscarPorNome(nome);
        personagem.ifPresent(personagemPublicador::publicar); // só envia se encontrou algo
        return personagem;
    }
}
