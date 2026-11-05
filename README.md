# Rick and Morty API: retry e fallback com Resilience4j

API escolhida: [Rick and Morty API](https://rickandmortyapi.com), que é pública e não precisa de chave.
Entidade: **Personagem**, com os campos `id`, `nome` e `status`.

```
GET http://localhost:8080/personagem/rick
→ {"id":1,"nome":"Rick Sanchez","status":"Alive"}
```

## Como rodar

```bash
./mvnw clean install
./mvnw -pl spring spring-boot:run
```

## Busca por id × busca por nome na API

| | Por id | Por nome |
|---|---|---|
| URL | `/api/character/1` | `/api/character/?name=rick` |
| Tipo | caminho da URL | **filtro** (query param) |
| Resposta | **1 objeto** `{ "id": 1, "name": "Rick Sanchez", ... }` | **uma lista** `{ "info": {...}, "results": [ {...}, {...} ] }` |
| Correspondência | exata | parcial e sem diferenciar maiúscula: `rick` traz Rick Sanchez, Adjudicator Rick, Alien Rick... |
| Não encontrou | 404 `{"error":"Character not found"}` | 404 `{"error":"There is nothing here"}` |

Na PokeAPI do professor, `/pokemon/pikachu` devolve um objeto só. Na Rick and Morty, a busca por nome devolve uma **lista**, por isso o código pega o **primeiro** item de `results`. A API ordena os resultados por id, então `rick` traz o Rick Sanchez (id 1). Para um resultado exato, basta mandar o nome completo: `/personagem/morty smith`.

## Arquivos

```
domain/   (Java puro, sem Spring)
  entity/Personagem.java                    ← id, nome, status
  integration/PersonagemIntegration.java    ← interface: buscarPorNome(nome)
  event/PersonagemPublicador.java           ← interface: publicar(personagem)  (SEND)
  service/PersonagemService.java            ← busca o personagem e publica a mensagem
spring/
  integration/PersonagemIntegrationImpl.java  ← chama a API + @Retry + fallback
  event/PersonagemPublicadorImpl.java         ← SEND: envia {nome, status} para a fila
  event/dto/PersonagemEvento.java             ← a mensagem (2 atributos)
  listener/PersonagemEscutador.java           ← LISTEN: @RabbitListener recebe a mensagem
  listener/PersonagemProcessador.java         ← processa a mensagem com @Retry + fallback
  configuration/RabbitMQConfig.java           ← cria a fila e o conversor JSON
  configuration/ServiceConfig.java            ← cria o PersonagemService
  controller/PersonagemController.java        ← GET /personagem/{nome}
  resources/application.yaml                  ← RabbitMQ + configuração dos dois retries
```

## O fallback

```java
@Retry(name = "personagemApi", fallbackMethod = "fallback")
public Optional<Personagem> buscarPorNome(String nome) {
    ... chama GET /character/?name={nome} ...
}

private Optional<Personagem> fallback(String nome, Throwable erro) {
    return Optional.of(new Personagem(null, nome, "Indisponível no momento"));
}
```

O que acontece quando a API cai:

```
GET /personagem/rick
  tentativa 1 → falhou (timeout / sem conexão / erro 5xx) → espera 500ms
  tentativa 2 → falhou → espera 500ms
  tentativa 3 → falhou → o Resilience4j chama fallback("rick", erro)
  resposta: {"id":null,"nome":"rick","status":"Indisponível no momento"}   (200, e não 500)
```

Regras do fallback:

1. **Mesmos parâmetros do método original + `Throwable` no final.** O original recebe `(String nome)`, então o fallback recebe `(String nome, Throwable erro)`.
2. **O mesmo tipo de retorno** (`Optional<Personagem>`).
3. **O nome do método** tem que ser igual ao que está escrito em `fallbackMethod = "fallback"`.
4. **404 não é falha.** Nome inexistente vira `Optional.empty()` dentro do próprio método, e o controller responde 404. Por isso não entra no retry nem no fallback.

Configuração no `application.yaml`:

```yaml
resilience4j:
  retry:
    instances:
      personagemApi:           # o mesmo nome do @Retry(name = "personagemApi")
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - org.springframework.web.client.ResourceAccessException    # timeout / sem conexão
          - org.springframework.web.client.HttpServerErrorException   # erro 5xx
```

**Para demonstrar:** chame `/personagem/rick` com a internet ligada (vem o Rick). Depois desligue a internet e chame de novo: a resposta demora uns segundos (as 3 tentativas), chega com `"Indisponível no momento"`, e o console mostra `[FALLBACK]`.

## RabbitMQ: send e listen (com retry no listen)

```
GET /personagem/rick
  → busca na API → SEND: PersonagemPublicadorImpl envia {"nome":"Rick Sanchez","status":"Alive"} para a fila "personagem.consultado"
                                   ↓
                    LISTEN: PersonagemEscutador recebe → PersonagemProcessador processa (@Retry "personagemListener")
```

- **Send:** o `PersonagemService` chama `publicador.publicar(personagem)`, e o `PersonagemPublicadorImpl` usa o `RabbitTemplate.convertAndSend(fila, evento)`. Só envia se encontrou o personagem.
- **Listen:** o `@RabbitListener(queues = "${rabbit.fila}")` recebe cada mensagem e chama o `PersonagemProcessador`.
- **Retry no listen:** se o processamento lançar exceção, o Resilience4j tenta de novo (3 vezes, com 1s de espera). Se as 3 falharem, o `fallback` registra o erro e a mensagem sai da fila. Sem isso, o RabbitMQ devolveria a mensagem para a fila e ela ficaria falhando para sempre.
- O retry fica numa classe separada do `@RabbitListener` porque o `@Retry` só funciona quando o método é chamado por **outra** classe (o intermediário do Spring fica "do lado de fora").

### Subir o RabbitMQ (Docker)

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:4-management
```
Painel: http://localhost:15672 (usuário `guest`, senha `guest`). Para usar o CloudAMQP, troque o bloco `spring.rabbitmq` do yaml por `addresses: amqps://USUARIO:SENHA@HOST/VHOST`.

### Demonstrar o retry do listen

1. No `application.yaml`, coloque `rabbit.simular-falha: true` e reinicie.
2. Chame `GET /personagem/rick`. No console:
   ```
   [SEND] Mensagem enviada para a fila 'personagem.consultado': PersonagemEvento[nome=Rick Sanchez, status=Alive]
   [LISTEN] Mensagem recebida: PersonagemEvento[nome=Rick Sanchez, status=Alive]
   [LISTEN-TENTATIVA] Processando 'Rick Sanchez'
   [LISTEN-TENTATIVA] Processando 'Rick Sanchez'      ← 1s depois
   [LISTEN-TENTATIVA] Processando 'Rick Sanchez'      ← 1s depois
   [LISTEN-FALLBACK] Desistiu de processar 'Rick Sanchez' depois das tentativas: Falha simulada...
   ```
3. Com `simular-falha: false`, aparece uma tentativa só e `[LISTEN] Processada com sucesso`.

```yaml
resilience4j:
  retry:
    instances:
      personagemListener:
        max-attempts: 3
        wait-duration: 1s
```

## Arquitetura Limpa e SOLID

- **Arquitetura Limpa:** o `domain` (entidade + interface) não conhece Spring, HTTP nem Resilience4j. O `spring` depende do `domain`, nunca o contrário. Se a API mudar, ou for trocada por um banco, muda só o `PersonagemIntegrationImpl`.
- **S (uma responsabilidade por classe):** a entidade guarda os dados, a `IntegrationImpl` busca na API e o controller atende o HTTP.
- **O (aberto para extensão, fechado para modificação):** dá para trocar a fonte de dados com uma nova implementação da interface, sem mexer no controller. Para mudar o retry, basta editar o yaml.
- **L (substituição):** qualquer implementação da `PersonagemIntegration` funciona no controller, desde que siga o contrato: `Optional.empty()` quando o personagem não existe.
- **I (interfaces pequenas):** a interface tem um único método, só o que o controller usa.
- **D (dependa de abstrações):** o `PersonagemService` depende das **interfaces** `PersonagemIntegration` e `PersonagemPublicador`, e não das implementações. Ele nem sabe que existe RabbitMQ. Quem entrega as implementações é o `ServiceConfig`.
