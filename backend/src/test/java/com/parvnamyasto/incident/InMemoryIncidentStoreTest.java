package com.parvnamyasto.incident;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The on-scene ("roadside helper here") flag and its attribution. */
class InMemoryIncidentStoreTest {

    private final InMemoryIncidentStore store = new InMemoryIncidentStore();

    private static Incident sample(String id) {
        return Incident.reported(id, "ACCIDENT", null, 42.7, 25.3, "A1", "Sofia", Instant.now());
    }

    @Test
    void freshIncidentIsNotOnScene() {
        Incident inc = store.addIfNew(sample("a")).orElseThrow();
        assertFalse(inc.onScene());
        assertNull(inc.onSceneBy());
    }

    @Test
    void markingOnSceneRecordsWhoSetIt() {
        store.addIfNew(sample("a"));

        Incident marked = store.setOnScene("a", true, "Spark Tow").orElseThrow();

        assertTrue(marked.onScene());
        assertEquals("Spark Tow", marked.onSceneBy());
        // and it is reflected in the stored copy
        assertTrue(store.all().get(0).onScene());
    }

    @Test
    void clearingOnSceneDropsTheAttribution() {
        store.addIfNew(sample("a"));
        store.setOnScene("a", true, "Spark Tow");

        Incident cleared = store.setOnScene("a", false, "Spark Tow").orElseThrow();

        assertFalse(cleared.onScene());
        assertNull(cleared.onSceneBy());
    }

    @Test
    void markingUnknownIncidentReturnsEmpty() {
        assertEquals(Optional.empty(), store.setOnScene("nope", true, "Spark Tow"));
    }

    @Test
    void markedIncidentStaysVisibleToEveryone() {
        store.addIfNew(sample("a"));
        store.setOnScene("a", true, "Spark Tow");
        // The flag labels, it never hides the job.
        assertEquals(1, store.all().size());
    }
}
