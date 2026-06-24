package com.parvnamyasto.incident;

import java.util.List;
import java.util.Optional;

/**
 * Storage for incidents. Two implementations sit behind this seam, chosen by the
 * {@code app.storage} property: {@link InMemoryIncidentStore} (default) and
 * {@link JdbcIncidentStore} (Supabase Postgres). The matching logic never knows
 * which one is in use.
 */
public interface IncidentStore {

    /**
     * Stores the incident if its id has not been seen before.
     *
     * @return the incident when it is new, or empty if it was already known.
     */
    Optional<Incident> addIfNew(Incident incident);

    /** All known incidents, most recently reported first. */
    List<Incident> all();

    /**
     * Marks (or clears) "a roadside helper is on scene" for one incident, recording
     * which operator set it. The incident itself stays in {@link #all()} either way —
     * the flag only labels it.
     *
     * @param id         the incident id
     * @param onScene    true to mark a helper on scene, false to clear it
     * @param onSceneBy  the operator setting it (ignored when clearing)
     * @return the updated incident, or empty if no incident has that id.
     */
    Optional<Incident> setOnScene(String id, boolean onScene, String onSceneBy);
}
