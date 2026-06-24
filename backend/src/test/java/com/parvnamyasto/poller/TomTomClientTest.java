package com.parvnamyasto.poller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The kept-incident filter: only crashes and broken-down vehicles are tow jobs;
 * every other TomTom category is dropped.
 */
class TomTomClientTest {

    @Test
    void accidentIsKeptAsAccident() {
        assertEquals("ACCIDENT", TomTomClient.typeFor(1));
    }

    @Test
    void brokenDownVehicleIsKeptAsHazard() {
        assertEquals("HAZARD", TomTomClient.typeFor(14));
    }

    @Test
    void hazardCategoriesAreDropped() {
        // 3 dangerous conditions, 5 ice, 7 lane closed, 8 road closed,
        // 9 road works, 11 flooding — all noise, not jobs.
        for (int category : new int[]{3, 5, 7, 8, 9, 11}) {
            assertNull(TomTomClient.typeFor(category), "category " + category + " must be dropped");
        }
    }

    @Test
    void jamAndUnknownAreDropped() {
        assertNull(TomTomClient.typeFor(6));  // jam
        assertNull(TomTomClient.typeFor(0));  // unknown
    }
}
