# Spring Boot + HTMX Famme Catalog

Kotlin-based Spring Boot + HTMX application for keeping a trimmed product catalog in sync with the public Famme API. The app persists products in PostgreSQL, keeps at most 50 products, and exposes a small HTMX-enhanced UI for browsing and triggering syncs.

## Features
- Hourly scheduled sync that pulls `/products.json` from famme.no and upserts the latest 50 items.
- Manual sync endpoint exposed via HTMX-driven UI to refresh on demand.
- Server-side rendered views (Thymeleaf) powered by Spring MVC with HTMX fragments for incremental updates.
- PostgreSQL persistence using Spring JDBC and Flyway migrations (JSONB column for variant details).
- Docker resources for running PostgreSQL locally and optional containerized app deployment.

## Tech Stack
- Kotlin 2.2 (JVM toolchain pinned to Java 24)
- Spring Boot 3.5 (Web, Data JDBC, Validation, Actuator)
- Flyway for schema migrations
- PostgreSQL 15+
- Thymeleaf + HTMX for the frontend
- Gradle (Kotlin DSL)

## Project Structure
```
src/
  main/kotlin/com/respiroc/gregfullstack/
    GregFullstackApplication.kt     # Spring Boot entry point (scheduling enabled)
    controller/ProductController.kt
    model/Product.kt, ProductVariant.kt
    repository/ProductRepository.kt
    service/ProductSyncService.kt   # REST client + sync scheduler
  main/resources/
    application.properties           # Local profile (PostgreSQL on localhost)
    application-docker.properties    # Docker profile override
    db/migration/                     # Flyway baseline + JSONB migration scripts
    templates/                        # Thymeleaf templates & HTMX fragments
    static/htmx.min.js
  test/kotlin/...                      # Service and repository tests
docker/
  postgres/init.sql                   # Seed data or extensions for local DB
docker-compose.yml
```

## Prerequisites
- JDK 24 (or a compatible runtime matching the Gradle/Kotlin toolchain)
- Docker Desktop (optional but recommended for PostgreSQL)
- `./gradlew` (bundled wrapper) or a compatible Gradle installation

## Getting Started (Local)
1. **Start PostgreSQL**
   ```bash
   docker compose up -d postgres
   ```
   Database credentials match `application.properties` (`postgres/password`).

2. **Run database migrations & boot the app**
   ```bash
   ./gradlew bootRun
   ```
   Flyway runs automatically on startup. The app listens on `http://localhost:8080`.

3. **UI Walkthrough**
   - `GET /` renders a dashboard with product count and HTMX trigger buttons.
   - `GET /products` returns the product table fragment (HTMX swaps this into the page).
   - `POST /products/sync` forces an API sync and returns a status fragment.
   - `POST /products` adds a minimal product (with a single variant) using form input.

4. **Scheduled Sync**
   The `ProductSyncService` calls the Famme API on launch and every hour (`fixedDelay = 3600000`). It keeps only 50 products in the database by pruning older rows.

## Running in Docker (App + DB)
1. Uncomment the `app` service block in `docker-compose.yml`.
2. Build & start both containers:
   ```bash
   docker compose up --build
   ```
   The application uses the `docker` Spring profile automatically (`SPRING_PROFILES_ACTIVE=docker`) with logging redirected to `/app/logs`.

## Configuration
- Update database credentials/URL through `application.properties` or environment variables (standard Spring overrides apply, e.g. `SPRING_DATASOURCE_URL`).
- To change the max product limit or remote API URL, adjust constants in `ProductSyncService`.
- Logging levels can be overridden via `logging.level.*` properties.

## Tests
```bash
./gradlew test
```
The current suite uses Mockito-based unit tests (no database required). Add integration tests as needed for repository behaviour.

## Data Model Highlights
- `Product` is a Kotlin class storing metadata plus an in-memory list of `ProductVariant` objects. Setters return `this` to support fluent chaining (useful in repositories and builders).
- Variants are stored as JSONB (`variants` column) in PostgreSQL. Serialization/deserialization happens through Jackson in `ProductRepository`.

## Maintenance Notes
- Flyway migrations run on every startup; add new migrations to `src/main/resources/db/migration` with versioned filenames (`V3__...sql`).
- HTMX static asset lives under `src/main/resources/static/htmx.min.js`; update it when bumping HTMX.
- Sample payloads for manual testing live in `sample-products.json`.
