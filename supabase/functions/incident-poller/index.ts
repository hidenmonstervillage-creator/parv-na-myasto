// supabase/functions/incident-poller/index.ts
// ─────────────────────────────────────────────
// Cron-triggered every 90s. Fetches Waze livemap for Bulgaria,
// deduplicates by waze_uuid, inserts new incidents.
//
// Deploy: supabase functions deploy incident-poller
// Schedule: via Supabase Dashboard → Database → Extensions → pg_cron
//   SELECT cron.schedule('poll-incidents', '*/2 * * * *',
//     $$SELECT net.http_post('https://<project>.supabase.co/functions/v1/incident-poller', ...);$$
//   );

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const BULGARIA_BBOX = {
  top: 44.22,
  bottom: 41.23,
  left: 22.36,
  right: 28.61,
};

const WAZE_URL = `https://www.waze.com/live-map/api/georss?top=${BULGARIA_BBOX.top}&bottom=${BULGARIA_BBOX.bottom}&left=${BULGARIA_BBOX.left}&right=${BULGARIA_BBOX.right}&env=row&types=alerts`;

Deno.serve(async (_req) => {
  try {
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    );

    // 1. Fetch Waze data
    const wazeRes = await fetch(WAZE_URL, {
      headers: {
        'User-Agent': 'Mozilla/5.0',
        'Referer': 'https://www.waze.com/live-map',
      },
    });

    if (!wazeRes.ok) {
      throw new Error(`Waze API returned ${wazeRes.status}`);
    }

    const data = await wazeRes.json();
    const alerts = data.alerts ?? [];

    // 2. Filter to ACCIDENT and HAZARD types
    const relevant = alerts.filter(
      (a: { type: string }) => a.type === 'ACCIDENT' || a.type === 'HAZARD'
    );

    if (relevant.length === 0) {
      return new Response(JSON.stringify({ inserted: 0 }), { status: 200 });
    }

    // 3. Upsert (dedup by waze_uuid)
    const rows = relevant.map((a: Record<string, unknown>) => ({
      waze_uuid: a.uuid as string,
      type: a.type as string,
      subtype: (a.subtype as string) ?? null,
      lat: (a.location as { y: number }).y,
      lng: (a.location as { x: number }).x,
      street: (a.street as string) ?? null,
      city: (a.city as string) ?? null,
      reported_at: new Date((a.pubMillis as number) ?? Date.now()).toISOString(),
      raw_json: a,
    }));

    const { data: inserted, error } = await supabase
      .from('incidents')
      .upsert(rows, { onConflict: 'waze_uuid', ignoreDuplicates: true })
      .select('id');

    if (error) throw error;

    return new Response(
      JSON.stringify({ fetched: relevant.length, inserted: inserted?.length ?? 0 }),
      { status: 200 }
    );
  } catch (err) {
    console.error('Poller error:', err);
    return new Response(JSON.stringify({ error: (err as Error).message }), { status: 500 });
  }
});
