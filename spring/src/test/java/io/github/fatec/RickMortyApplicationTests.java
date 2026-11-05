package io.github.fatec;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// auto-startup=false: o teste sobe a aplicação sem tentar conectar no RabbitMQ
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class RickMortyApplicationTests {

    @Test
    void contextLoads() {
    }
}
