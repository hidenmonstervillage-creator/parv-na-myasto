---
project_name: 'parv-na-myasto'
user_name: 'Ivayla'
date: '2026-06-24'
sections_completed: ['technology_stack']
existing_patterns_found: 8
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in this project. Focus on unobvious details that agents might otherwise miss._

---

## Technology Stack & Versions

- **Language:** Java 17 (`<java.version>17</java.version>`; JDK 24 also works at runtime).
- **Framework:** Spring Boot 3.4.1 (parent POM) — single self-contained app.
- **Build:** Maven via the bundled **Maven Wrapper** (`mvnw` / `mvnw.cmd`). Maven is NOT assumed installed — always use the wrapper.
- **Modules in use:** `spring-boot-starter-web` (REST + SSE + serves the static UI), `spring-boot-starter-jdbc` (JdbcTemplate + HikariCP, Supabase mode only), `postgresql` (runtime), `spring-boot-starter-test` (test scope).
- **Frontend:** one static page — `backend/src/main/resources/static/index.html` with Leaflet. No Node/React/build step; Spring serves it directly.
- **Data source:** Waze livemap API (MVP intent), **TomTom Traffic Incident Details API (production, live)** behind the `IncidentSource` seam. Waze hard-blocks automated requests (`403`), so TomTom is the running source; the seam keeps the swap to one class. The poller keeps **only crashes + broken-down vehicles** (TomTom iconCategory 1 + 14); all other categories are dropped.
- **Persistence:** in-memory by default (`ConcurrentHashMap`); `APP_STORAGE=supabase` switches to Supabase Postgres via JDBC behind the same store interfaces.
- **Package root:** `com.parvnamyasto`.
- **App version:** `0.1.0` (jar: `target/backend-0.1.0.jar`).

## Critical Implementation Rules

_Documented in the generation phase (step 2)._
