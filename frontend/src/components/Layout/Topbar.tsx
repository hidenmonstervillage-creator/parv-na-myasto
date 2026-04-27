interface TopbarProps {
  incidentCount: number;
  isConnected: boolean;
}

export default function Topbar({ incidentCount, isConnected }: TopbarProps) {
  return (
    <header className="flex items-center justify-between px-4 py-3 bg-surface border-b border-border z-50">
      {/* Logo */}
      <div className="flex items-center gap-2">
        <div className="w-8 h-8 bg-accent-red rounded-lg flex items-center justify-center">
          <span className="text-white font-bold text-sm">1</span>
        </div>
        <div>
          <h1 className="text-sm font-extrabold leading-none tracking-tight">
            Пръв на Място
          </h1>
          <p className="text-[10px] text-gray-500 leading-none mt-0.5">
            incident radar
          </p>
        </div>
      </div>

      {/* Status badges */}
      <div className="flex items-center gap-3">
        {/* Incident counter */}
        <div className="flex items-center gap-1.5 bg-accent-red/15 text-accent-red px-2.5 py-1 rounded-full">
          <span className="w-1.5 h-1.5 rounded-full bg-accent-red animate-pulse" />
          <span className="text-xs font-bold font-mono">{incidentCount}</span>
        </div>

        {/* Connection status */}
        <div
          className={`w-2 h-2 rounded-full ${
            isConnected ? 'bg-accent-green' : 'bg-gray-600'
          }`}
          title={isConnected ? 'Свързан' : 'Офлайн'}
        />
      </div>
    </header>
  );
}
