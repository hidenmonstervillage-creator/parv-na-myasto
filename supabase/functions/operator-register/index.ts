// supabase/functions/operator-register/index.ts
// ──────────────────────────────────────────────
// Handles new operator signup. Creates auth user + operator profile.

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

Deno.serve(async (req) => {
  try {
    if (req.method !== 'POST') {
      return new Response('Method not allowed', { status: 405 });
    }

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    );

    const { name, phone, base_lat, base_lng, radius_km } = await req.json();

    // Validate
    if (!name || !phone || !base_lat || !base_lng) {
      return new Response(
        JSON.stringify({ error: 'Missing required fields: name, phone, base_lat, base_lng' }),
        { status: 400 }
      );
    }

    // Create operator profile
    const { data, error } = await supabase
      .from('operators')
      .insert({
        name,
        phone,
        base_lat,
        base_lng,
        radius_km: radius_km ?? 15,
        subscription_status: 'trial',
      })
      .select()
      .single();

    if (error) throw error;

    return new Response(JSON.stringify({ operator: data }), { status: 201 });
  } catch (err) {
    console.error('Register error:', err);
    return new Response(JSON.stringify({ error: (err as Error).message }), { status: 500 });
  }
});
