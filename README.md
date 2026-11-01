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
spring/
  integration/PersonagemIntegrationImpl.java  ← chama a API + @Retry + fallback
  controller/PersonagemController.java        ← GET /personagem/{nome}
  resources/application.yaml                  ← configuração do retry
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

## Arquitetura Limpa e SOLID

- **Arquitetura Limpa:** o `domain` (entidade + interface) não conhece Spring, HTTP nem Resilience4j. O `spring` depende do `domain`, nunca o contrário. Se a API mudar, ou for trocada por um banco, muda só o `PersonagemIntegrationImpl`.
- **S (uma responsabilidade por classe):** a entidade guarda os dados, a `IntegrationImpl` busca na API e o controller atende o HTTP.
- **O (aberto para extensão, fechado para modificação):** dá para trocar a fonte de dados com uma nova implementação da interface, sem mexer no controller. Para mudar o retry, basta editar o yaml.
- **L (substituição):** qualquer implementação da `PersonagemIntegration` funciona no controller, desde que siga o contrato: `Optional.empty()` quando o personagem não existe.
- **I (interfaces pequenas):** a interface tem um único método, só o que o controller usa.
- **D (dependa de abstrações):** o controller depende da **interface** `PersonagemIntegration`, e não da implementação. Quem entrega a implementação é o Spring.
