package com.parvnamyasto.incident;

import java.time.Instant;

/**
 * A traffic incident (accident or broken-down vehicle) pulled from the live feed.
 *
 * <p>{@code id} is the source's own alert id — using it as our id means the same
 * real-world incident reported on every poll is recognised as the same row, so
 * dedup is free (see {@link IncidentStore}).
 *
 * <p>{@code onScene} is our own, app-level signal (not from the feed): an operator
 * can mark that a roadside helper is already at the incident — the Waze "roadside
 * help" idea, rebuilt in our UI. {@code onSceneBy} records which operator set it,
 * so the claim is attributable rather than anonymous. A false claim cannot hide a
 * job: the incident stays visible to everyone, just labelled.
 */
public record Incident(
        String id,
        String type,        // ACCIDENT | HAZARD
        String subtype,     // e.g. ACCIDENT_MAJOR, may be null
        double lat,
        double lng,
        String street,      // may be null
        String city,        // may be null
        Instant reportedAt,
        boolean onScene,    // a helper has been marked on scene
        String onSceneBy    // which operator marked it, null when not on scene
) {

    /** A freshly ingested incident: nobody has marked a helper on scene yet. */
    public static Incident reported(
            String id, String type, String subtype, double lat, double lng,
            String street, String city, Instant reportedAt) {
        return new Incident(id, type, subtype, lat, lng, street, city, reportedAt, false, null);
    }

    /**
     * A copy with the on-scene flag set (or cleared). When clearing, the
     * "marked by" attribution is dropped too.
     */
    public Incident withOnScene(boolean onScene, String onSceneBy) {
        return new Incident(id, type, subtype, lat, lng, street, city, reportedAt,
                onScene, onScene ? onSceneBy : null);
    }
}
