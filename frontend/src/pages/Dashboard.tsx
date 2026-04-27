import { useState } from 'react';
import MapView from '../components/Map/MapView';
import IncidentList from '../components/IncidentList/IncidentList';
import Topbar from '../components/Layout/Topbar';
import { useIncidents } from '../hooks/useIncidents';
import { useNotifications } from '../hooks/useNotifications';
import type { IncidentWithDistance } from '../lib/types';

// Default operator position: central Sofia
const DEFAULT_OPERATOR = {
  lat: 42.6977,
  lng: 23.3219,
  radius: 15, // km
};

export default function Dashboard() {
  const [selectedIncident, setSelectedIncident] = useState<string | null>(null);
  const [showList, setShowList] = useState(false);

  const { incidents, loading } = useIncidents(DEFAULT_OPERATOR.lat, DEFAULT_OPERATOR.lng);

  // Enable push notifications
  useNotifications(incidents);

  const handleIncidentClick = (incident: IncidentWithDistance) => {
    setSelectedIncident(incident.id === selectedIncident ? null : incident.id);
  };

  if (loading) {
    return (
      <div className="h-screen flex items-center justify-center bg-bg">
        <div className="text-center">
          <div className="w-10 h-10 border-2 border-accent-red border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p className="text-gray-500 text-sm">Зареждане на инциденти...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen flex flex-col">
      <Topbar incidentCount={incidents.length} isConnected={true} />

      {/* Desktop: side-by-side / Mobile: map + bottom toggle */}
      <div className="flex-1 flex relative overflow-hidden">
        {/* Map */}
        <div className="flex-1 relative">
          <MapView
            incidents={incidents}
            operatorLat={DEFAULT_OPERATOR.lat}
            operatorLng={DEFAULT_OPERATOR.lng}
            radiusKm={DEFAULT_OPERATOR.radius}
            onIncidentClick={handleIncidentClick}
          />

          {/* Mobile: toggle list button */}
          <button
            onClick={() => setShowList(!showList)}
            className="absolute bottom-4 left-1/2 -translate-x-1/2 md:hidden
              bg-surface border border-border px-5 py-2.5 rounded-full shadow-xl
              text-sm font-bold flex items-center gap-2 z-10"
          >
            {showList ? (
              <>
                <span>🗺️</span> Карта
              </>
            ) : (
              <>
                <span>📋</span> Списък ({incidents.length})
              </>
            )}
          </button>
        </div>

        {/* Incident list — desktop sidebar / mobile overlay */}
        <div
          className={`
            absolute inset-0 md:relative md:w-96 md:border-l md:border-border
            transition-transform duration-300 z-20
            ${showList ? 'translate-y-0' : 'translate-y-full md:translate-y-0'}
          `}
        >
          <IncidentList
            incidents={incidents}
            selectedId={selectedIncident}
            onSelect={handleIncidentClick}
            radiusKm={DEFAULT_OPERATOR.radius}
          />
        </div>
      </div>
    </div>
  );
}
