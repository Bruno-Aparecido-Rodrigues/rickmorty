package io.github.fatec.integration;

import io.github.fatec.entity.Personagem;

import java.util.Optional;

public interface PersonagemIntegration {

    Optional<Personagem> buscarPorNome(String nome);
}
