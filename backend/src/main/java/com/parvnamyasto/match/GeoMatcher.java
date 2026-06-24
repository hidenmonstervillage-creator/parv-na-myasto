package com.parvnamyasto.match;

import com.parvnamyasto.incident.Incident;
import com.parvnamyasto.operator.Operator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * The core of the whole system: match incidents to operators by proximity.
 *
 * <p>The rule is one line — an incident belongs to an operator when it falls
 * inside the operator's coverage circle:
 *
 * <pre>distance(operator.base, incident) &lt;= operator.radiusKm</pre>
 *
 * <p>Distance is the great-circle (haversine) distance in kilometres. The
 * original prototype ran this same test inside Postgres with PostGIS
 * {@code ST_DWithin}; keeping it as a few lines of Java makes the idea — and the
 * "whoever is alerted first wins the job" product bet — easy to read and test.
 */
@Component
public class GeoMatcher {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /** Operators whose coverage circle contains the incident. */
    public List<Operator> operatorsInRange(Incident incident, List<Operator> operators) {
        return operators.stream()
                .filter(op -> isInRange(op, incident))
                .toList();
    }

    /**
     * Incidents inside one operator's coverage circle, nearest first.
     * Each result carries its distance so the UI can show "3.2 km away".
     */
    public List<IncidentMatch> incidentsInRange(Operator operator, List<Incident> incidents) {
        return incidents.stream()
                .map(inc -> new IncidentMatch(inc, distanceKm(
                        operator.baseLat(), operator.baseLng(), inc.lat(), inc.lng())))
                .filter(m -> m.distanceKm() <= operator.radiusKm())
                .sorted(Comparator.comparingDouble(IncidentMatch::distanceKm))
                .toList();
    }

    public boolean isInRange(Operator operator, Incident incident) {
        return distanceKm(operator.baseLat(), operator.baseLng(),
                incident.lat(), incident.lng()) <= operator.radiusKm();
    }

    /** Great-circle distance between two coordinates, in kilometres. */
    public double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
