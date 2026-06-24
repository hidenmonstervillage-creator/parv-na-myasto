package com.parvnamyasto.match;

import com.parvnamyasto.incident.Incident;

/** An incident paired with how far it is from a given operator's base. */
public record IncidentMatch(Incident incident, double distanceKm) {
}
