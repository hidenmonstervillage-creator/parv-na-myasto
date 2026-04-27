import { useRef, useCallback } from 'react';
import MapGL, { Marker, Source, Layer, NavigationControl } from 'react-map-gl/maplibre';
import type { MapRef } from 'react-map-gl/maplibre';
import 'maplibre-gl/dist/maplibre-gl.css';
import type { IncidentWithDistance } from '../../lib/types';
import { SOFIA_CENTER } from '../../lib/types';

interface MapViewProps {
  incidents: IncidentWithDistance[];
  operatorLat: number;
  operatorLng: number;
  radiusKm: number;
  onIncidentClick: (incident: IncidentWithDistance) => void;
}

export default function MapView({
  incidents,
  operatorLat,
  operatorLng,
  radiusKm,
  onIncidentClick,
}: MapViewProps) {
  const mapRef = useRef<MapRef>(null);

  const flyToIncident = useCallback((lat: number, lng: number) => {
    mapRef.current?.flyTo({ center: [lng, lat], zoom: 15, duration: 800 });
  }, []);

  // Generate circle polygon for operator radius
  const radiusGeoJSON = generateCircle(operatorLat, operatorLng, radiusKm);

  return (
    <MapGL
      ref={mapRef}
      initialViewState={SOFIA_CENTER}
      style={{ width: '100%', height: '100%' }}
      mapStyle="https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
    >
      <NavigationControl position="top-right" />

      {/* Operator radius circle */}
      <Source id="radius" type="geojson" data={radiusGeoJSON}>
        <Layer
          id="radius-fill"
          type="fill"
          paint={{ 'fill-color': '#3b82f6', 'fill-opacity': 0.08 }}
        />
        <Layer
          id="radius-border"
          type="line"
          paint={{ 'line-color': '#3b82f6', 'line-width': 2, 'line-opacity': 0.4 }}
        />
      </Source>

      {/* Operator base marker */}
      <Marker latitude={operatorLat} longitude={operatorLng}>
        <div className="w-4 h-4 bg-accent-blue rounded-full border-2 border-white shadow-lg" />
      </Marker>

      {/* Incident markers */}
      {incidents.map((inc) => (
        <Marker
          key={inc.id}
          latitude={inc.lat}
          longitude={inc.lng}
          onClick={(e) => {
            e.originalEvent.stopPropagation();
            flyToIncident(inc.lat, inc.lng);
            onIncidentClick(inc);
          }}
        >
          <div
            className={`relative w-8 h-8 rounded-full flex items-center justify-center cursor-pointer
              ${inc.type === 'ACCIDENT' ? 'bg-accent-red incident-pulse' : 'bg-accent-orange'}
              ${inc.subtype === 'ACCIDENT_MAJOR' ? 'w-10 h-10' : ''}
            `}
            title={inc.street || 'Инцидент'}
          >
            <span className="text-white text-sm font-bold">
              {inc.type === 'ACCIDENT' ? '!' : '⚠'}
            </span>
          </div>
        </Marker>
      ))}
    </MapGL>
  );
}

/**
 * Generate a GeoJSON circle polygon from center + radius.
 */
function generateCircle(lat: number, lng: number, radiusKm: number, steps = 64) {
  const coords: [number, number][] = [];
  for (let i = 0; i <= steps; i++) {
    const angle = (i * 360) / steps;
    const dx = radiusKm * Math.cos((angle * Math.PI) / 180);
    const dy = radiusKm * Math.sin((angle * Math.PI) / 180);
    coords.push([
      lng + (dx / (111.32 * Math.cos((lat * Math.PI) / 180))),
      lat + dy / 110.574,
    ]);
  }
  return {
    type: 'Feature' as const,
    geometry: { type: 'Polygon' as const, coordinates: [coords] },
    properties: {},
  };
}
