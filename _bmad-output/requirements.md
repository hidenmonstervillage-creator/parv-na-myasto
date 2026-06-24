---
project_name: 'parv-na-myasto'
artifact: 'Requirements (PRD)'
author: 'Ivayla'
method: 'BMAD — bmad-prd / bmad-create-prd'
date: '2026-06-24'
status: 'reconciled with shipped MVP (v0.1.0)'
---

# Requirements — Пръв на Място

Functional and non-functional requirements for the MVP, traced to the code that
satisfies them. See the [product brief](product-brief.md) for the why and
[architecture](architecture.md) for the how.

## Goals

- **G1.** Cut the time from "incident appears on a live feed" to "right operator is
  alerted" to near zero.
- **G2.** Alert *only* the operators whose coverage area contains the incident.
- **G3.** Never show fabricated data — an empty feed must look empty.
- **G4.** Keep the data provider swappable without touching the matching logic.

## Personas

- **Operator** — a tow / road-assistance driver covering one city + ring road.
  Wants only the jobs they can realistically reach first.
- **(Future) Operator-admin** — signs up, defines coverage, owns identity for claims.

---

## Functional requirements

| ID | Requirement | Priority | Satisfied by |
| --- | --- | --- | --- |
| **FR1** | Poll a live traffic-incident source on a fixed schedule. | Must | `IncidentPoller` (`@Scheduled`, `poller.fixed-delay-ms` = 90s) |
| **FR2** | Keep only real tow jobs — crashes + broken-down vehicles (TomTom iconCategory 1 + 14); drop all other categories. | Must | `TomTomClient` filter |
| **FR3** | Deduplicate incidents by the source's own id so a repeatedly-seen incident is processed once. | Must | `IncidentStore.addIfNew` |
| **FR4** | For each *new* incident, compute which operators have it in coverage. | Must | `GeoMatcher.operatorsInRange` |
| **FR5** | Answer "which incidents are inside this operator's radius, nearest first" with each result's distance. | Must | `GeoMatcher.incidentsInRange`, `GET /api/operators/{id}/incidents` |
| **FR6** | Push new/updated incidents to all connected browsers with no refresh. | Must | `IncidentStream` (SSE), `GET /api/stream` |
| **FR7** | Render incidents and the selected operator's coverage circle on a live map with a one-tap Navigate link. | Must | `static/index.html` (Leaflet) |
| **FR8** | Expose operators and their coverage circles. | Must | `GET /api/operators` |
| **FR9** | Expose every incident seen so far. | Must | `GET /api/incidents` |
| **FR10** | Let any operator mark/clear "helper on scene"; relabel the job live for everyone; record *which* operator claimed it; never hide the job. | Should | `POST /api/incidents/{id}/on-scene`, `onSceneBy` |
| **FR11** | Seed a starter set of operators across BG so matching is visible out of the box. | Should | `DefaultOperators` (Sofia, Plovdiv, Varna, Burgas, V. Tarnovo, Ruse) |
| **FR12** | Optionally persist incidents + operators in Postgres. | Could | `APP_STORAGE=supabase` → `Jdbc*Store` |

## Non-functional requirements

| ID | Requirement | Rationale / evidence |
| --- | --- | --- |
| **NFR1 — Latency** | New incident → operator's screen within one poll cycle (~90s), minimal hops. | Poll → store → match → SSE broadcast, no intermediate queue. |
| **NFR2 — Honesty** | No key / failed poll ⇒ truthful empty state, never fake data. | `IncidentPoller` swallows poll errors and logs; UI shows empty state. |
| **NFR3 — Resilience** | One bad poll must never stop the schedule; next tick retries. | `try/catch` around the poll body; `@Scheduled` continues. |
| **NFR4 — Provider independence** | Swapping Waze↔TomTom touches one class. | `IncidentSource` seam; `TomTomClient` is the only implementation. |
| **NFR5 — Storage independence** | Matching logic identical in-memory vs Postgres. | `IncidentStore` / `OperatorStore` interfaces; matcher never sees storage. |
| **NFR6 — Zero-setup run** | One command, no DB, no Node, no Maven install. | Maven Wrapper + `spring-boot-starter-web` serves the static UI. |
| **NFR7 — Readability** | Core stays a few small, testable classes. | `GeoMatcher` is plain haversine; covered by unit tests. |

## Acceptance criteria (key flows)

- **Live match.** Given a running app with a valid `TOMTOM_API_KEY`, when the feed
  reports a crash/breakdown inside an operator's radius, then within one poll cycle
  that incident appears on the operator's map and in `GET /api/operators/{id}/incidents`,
  ordered nearest-first with a distance.
- **Honest empty.** Given no key (or an empty live feed), `GET /api/incidents`
  returns `[]` and the UI shows the empty-state message — no fabricated rows.
- **On-scene.** When an operator POSTs `on-scene=true`, every connected client
  relabels that incident live, `onSceneBy` records the claimant, and the incident
  remains visible to all.

## Explicitly out of scope (MVP)

Operator authentication & trustworthy claim identity; signup-driven coverage;
multi-country; native mobile; durable persistence as default (Supabase mode is
opt-in). These are the natural next steps once the speed bet is validated.
