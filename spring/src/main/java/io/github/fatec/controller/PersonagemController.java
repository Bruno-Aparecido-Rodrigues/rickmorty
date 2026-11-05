package io.github.fatec.controller;

import io.github.fatec.entity.Personagem;
import io.github.fatec.service.PersonagemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/personagem")
public class PersonagemController {

    private final PersonagemService personagemService;

    public PersonagemController(PersonagemService personagemService) {
        this.personagemService = personagemService;
    }

    // GET /personagem/rick -> busca na API, envia a mensagem para o RabbitMQ e devolve o personagem
    @GetMapping("/{nome}")
    public ResponseEntity<Personagem> buscarPorNome(@PathVariable("nome") String nome) {
        return personagemService.buscarPorNome(nome)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
