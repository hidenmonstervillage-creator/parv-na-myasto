# Addendum — PRD: Пръв на Място (2026-06-24)

Depth that belongs downstream (architecture / solution design) or is rationale-preserving,
kept out of the PRD's main narrative. Not authoritative for scope — `prd.md` is.

## Technical provenance: original prototype → simplified MVP

The repository originally implemented a full commercial design that required cloud accounts
to run at all:

- **Supabase** (Postgres + PostGIS + Realtime + Auth) as the platform.
- **Two backends doing overlapping work:** a Java Spring Boot service *and* a set of Deno
  Supabase Edge Functions (`incident-poller`, `incident-notifier`, `operator-register`),
  the latter "kept for reference."
- **PostGIS spatial matching** via a `find_operators_in_range` SQL function using
  `ST_DWithin` over a GIST-indexed `geography` column.
- **Supabase Auth Admin** operator signup, RLS policies linking `operators.id` to
  `auth.users`, and a `subscription_status` tier model (trial/starter/pro/fleet/expired).
- **React + Vite + Tailwind + MapLibre GL** frontend consuming Supabase Realtime.

To run end to end it needed: a Supabase project + CLI + migrations, JDK + Maven, Node,
`.env` files in three places, and a live Waze feed. The clever kernel (geo-proximity match
+ live push) was real but buried under that scaffolding.

The MVP collapses this to one Spring Boot app (see `prd.md` §10). The mapping:

| Original | MVP |
| --- | --- |
| `incident-poller` (Deno cron) + Spring `IncidentPoller` | single `IncidentPoller` (`@Scheduled`) |
| `incident-notifier` (DB webhook) + PostGIS `ST_DWithin` | `GeoMatcher` (haversine in Java) |
| `operator-register` + Supabase Auth | removed (seeded `OperatorStore`) |
| Supabase Postgres tables | `IncidentStore` / `OperatorStore` — in-memory by default, **optional Supabase Postgres** (`app.storage=supabase`) |
| Supabase Realtime (WebSocket) | `IncidentStream` (Server-Sent Events) |
| Waze livemap (scraped) | TomTom Traffic API behind an `IncidentSource` seam |
| React + MapLibre PWA | single static Leaflet page served by Spring |

## Rejected alternatives considered during simplification

- **Keep PostGIS, drop only the duplicate backend.** Rejected for the MVP's *default*: it would
  force a DB just to start, and hides the core match in SQL. Persistence was later added back as
  an **opt-in** (Supabase via JDBC) without moving the match out of Java; pushing the match into
  PostGIS `ST_DWithin` remains the scale-path option when operator counts grow.
- **All-TypeScript single app (Node + React).** Considered; rejected because the application
  is for a role where demonstrating Java/Spring is the goal.
- **Mock / seeded incident data.** Briefly used a startup incident seed so the demo always
  showed matches, then **removed it** on the principle that the product must never fabricate
  jobs. When the public Waze endpoint bot-blocks (403) or is quiet, the dashboard shows an
  honest empty-state message instead. Only the *operators* remain seeded (there is no signup
  yet); the incidents are always real. See PRD FR-8.

## Notes on correctness of the haversine match

- Great-circle (haversine) distance is a sphere approximation; at city/operator-radius scale
  (≤ ~20 km) the error vs. a proper geodesic is negligible for "is it in my zone?" decisions.
- `ST_DWithin` on a `geography` type uses true spheroidal distance; swapping the in-memory
  matcher for it later changes precision negligibly but adds an indexed lookup that matters
  only at large operator counts (the match is O(operators) per incident today).

## Scale path (when/if the vision is pursued)

1. **Persistence:** ✅ *done (optional)* — `JdbcIncidentStore` / `JdbcOperatorStore` persist to
   Supabase Postgres behind the store interfaces (`app.storage=supabase`); default stays in-memory.
2. **Spatial index:** add PostGIS + GIST + `ST_DWithin` behind the same store interface when
   operator/incident volume makes the linear scan meaningful.
3. **Delivery:** move from in-process SSE to a managed pub/sub (or keep SSE behind a gateway)
   for multi-node fan-out.
4. **Data source:** ✅ *done* — moved off the unsanctioned Waze scrape to the TomTom Traffic API
   behind `IncidentSource`; adding HERE or a paid tier is a drop-in.
5. **Identity & billing:** reintroduce operator signup, auth, and the subscription tiers.

## Operator seed (MVP)

Six operators across София (×2), Пловдив, Варна, Бургас, Велико Търново with radii 10–20 km,
covering the main cities so that real TomTom incidents in those areas match an operator. Only
the operators are seeded; incidents are always real (no fabricated data — see PRD FR-8). In
Supabase mode the same six are seeded into the `operators` table on first run.
