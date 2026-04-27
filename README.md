# Пръв на Място (First on the Scene)

Real-time incident radar for tow truck operators in Bulgaria.
Monitors traffic incidents and pushes alerts to operators within their coverage zone.

## Quick Start

### 1. Frontend

```bash
cd frontend
cp .env.example .env      # add your Supabase credentials
npm install
npm run dev                # opens at http://localhost:5173
```

### 2. Supabase

```bash
# Install Supabase CLI: https://supabase.com/docs/guides/cli
supabase init
supabase db push           # runs migrations/001_initial_schema.sql
supabase functions deploy incident-poller
supabase functions deploy incident-notifier
supabase functions deploy operator-register
```

### 3. Test on Phone

Vite dev server binds to `0.0.0.0:5173` — open `http://<your-local-ip>:5173` on your phone (same WiFi).

## Architecture

```
Frontend (React PWA)
  ↕ Supabase Realtime (WebSocket)
Supabase
  ├── incidents table ← incident-poller (cron, Waze API)
  ├── notifications table ← incident-notifier (webhook trigger)
  └── operators table ← operator-register (signup)
```

## Tech Stack

- **Frontend**: React 18 + TypeScript, Vite, Tailwind, MapLibre GL, react-map-gl
- **Backend**: Supabase (PostgreSQL + PostGIS + Edge Functions + Realtime)
- **Data Source**: Waze livemap API (MVP), TomTom (production)
- **Deploy**: Vercel (frontend) + Supabase (backend)
