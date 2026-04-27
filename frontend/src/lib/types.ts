// ── Database row types (mirror Supabase schema) ──

export interface Operator {
  id: string;
  name: string;
  phone: string;
  base_lat: number;
  base_lng: number;
  radius_km: number;
  active: boolean;
  subscription_status: 'trial' | 'starter' | 'pro' | 'fleet' | 'expired';
  created_at: string;
}

export type IncidentType = 'ACCIDENT' | 'HAZARD';

export type IncidentSubtype =
  | 'ACCIDENT_MINOR'
  | 'ACCIDENT_MAJOR'
  | 'HAZARD_ON_ROAD'
  | 'HAZARD_ON_SHOULDER'
  | 'HAZARD_WEATHER';

export interface Incident {
  id: string;
  waze_uuid: string;
  type: IncidentType;
  subtype: IncidentSubtype | null;
  lat: number;
  lng: number;
  street: string | null;
  city: string | null;
  reported_at: string;
  raw_json: Record<string, unknown> | null;
}

export interface Notification {
  id: string;
  operator_id: string;
  incident_id: string;
  delivered_at: string;
  seen_at: string | null;
  converted: boolean;
}

// ── Frontend-only types ──

export interface IncidentWithDistance extends Incident {
  distance_km: number;
}

export interface OperatorLocation {
  lat: number;
  lng: number;
  accuracy: number;
  timestamp: number;
}

// Map viewport
export interface MapViewport {
  latitude: number;
  longitude: number;
  zoom: number;
}

// Bulgaria bounding box for Waze API
export const BULGARIA_BBOX = {
  top: 44.22,
  bottom: 41.23,
  left: 22.36,
  right: 28.61,
} as const;

// Sofia default center
export const SOFIA_CENTER = {
  latitude: 42.6977,
  longitude: 23.3219,
  zoom: 12,
} as const;
