import type { IncidentWithDistance } from '../../lib/types';
import IncidentCard from '../IncidentCard/IncidentCard';

interface IncidentListProps {
  incidents: IncidentWithDistance[];
  selectedId: string | null;
  onSelect: (incident: IncidentWithDistance) => void;
  radiusKm: number;
}

export default function IncidentList({
  incidents,
  selectedId,
  onSelect,
  radiusKm,
}: IncidentListProps) {
  const inRange = incidents.filter((i) => i.distance_km <= radiusKm);
  const outOfRange = incidents.filter((i) => i.distance_km > radiusKm);

  return (
    <div className="h-full flex flex-col bg-surface overflow-hidden">
      {/* Header */}
      <div className="p-4 border-b border-border flex items-center justify-between">
        <div>
          <h2 className="text-lg font-bold">Инциденти</h2>
          <p className="text-xs text-gray-500 mt-0.5">
            {inRange.length} в радиус • {incidents.length} общо
          </p>
        </div>
        <div className="flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-accent-green animate-pulse" />
          <span className="text-xs text-accent-green font-mono">LIVE</span>
        </div>
      </div>

      {/* List */}
      <div className="flex-1 overflow-y-auto hide-scrollbar">
        {inRange.length > 0 && (
          <>
            <div className="px-4 py-2 bg-accent-red/10 text-accent-red text-xs font-bold uppercase tracking-wider">
              В твоя радиус ({radiusKm} км)
            </div>
            {inRange.map((inc) => (
              <IncidentCard
                key={inc.id}
                incident={inc}
                isSelected={selectedId === inc.id}
                onClick={() => onSelect(inc)}
              />
            ))}
          </>
        )}

        {outOfRange.length > 0 && (
          <>
            <div className="px-4 py-2 bg-surface2 text-gray-500 text-xs font-bold uppercase tracking-wider">
              Извън радиус
            </div>
            {outOfRange.map((inc) => (
              <IncidentCard
                key={inc.id}
                incident={inc}
                isSelected={selectedId === inc.id}
                onClick={() => onSelect(inc)}
              />
            ))}
          </>
        )}

        {incidents.length === 0 && (
          <div className="p-8 text-center text-gray-500">
            <p className="text-3xl mb-2">✓</p>
            <p className="font-bold">Няма инциденти</p>
            <p className="text-sm mt-1">Ще получиш известие при нов</p>
          </div>
        )}
      </div>
    </div>
  );
}
