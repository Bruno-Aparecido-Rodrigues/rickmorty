package io.github.fatec.controller;

import io.github.fatec.entity.Personagem;
import io.github.fatec.integration.PersonagemIntegration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/personagem")
public class PersonagemController {

    // Depende da INTERFACE, não da implementação. O Spring injeta o PersonagemIntegrationImpl.
    private final PersonagemIntegration personagemIntegration;

    public PersonagemController(PersonagemIntegration personagemIntegration) {
        this.personagemIntegration = personagemIntegration;
    }

    // GET /personagem/rick
    @GetMapping("/{nome}")
    public ResponseEntity<?> buscarPorNome(@PathVariable("nome") String nome) {
        return personagemIntegration.buscarPorNome(nome)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("mensagem", "Personagem '" + nome + "' não encontrado")));
    }
}
