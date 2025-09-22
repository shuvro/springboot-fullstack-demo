# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/respiroc/gregfullstack` hosts the Spring Boot application (controllers, repositories, scheduling entrypoints).
- `src/main/resources/templates` contains Thymeleaf pages; shared UI pieces live in `templates/fragments`.
- `src/main/resources/db/migration` stores Flyway migrations as `V{step}__description.sql` and runs on startup.
- `src/main/resources/static` holds client assets like `htmx.min.js` served directly by Spring.
- `src/test/java` mirrors the production package tree for JUnit 5 tests.
- Docker assets (`docker-compose.yml`, `docker/postgres/init.sql`) define the local Postgres and app containers.

## Build, Test, and Development Commands
- `./gradlew clean build`: compile, run unit tests, and package the app.
- `./gradlew bootRun`: start the API against a locally running Postgres (see `application.properties`).
- `make up` / `make dev`: build images, start the Dockerized stack, and wait for the health check; app listens on `http://localhost:8080`.
- `./gradlew test`: execute the JUnit suite without Docker.
- `make test`: run the Gradle test task inside Docker; ensure companion Compose overrides are present.
- `make logs` or `make logs-all`: follow application or full stack logs for troubleshooting.

## Coding Style & Naming Conventions
- Target Java 25 with 4-space indentation; keep lines under ~120 characters.
- Package names stay lowercase; classes and records use PascalCase, methods and variables use camelCase.
- Favor constructor injection; keep SQL in repository classes under `...gregfullstack`.
- Format templates with consistent attribute ordering; keep fragments reusable and avoid inline scripts.
- Update configuration via `application.properties` (local) or `application-docker.properties` (Compose) instead of hardcoding.

## Testing Guidelines
- Use JUnit 5 and Spring Boot test utilities; annotate integration cases with `@SpringBootTest`.
- Name test classes `*Tests` and place fixtures beside the code they validate.
- Seed database state via Flyway or dedicated SQL scripts rather than ad-hoc inserts.
- Run `./gradlew test` before pushing; aim to keep Spring context-based tests fast and focused.

## Commit & Pull Request Guidelines
- Follow the existing history: concise, imperative subjects (e.g., "Add product search functionality").
- Reference related issues or tickets in the body, and list manual test steps or commands executed.
- For UI changes, attach before/after screenshots or GIFs from the Thymeleaf pages.
- Keep PRs narrowly scoped; ensure CI (Gradle build/tests or Docker smoke checks) passes before requesting review.
