---
project_name: 'parv-na-myasto'
artifact: 'Architecture & Decisions'
author: 'Ivayla'
method: 'BMAD — bmad-create-architecture / bmad-agent-architect'
date: '2026-06-24'
status: 'reconciled with shipped MVP (v0.1.0)'
---

# Architecture & Decision Record — Пръв на Място

How the system is built, and the options that were weighed at each fork. Traces to
the [requirements](requirements.md). The guiding constraint throughout: **latency is
the product**, so every hop between "feed reports it" and "operator's screen lights
up" is suspect.

## 1. System shape

One self-contained Spring Boot app. The pipeline is linear and short:

```
                ┌──────────────────────── backend (one Spring Boot app) ───────────────────────┐
  live TomTom ──▶│  IncidentPoller  ──▶  IncidentStore  ──▶  GeoMatcher  ──▶  IncidentStream     │──▶ browser
   (every 90s)  │  (poll + filter)      (dedup, in-mem)     (haversine match)   (SSE push)      │   (Leaflet map)
                └──────────────────────────────────────────────────────────────────────────────┘
```

- **IncidentPoller** — `@Scheduled` loop: `source.fetch()` → for each genuinely new
  incident, store → match → broadcast. Wrapped in `try/catch` so one bad poll never
  kills the schedule.
- **IncidentSource / TomTomClient** — the provider seam; filters to crashes +
  broken-down vehicles only.
- **IncidentStore** — dedups by the source's id (`addIfNew`); also holds the
  on-scene flag.
- **GeoMatcher** — the core: haversine distance ≤ operator radius.
- **IncidentStream** — SSE fan-out to every connected browser.
- **ApiController** — the REST/SSE surface the Leaflet page consumes.

## 2. Key decisions (options weighed → choice)

### D1 — Data source: Waze vs TomTom
- **Options.** (a) Waze livemap (richest crowd-sourced feed, incl. roadside-help);
  (b) TomTom Traffic Incident Details API; (c) national 112/police feed (no public
  access).
- **Choice.** Design for Waze, **ship on TomTom.** Waze hard-blocks automated
  requests with `403`; TomTom offers a sanctioned, free-keyed live API.
- **Consequence.** Hidden behind `IncidentSource`, so the swap is one class
  (`NFR4`). Trade-off accepted: TomTom's BG incident coverage is thinner than Waze's,
  so the honest empty state is common — preferred over fabricating data.

### D2 — The match: PostGIS vs plain Java
- **Options.** (a) Postgres + PostGIS `ST_DWithin`; (b) a few lines of haversine in
  Java.
- **Choice.** **Plain Java haversine** (`GeoMatcher`).
- **Why.** Operator counts are small; the match is trivially fast in-process,
  trivially unit-testable, and keeps the product's central idea readable instead of
  buried in SQL. The PostGIS form remains a drop-in later if scale demands it.

### D3 — Delivery: polling the browser vs server push
- **Options.** (a) Browser polls REST every N seconds; (b) WebSocket; (c)
  Server-Sent Events.
- **Choice.** **SSE** (`IncidentStream`, `GET /api/stream`).
- **Why.** One-directional server→client is exactly the shape of the problem;
  SSE is simpler than WebSocket, auto-reconnects, and adds the fewest hops to the
  latency budget (`NFR1`).

### D4 — Storage: durable-by-default vs in-memory-by-default
- **Options.** (a) Always Postgres; (b) in-memory default with optional Postgres.
- **Choice.** **In-memory `ConcurrentHashMap` by default; `APP_STORAGE=supabase`
  opts into JDBC Postgres**, both behind `IncidentStore` / `OperatorStore`.
- **Why.** Zero-setup single-command run (`NFR6`) for the MVP/demo, with a real
  persistence path proven behind the same interface (`NFR5`) — the matcher never
  knows which is active.

### D5 — Frontend: SPA build vs single served page
- **Options.** (a) React/Vite SPA (an earlier `frontend/` existed); (b) one static
  Leaflet page served by Spring.
- **Choice.** **One static page** (`static/index.html`), no Node/build step.
- **Why.** The UI is a map + a list + an SSE subscription. A build pipeline would
  add ceremony and latency to iteration without buying anything the MVP needs. (The
  former React `frontend/` was removed in favour of this.)

### D6 — Trust model for "on scene"
- **Options.** (a) A claim hides the job; (b) a claim only *labels* the job and
  records who made it.
- **Choice.** **Label, never hide**, and record `onSceneBy`.
- **Why.** A wrong or malicious claim must not be able to make a real job vanish
  from anyone's map. Honesty over convenience (`NFR2`). Real claim *identity* (auth)
  is the acknowledged next step.

## 3. The seams that make it flexible

