import { useState, useEffect, useCallback } from 'react';
import type { Incident, IncidentWithDistance } from '../lib/types';
import { distanceKm } from '../lib/geo';

// ── Mock incidents for prototype demo ──
const MOCK_INCIDENTS: Incident[] = [
  {
    id: '1',
    waze_uuid: 'mock-001',
    type: 'ACCIDENT',
    subtype: 'ACCIDENT_MAJOR',
    lat: 42.6945,
    lng: 23.3340,
    street: 'бул. Цариградско шосе',
    city: 'София',
    reported_at: new Date(Date.now() - 3 * 60_000).toISOString(),
    raw_json: null,
  },
  {
    id: '2',
    waze_uuid: 'mock-002',
    type: 'ACCIDENT',
    subtype: 'ACCIDENT_MINOR',
    lat: 42.7105,
    lng: 23.2890,
    street: 'бул. Ломско шосе',
    city: 'София',
    reported_at: new Date(Date.now() - 7 * 60_000).toISOString(),
    raw_json: null,
  },
  {
    id: '3',
    waze_uuid: 'mock-003',
    type: 'HAZARD',
    subtype: 'HAZARD_ON_ROAD',
    lat: 42.6830,
    lng: 23.3550,
    street: 'бул. Александър Малинов',
    city: 'София',
    reported_at: new Date(Date.now() - 12 * 60_000).toISOString(),
    raw_json: null,
  },
  {
    id: '4',
    waze_uuid: 'mock-004',
    type: 'ACCIDENT',
    subtype: 'ACCIDENT_MAJOR',
    lat: 42.6650,
    lng: 23.2750,
    street: 'Околовръстен път',
    city: 'София',
    reported_at: new Date(Date.now() - 1 * 60_000).toISOString(),
    raw_json: null,
  },
  {
    id: '5',
    waze_uuid: 'mock-005',
    type: 'HAZARD',
    subtype: 'HAZARD_ON_SHOULDER',
    lat: 42.7250,
    lng: 23.3100,
    street: 'Автомагистрала Хемус',
    city: 'София',
    reported_at: new Date(Date.now() - 18 * 60_000).toISOString(),
    raw_json: null,
  },
  {
    id: '6',
    waze_uuid: 'mock-006',
    type: 'ACCIDENT',
    subtype: 'ACCIDENT_MINOR',
    lat: 42.1500,
    lng: 24.7500,
    street: 'бул. Марица',
    city: 'Пловдив',
    reported_at: new Date(Date.now() - 5 * 60_000).toISOString(),
    raw_json: null,
  },
  {
    id: '7',
    waze_uuid: 'mock-007',
    type: 'ACCIDENT',
    subtype: 'ACCIDENT_MAJOR',
    lat: 42.4350,
    lng: 23.6500,
    street: 'Автомагистрала Тракия km 34',
    city: null,
    reported_at: new Date(Date.now() - 2 * 60_000).toISOString(),
    raw_json: null,
  },
  {
    id: '8',
    waze_uuid: 'mock-008',
    type: 'HAZARD',
    subtype: 'HAZARD_ON_ROAD',
    lat: 42.7020,
    lng: 23.3700,
    street: 'бул. Ботевградско шосе',
    city: 'София',
    reported_at: new Date(Date.now() - 9 * 60_000).toISOString(),
    raw_json: null,
  },
];

/**
 * Returns incidents sorted by distance from operator.
 * In production, this will subscribe to Supabase Realtime.
 */
export function useIncidents(operatorLat: number, operatorLng: number) {
  const [incidents, setIncidents] = useState<IncidentWithDistance[]>([]);
  const [loading, setLoading] = useState(true);

  const enrichWithDistance = useCallback(
    (raw: Incident[]): IncidentWithDistance[] => {
      return raw
        .map((inc) => ({
          ...inc,
          distance_km: distanceKm(operatorLat, operatorLng, inc.lat, inc.lng),
        }))
        .sort((a, b) => a.distance_km - b.distance_km);
    },
    [operatorLat, operatorLng]
  );

  useEffect(() => {
    // TODO: Replace with Supabase realtime subscription
    // const channel = supabase
    //   .channel('incidents')
    //   .on('postgres_changes', { event: 'INSERT', schema: 'public', table: 'incidents' }, (payload) => {
    //     setIncidents(prev => enrichWithDistance([...prev, payload.new as Incident]));
    //   })
    //   .subscribe();

    setIncidents(enrichWithDistance(MOCK_INCIDENTS));
    setLoading(false);

    // Simulate a new incident arriving every 30s
    const interval = setInterval(() => {
      const newIncident: Incident = {
        id: `live-${Date.now()}`,
        waze_uuid: `live-${Date.now()}`,
        type: Math.random() > 0.3 ? 'ACCIDENT' : 'HAZARD',
        subtype: Math.random() > 0.5 ? 'ACCIDENT_MAJOR' : 'ACCIDENT_MINOR',
        lat: 42.69 + (Math.random() - 0.5) * 0.06,
        lng: 23.32 + (Math.random() - 0.5) * 0.08,
        street: ['бул. България', 'бул. Витоша', 'Околовръстен път', 'бул. Сливница'][
          Math.floor(Math.random() * 4)
        ],
        city: 'София',
        reported_at: new Date().toISOString(),
        raw_json: null,
      };
      setIncidents((prev) => enrichWithDistance([...prev.map(p => ({ ...p })), newIncident]));
    }, 30_000);

    return () => clearInterval(interval);
  }, [enrichWithDistance]);

  return { incidents, loading };
}
