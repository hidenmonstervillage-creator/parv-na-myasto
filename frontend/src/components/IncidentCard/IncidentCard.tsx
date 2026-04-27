import { formatDistanceToNow } from 'date-fns';
import { bg } from 'date-fns/locale';
import type { IncidentWithDistance } from '../../lib/types';
import { formatDistance, googleMapsNavUrl, wazeNavUrl } from '../../lib/geo';

interface IncidentCardProps {
  incident: IncidentWithDistance;
  isSelected: boolean;
  onClick: () => void;
}

export default function IncidentCard({ incident, isSelected, onClick }: IncidentCardProps) {
  const isAccident = incident.type === 'ACCIDENT';
  const isMajor = incident.subtype === 'ACCIDENT_MAJOR';
  const timeAgo = formatDistanceToNow(new Date(incident.reported_at), {
    addSuffix: true,
    locale: bg,
  });

  return (
    <div
      onClick={onClick}
      className={`p-4 border-b border-border cursor-pointer transition-colors
        ${isSelected ? 'bg-surface2' : 'hover:bg-surface2/50'}
      `}
    >
      {/* Header row */}
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <span
            className={`w-3 h-3 rounded-full ${
              isAccident ? (isMajor ? 'bg-accent-red' : 'bg-accent-orange') : 'bg-accent-yellow'
            }`}
          />
          <span className="font-bold text-sm">
            {isAccident ? (isMajor ? 'Тежко ПТП' : 'ПТП') : 'Опасност'}
          </span>
        </div>
        <span className="text-xs text-gray-500 font-mono">
          {formatDistance(incident.distance_km)}
        </span>
      </div>

      {/* Street */}
      <p className="text-sm text-gray-300 mb-1">
        {incident.street || 'Неизвестна улица'}
        {incident.city && <span className="text-gray-500"> • {incident.city}</span>}
      </p>

      {/* Time */}
      <p className="text-xs text-gray-500">{timeAgo}</p>

      {/* Navigation buttons — only show when selected */}
      {isSelected && (
        <div className="flex gap-2 mt-3">
          <a
            href={googleMapsNavUrl(incident.lat, incident.lng)}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 text-center py-2 px-3 bg-accent-green/20 text-accent-green text-sm font-bold rounded-lg hover:bg-accent-green/30 transition-colors"
          >
            Google Maps
          </a>
          <a
            href={wazeNavUrl(incident.lat, incident.lng)}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 text-center py-2 px-3 bg-accent-blue/20 text-accent-blue text-sm font-bold rounded-lg hover:bg-accent-blue/30 transition-colors"
          >
            Waze
          </a>
        </div>
      )}
    </div>
  );
}
