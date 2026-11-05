package io.github.fatec.configuration;

import io.github.fatec.event.PersonagemPublicador;
import io.github.fatec.integration.PersonagemIntegration;
import io.github.fatec.service.PersonagemService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// O PersonagemService é Java puro (domain, sem @Service). Aqui o Spring aprende a criá-lo,
// entregando as implementações (IntegrationImpl e PublicadorImpl) no lugar das interfaces.
@Configuration
public class ServiceConfig {

    @Bean
    public PersonagemService personagemService(PersonagemIntegration personagemIntegration,
                                               PersonagemPublicador personagemPublicador) {
        return new PersonagemService(personagemIntegration, personagemPublicador);
    }
}