Three interfaces carry all the optionality, so swaps stay local:

| Seam | Swaps | Matcher impact |
| --- | --- | --- |
| `IncidentSource` | Waze ↔ TomTom ↔ mock | none |
| `IncidentStore` | in-memory ↔ Postgres | none |
| `OperatorStore` | in-memory ↔ Postgres | none |

`GeoMatcher` depends only on `Incident` and `Operator` value types — never on a
provider or a store — which is why the central logic is unit-tested in isolation.

## 4. The data-source layer (Waze / TomTom)

The whole pipeline depends on one provider-independent interface, not on any single
feed. Swapping the provider is a one-class change that touches neither the matcher
nor the live channel.

```
IncidentPoller ──▶ IncidentSource (interface)          fetch(): List<Incident>
                        ├── TomTomClient   ← the live implementation today
                        └── (Waze / mock)  ← future providers, behind the same seam
```

**Design intent vs. shipped reality.** The MVP targeted **Waze livemap** — the
richest crowd-sourced feed, including its roadside-help reports (the origin of our
`onScene` idea). But Waze hard-blocks automated requests with `403`, so the running
provider is **TomTom's sanctioned Traffic Incident Details API**. Waze survives only
as a concept rebuilt on our own data (`Incident.onScene` / `onSceneBy`).

**What `TomTomClient.fetch()` does** (`backend/.../poller/TomTomClient.java`):

1. **No key → honest empty.** Returns `List.of()` (logs once); never throws, never
   fabricates.
2. **Tiling.** TomTom caps one request at 10,000 km² — smaller than Bulgaria — so the
   configured bbox (lat 41.23–44.22, lng 22.36–28.61) is split into a grid of ~1°×1°
   tiles, each safely under the cap, and every tile is queried (`tiles()`).
3. **Query.** Per tile: `bbox` + a narrow `fields` selection (only what we map) +
   `timeValidityFilter=present` (active incidents only).
4. **Filter — only tow jobs.** Keep just `iconCategory` 1 (Accident → `ACCIDENT`)
   and 14 (Broken-down vehicle → `HAZARD`); drop road works, jams, weather, closures
   as noise (`typeFor`).
5. **Dedup across tiles.** A border incident seen in two tiles is merged by id
   (`byId.putIfAbsent`).
6. **Resilience.** A failing tile doesn't sink the others (per-tile `try/catch`); the
   first error is logged only if *every* tile failed.

**The translation step (the layer's real job).** TomTom returns GeoJSON (Point or
LineString geometry, nested coordinates). The client maps each one into our clean
`Incident` record so the rest of the system never sees provider-specific JSON:

| `Incident` field | Source in TomTom payload |
| --- | --- |
| `id` | `properties.id` (coordinate-based fallback when absent) → free dedup in `IncidentStore` |
| `lat` / `lng` | `firstCoordinate` descends nested geometry to the first `[lon,lat]` |
| `street` | `from` → event description → road number (first available) |
| `reportedAt` | `properties.startTime` (falls back to now) |
| `type` / `subtype` | derived from `iconCategory` |

Because `GeoMatcher`, `IncidentStore` and the UI depend only on `Incident`, the data
layer is a **thin adapter**: it absorbs the live, messy provider format, narrows it
to real tow jobs, and translates it into one internal model — so everything
downstream is provider-agnostic.

| Property | How it's achieved |
| --- | --- |
| Provider swap = 1 class | Everything sits behind `IncidentSource.fetch()` |
| Never fake data | Missing key / failure → `List.of()`, not an exception |
| Resilience | One bad tile doesn't sink the poll (per-tile `try/catch`) |
| Testability | Mock `IncidentSource` in tests, no network needed |

## 5. Tech stack

- **Backend:** Java 17, Spring Boot 3.4.1 — one app. `spring-boot-starter-web`
  (REST + SSE + serves the static UI), `spring-boot-starter-jdbc` + `postgresql`
  (Supabase mode only).
- **Frontend:** single static `index.html` + Leaflet. No Node/React/build.
- **Build/run:** Maven Wrapper (`mvnw` / `mvnw.cmd`) — Maven need not be installed.
- **Config:** `TOMTOM_API_KEY` (live data), `poller.fixed-delay-ms` (90s default),
  BG bounding box (lat 41.23–44.22, lng 22.36–28.61).

## 6. Known limitations / next steps

- **Thin live coverage.** TomTom reports few BG incidents at any moment; the empty
  state dominates. A second provider behind `IncidentSource` would densify the feed.
- **No operator identity.** On-scene claims are not yet authenticated.
- **In-memory default loses state on restart.** Acceptable for the MVP; Supabase
  mode addresses it.
- **Single-node SSE.** Fan-out is in-process; horizontal scale would need a shared
  broker.
