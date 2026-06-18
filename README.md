# AgendaFácil Pro

Sistema web profissional para agendamento de salões, barbearias, estética e serviços locais, feito com Spring Boot, Thymeleaf, PostgreSQL, Flyway e Spring Security.

## Principais recursos

- Página pública por estabelecimento: `/b/{slug}`.
- Login seguro para dono, gerente e profissional.
- Cadastro de serviços, profissionais, horários e bloqueios.
- Agendamento público sem conta para o cliente final.
- Identificação prática do cliente por telefone normalizado.
- Controle de faltas, cliente em observação e bloqueio.
- Reserva temporária com expiração para reduzir horários fantasmas.
- Limite por telefone e por IP.
- Honeypot anti-bot.
- Aprovação manual de clientes novos ou suspeitos.
- Token seguro para cancelamento/remarcação futura.
- Auditoria de ações críticas.
- Flyway para versionamento do banco.

## Requisitos

- Java 17+
- Maven 3.9+
- Docker Desktop ou PostgreSQL local

## Como rodar localmente

1. Suba o PostgreSQL:

```bash
docker compose up -d
```

2. Rode a aplicação:

```bash
mvn spring-boot:run
```

3. Acesse:

```text
http://localhost:8080
```

## Usuário inicial de demonstração

A aplicação cria dados de exemplo automaticamente no primeiro start.

```text
E-mail: dono@agendafacil.local
Senha: Admin@123
```

Página pública da barbearia modelo:

```text
http://localhost:8080/b/barbearia-modelo
```

## Segurança em produção

Antes de hospedar para clientes reais:

- Trocar `APP_HASH_SECRET`.
- Usar senha forte no banco.
- Ativar `SESSION_COOKIE_SECURE=true` com HTTPS.
- Não versionar `.env`.
- Configurar backup diário do PostgreSQL.
- Criar ambiente de homologação antes da produção.
- Revisar logs sem expor senha, token ou dados sensíveis.
- Usar Railway, Render, Neon, AWS, Azure ou outra plataforma com HTTPS e controles de acesso.

## Observação importante

Nenhum sistema é 100% inviolável. Este projeto foi estruturado com boas práticas para reduzir fraudes, erros operacionais e riscos de segurança, mas precisa de testes reais, revisão contínua, backup e monitoramento quando for usado por clientes pagantes.
