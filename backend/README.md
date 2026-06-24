# Пръв на Място — Backend (Java Spring Boot)

A single self-contained Spring Boot app that owns the whole pipeline and also serves the
map UI. No external database or service.

```
@Scheduled poll (live TomTom)  →  store + dedup (in-memory)  →  proximity match (haversine)  →  SSE push
                                                                                                   │
                                                                          static Leaflet UI  ◀──────┘
```

## The pieces

| Class | Role |
| --- | --- |
| `poller.IncidentSource` | the data-source seam (provider-independent) |
| `poller.TomTomClient` | fetches live incidents from the TomTom Traffic API, keeps only crashes + broken-down vehicles, maps them to our model |
| `poller.IncidentPoller` | `@Scheduled` loop: poll → keep new → match → push |
| `incident.IncidentStore` | in-memory incidents, deduped on Waze's alert id |
| `operator.OperatorStore` | seeded tow-truck operators + their coverage radius |
| `match.GeoMatcher` | **the core** — which operators/incidents are in range (haversine) |
| `web.IncidentStream` | broadcasts new/updated incidents to browsers over Server-Sent Events |
| `web.ApiController` | the `/api/**` REST + SSE surface (incl. the "helper on scene" toggle) |

`GeoMatcher` is where the product lives. The original prototype ran this same proximity
test inside Postgres with PostGIS `ST_DWithin`; here it is a few lines of Java, which makes
the idea easy to read and unit-test. Because state sits behind `IncidentStore` /
`OperatorStore`, a real database can replace them later without touching the match.

## Run

Requires only JDK 17+ (JDK 24 works). A **Maven Wrapper** is bundled, so you do not need
Maven installed — `mvnw` downloads the right version on first run:

```bash
cd backend
# optional but recommended — real live incidents:
#   PowerShell:  $env:TOMTOM_API_KEY="your-free-key"
#   bash:        export TOMTOM_API_KEY=your-free-key
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
                              # then open http://localhost:8080
```

Or open `backend/` in IntelliJ IDEA / VS Code and run `ParvNaMyastoApplication`.

Build a jar: `./mvnw clean package` → `java -jar target/backend-0.1.0.jar`.

## Config (`src/main/resources/application.yml`, all env-overridable)

- `TOMTOM_API_KEY` — free key from [developer.tomtom.com](https://developer.tomtom.com); without it the app runs but shows no live incidents (honest empty state).
- `POLLER_FIXED_DELAY_MS` (default 90000) — poll cadence.
- `POLLER_INITIAL_DELAY_MS` (default 3000) — delay before the first poll.
- `POLLER_ENABLED` (default true) — turn ingestion off.
- `PORT` (default 8080).

## Storage — in-memory (default) or Supabase Postgres

By default incidents and operators live in memory (`app.storage=memory`), so the app runs
with no database. Set `app.storage=supabase` to persist them in Supabase Postgres instead —
incidents then survive restarts. The store sits behind an interface
(`IncidentStore` / `OperatorStore`), so the matching logic is identical either way:

- in-memory: `InMemoryIncidentStore`, `InMemoryOperatorStore`
- Supabase: `JdbcIncidentStore`, `JdbcOperatorStore` (+ `SupabaseConfig` datasource)

To use Supabase:

1. Create a free project at [supabase.com](https://supabase.com).
2. Project Settings → Database → copy the connection details (host, password).
3. Set env vars and run (the `incidents` and `operators` tables are created automatically
   on first run, and operators are seeded once — no migration step):

```bash
# PowerShell
$env:APP_STORAGE="supabase"
$env:SUPABASE_DB_URL="jdbc:postgresql://<host>:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USER="postgres"            # or postgres.<project-ref> when using the pooler
$env:SUPABASE_DB_PASSWORD="<your-db-password>"
.\mvnw.cmd spring-boot:run
```
