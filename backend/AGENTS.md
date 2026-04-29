# Backend Agent Instructions

Use this file for all work under `backend/`. Also read the root `AGENTS.md`.

## Required Reading

Before editing backend code, consult:

- `backend/CONVENTIONS.md` for package structure, layer rules, DTO conventions, and error handling.
- `backend/CODING_GUIDE.md` for Entity, Service, Repository, and clean-code patterns.
- `backend/TEST_STRATEGY.md` for test scope, mocking policy, and priorities.
- `backend/CLAUDE.md` for additional historical project context if needed.

## Stack

- Java 21
- Spring Boot 3.x
- Gradle Kotlin DSL
- Spring Data JPA
- MySQL 8.0 in production
- H2 for development/testing paths where configured
- AI access through `ResilientAiClient`

Backend AI uses OpenAI GPT-4o-mini as the primary path with Claude fallback. Model identifiers are configuration concerns and must not be hardcoded in Java code.

## Architecture Rules

- Do not return entities directly from controllers or APIs. Convert to response DTOs.
- Keep controller, service, repository, entity, and DTO responsibilities separated according to `backend/CONVENTIONS.md`.
- Use `@Transactional(readOnly = true)` by default for read services.
- Use `@Transactional` only for write methods or flows that require it.
- Database schema changes must go through Flyway migrations. Do not rely on manual DDL.
- AI calls must go through `ResilientAiClient`; do not call OpenAI or Claude SDKs directly from domain/application services.
- Lambda is decoupled through S3/EventBridge. Backend should not directly invoke Lambda.
- Comments should be rare and explain non-obvious why, not obvious what.

## Spec Requirement

- Before changing `backend/src/`, check `.omc/plans/` or `docs/plans/` for a relevant spec.
- If the change is a new feature or meaningful behavior change, create or update the spec first.
- Include the why, goal, evidence, and trade-offs for non-trivial decisions.

## Testing

Primary commands:

```bash
./gradlew test
./gradlew test --tests "InterviewServiceTest"
./gradlew test --tests "com.rehearse.api.domain.interview.*"
```

Follow `backend/TEST_STRATEGY.md` over generic testing guidance when there is a conflict.

Project-established backend testing patterns:

- Mockito unit tests are common: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`.
- Repository and finder collaborators may be mocked where existing tests use that style.
- Controller slices use `@WebMvcTest`.
- Repository slices use `@DataJpaTest` when custom query behavior needs verification.
- Testcontainers are not currently part of the project default.
- Do not mock Entity, VO, or DTO objects. Prefer real objects and `TestFixtures`.
- Avoid brittle tests that assert implementation details instead of observable behavior.
- Do not snapshot non-deterministic LLM or timestamp output.

## Backend Conventions to Re-check Often

- Package placement and layer dependencies: `backend/CONVENTIONS.md`.
- DTO naming and mapping rules: `backend/CONVENTIONS.md`.
- Service and transaction patterns: `backend/CODING_GUIDE.md`.
- Repository query patterns: `backend/CODING_GUIDE.md`.
- Test prioritization and mock policy: `backend/TEST_STRATEGY.md`.
