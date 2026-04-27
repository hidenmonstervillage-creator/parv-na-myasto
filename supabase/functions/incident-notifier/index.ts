// supabase/functions/incident-notifier/index.ts
// ──────────────────────────────────────────────
// Called via Database Webhook on INSERT to incidents table.
// Finds all active operators whose radius contains the new incident,
// creates notification rows. Supabase Realtime delivers to frontend.
//
// Setup: Supabase Dashboard → Database → Webhooks → New webhook
//   Table: incidents, Events: INSERT
//   URL: https://<project>.supabase.co/functions/v1/incident-notifier

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

Deno.serve(async (req) => {
  try {
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    );

    const payload = await req.json();
    const incident = payload.record;

    if (!incident?.lat || !incident?.lng) {
      return new Response(JSON.stringify({ error: 'No location data' }), { status: 400 });
    }

    // Find operators within range using PostGIS
    // ST_DWithin checks if two geographies are within a distance (meters)
    const { data: operators, error } = await supabase
      .rpc('find_operators_in_range', {
        incident_lat: incident.lat,
        incident_lng: incident.lng,
      });

    // NOTE: You need to create this RPC function in a migration:
    //
    // CREATE OR REPLACE FUNCTION find_operators_in_range(
    //   incident_lat DOUBLE PRECISION,
    //   incident_lng DOUBLE PRECISION
    // ) RETURNS SETOF operators AS $$
    //   SELECT *
    //   FROM operators
    //   WHERE active = true
    //     AND ST_DWithin(
    //       base_location,
    //       ST_SetSRID(ST_MakePoint(incident_lng, incident_lat), 4326)::geography,
    //       radius_km * 1000  -- convert km to meters
    //     );
    // $$ LANGUAGE sql;

    if (error) throw error;
    if (!operators || operators.length === 0) {
      return new Response(JSON.stringify({ notified: 0 }), { status: 200 });
    }

    // Create notification rows
    const notificationRows = operators.map((op: { id: string }) => ({
      operator_id: op.id,
      incident_id: incident.id,
    }));

    const { error: insertError } = await supabase
      .from('notifications')
      .insert(notificationRows);

    if (insertError) throw insertError;

    return new Response(
      JSON.stringify({ notified: operators.length }),
      { status: 200 }
    );
  } catch (err) {
    console.error('Notifier error:', err);
    return new Response(JSON.stringify({ error: (err as Error).message }), { status: 500 });
  }
});
