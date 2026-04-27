import { useEffect, useRef } from 'react';
import type { IncidentWithDistance } from '../lib/types';

/**
 * Plays a sound and shows a browser notification when a new incident appears.
 * In production, this will use Web Push via Supabase.
 */
export function useNotifications(incidents: IncidentWithDistance[]) {
  const prevCount = useRef(incidents.length);
  const audioRef = useRef<HTMLAudioElement | null>(null);

  // Request notification permission on mount
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  }, []);

  useEffect(() => {
    if (incidents.length > prevCount.current) {
      const newest = incidents[0]; // sorted by distance, but newest is most relevant

      // Find the actually newest one
      const latest = incidents.reduce((a, b) =>
        new Date(b.reported_at) > new Date(a.reported_at) ? b : a
      );

      // Play alert sound
      if (!audioRef.current) {
        // Simple beep using Web Audio API
        try {
          const ctx = new AudioContext();
          const oscillator = ctx.createOscillator();
          const gain = ctx.createGain();
          oscillator.connect(gain);
          gain.connect(ctx.destination);
          oscillator.frequency.value = 880;
          gain.gain.value = 0.3;
          oscillator.start();
          oscillator.stop(ctx.currentTime + 0.2);
        } catch {
          // Audio not available
        }
      }

      // Browser notification
      if ('Notification' in window && Notification.permission === 'granted') {
        const typeLabel = latest.type === 'ACCIDENT' ? '🚨 ПТП' : '⚠️ Опасност';
        new Notification(`${typeLabel} — ${latest.street || 'Неизвестна улица'}`, {
          body: `${latest.city || ''} • ${latest.distance_km.toFixed(1)} км от вас`,
          icon: '/icons/icon-192.png',
          tag: latest.id, // prevents duplicate notifications
        });
      }
    }
    prevCount.current = incidents.length;
  }, [incidents]);
}
