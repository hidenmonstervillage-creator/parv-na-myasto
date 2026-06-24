package com.parvnamyasto.poller;

import com.parvnamyasto.incident.Incident;

import java.util.List;

/**
 * A source of live traffic incidents. The seam that lets the ingestion pipeline
 * stay independent of any one provider — TomTom today, another feed tomorrow.
 */
public interface IncidentSource {

    /**
     * @return the current relevant incidents from the source, already mapped to
     *         our {@link Incident} model. Returns an empty list (never throws for
     *         a missing key) when the source is unavailable.
     */
    List<Incident> fetch();
}
