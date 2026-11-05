package io.github.fatec.controller;

import io.github.fatec.entity.Personagem;
import io.github.fatec.service.PersonagemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/personagem")
public class PersonagemController {

    private final PersonagemService personagemService;

    public PersonagemController(PersonagemService personagemService) {
        this.personagemService = personagemService;
    }

    // GET /personagem/rick -> busca na API, envia a mensagem para o RabbitMQ e devolve o personagem
    @GetMapping("/{nome}")
    public ResponseEntity<?> buscarPorNome(@PathVariable("nome") String nome) {
        Optional<Personagem> personagem = personagemService.buscarPorNome(nome);

        if (personagem.isPresent()) {
            return ResponseEntity.ok(personagem.get());
        }

        // Nome que não existe na API: 404 com uma mensagem (em vez de uma página em branco)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("mensagem", "Personagem '" + nome + "' não encontrado"));
    }
}
