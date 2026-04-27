-- Enable PostGIS for geo queries
CREATE EXTENSION IF NOT EXISTS postgis;

-- ── Operators ──
CREATE TABLE operators (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  phone TEXT UNIQUE NOT NULL,
  base_lat DOUBLE PRECISION NOT NULL,
  base_lng DOUBLE PRECISION NOT NULL,
  radius_km INTEGER NOT NULL DEFAULT 15,
  active BOOLEAN NOT NULL DEFAULT true,
  subscription_status TEXT NOT NULL DEFAULT 'trial'
    CHECK (subscription_status IN ('trial', 'starter', 'pro', 'fleet', 'expired')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Geo column for fast spatial queries
ALTER TABLE operators ADD COLUMN base_location GEOGRAPHY(POINT, 4326)
  GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(base_lng, base_lat), 4326)::geography) STORED;

CREATE INDEX idx_operators_location ON operators USING GIST (base_location);
CREATE INDEX idx_operators_active ON operators (active) WHERE active = true;

-- ── Incidents ──
CREATE TABLE incidents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  waze_uuid TEXT UNIQUE NOT NULL,
  type TEXT NOT NULL CHECK (type IN ('ACCIDENT', 'HAZARD')),
  subtype TEXT,
  lat DOUBLE PRECISION NOT NULL,
  lng DOUBLE PRECISION NOT NULL,
  street TEXT,
  city TEXT,
  reported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  raw_json JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Geo column for spatial matching
ALTER TABLE incidents ADD COLUMN location GEOGRAPHY(POINT, 4326)
  GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(lng, lat), 4326)::geography) STORED;

CREATE INDEX idx_incidents_location ON incidents USING GIST (location);
CREATE INDEX idx_incidents_reported ON incidents (reported_at DESC);
CREATE INDEX idx_incidents_type ON incidents (type);

-- Auto-cleanup: delete incidents older than 24h
-- (run via pg_cron or Supabase scheduled function)

-- ── Notifications ──
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  operator_id UUID NOT NULL REFERENCES operators(id) ON DELETE CASCADE,
  incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
  delivered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  seen_at TIMESTAMPTZ,
  converted BOOLEAN NOT NULL DEFAULT false,

  UNIQUE (operator_id, incident_id)
);

CREATE INDEX idx_notifications_operator ON notifications (operator_id, delivered_at DESC);
CREATE INDEX idx_notifications_converted ON notifications (operator_id, converted)
  WHERE converted = true;

-- ── Row Level Security ──
ALTER TABLE operators ENABLE ROW LEVEL SECURITY;
ALTER TABLE incidents ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Incidents are readable by all authenticated users
CREATE POLICY "Incidents are viewable by authenticated users"
  ON incidents FOR SELECT
  TO authenticated
  USING (true);

-- Operators can only see their own row
CREATE POLICY "Operators can view own profile"
  ON operators FOR SELECT
  TO authenticated
  USING (auth.uid() = id);

-- Notifications scoped to operator
CREATE POLICY "Operators can view own notifications"
  ON notifications FOR SELECT
  TO authenticated
  USING (operator_id = auth.uid());

CREATE POLICY "Operators can update own notifications"
  ON notifications FOR UPDATE
  TO authenticated
  USING (operator_id = auth.uid());

-- ── Realtime ──
-- Enable realtime for incidents table so frontend gets live updates
ALTER PUBLICATION supabase_realtime ADD TABLE incidents;
