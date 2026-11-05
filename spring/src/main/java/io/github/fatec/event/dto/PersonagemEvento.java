package io.github.fatec.event.dto;

// A mensagem que vai para o RabbitMQ: só os 2 atributos (nome e status).
// Vira JSON na fila: {"nome":"Rick Sanchez","status":"Alive"}
public record PersonagemEvento(String nome, String status) {
}
