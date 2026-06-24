package com.parvnamyasto.poller;

import com.parvnamyasto.incident.Incident;
import com.parvnamyasto.incident.IncidentStore;
import com.parvnamyasto.match.GeoMatcher;
import com.parvnamyasto.operator.Operator;
import com.parvnamyasto.operator.OperatorStore;
import com.parvnamyasto.web.IncidentStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The ingestion loop: poll the live Data Source → keep new incidents → match → push.
 *
 * <p>Every {@code poller.fixed-delay-ms} it asks the {@link IncidentSource} for the
 * current relevant incidents and, for each genuinely new one:
 * <ol>
 *   <li>stores it ({@link IncidentStore} dedups on the source's id),</li>
 *   <li>asks {@link GeoMatcher} which operators have it in range,</li>
 *   <li>broadcasts it to connected browsers via {@link IncidentStream}.</li>
 * </ol>
 * Latency is the product, so the fewer hops between "the feed reports it" and "the
 * operator's screen lights up", the better.
 */
@Component
@ConditionalOnProperty(name = "poller.enabled", havingValue = "true", matchIfMissing = true)
public class IncidentPoller {

    private static final Logger log = LoggerFactory.getLogger(IncidentPoller.class);

    private final IncidentSource source;
    private final IncidentStore incidents;
    private final OperatorStore operators;
    private final GeoMatcher matcher;
    private final IncidentStream stream;

    public IncidentPoller(IncidentSource source, IncidentStore incidents,
                          OperatorStore operators, GeoMatcher matcher, IncidentStream stream) {
        this.source = source;
        this.incidents = incidents;
        this.operators = operators;
        this.matcher = matcher;
        this.stream = stream;
    }

    @Scheduled(
            fixedDelayString = "${poller.fixed-delay-ms:90000}",
            initialDelayString = "${poller.initial-delay-ms:3000}"
    )
    public void poll() {
        try {
            List<Incident> fetched = source.fetch();
            int inserted = 0;

            for (Incident incident : fetched) {
                if (incident.id() == null) {
                    continue;
                }
                if (incidents.addIfNew(incident).isPresent()) {
                    inserted++;
                    handleNewIncident(incident);
                }
            }

            if (inserted > 0) {
                log.info("Poll complete: {} new incident(s)", inserted);
            } else {
                log.debug("Poll complete: no new incidents ({} fetched)", fetched.size());
            }
        } catch (Exception e) {
            // One bad poll must never kill the schedule; the next tick retries.
            // The dashboard simply shows its honest empty state meanwhile; it
            // never fabricates incidents.
            String msg = e.getMessage();
            if (msg != null && msg.length() > 120) {
                msg = msg.substring(0, 120) + "…";
            }
            log.warn("Poll failed ({}): {}", e.getClass().getSimpleName(), msg);
        }
    }

    /** Match the new incident to operators and push it live to all clients. */
    private void handleNewIncident(Incident incident) {
        List<Operator> matched = matcher.operatorsInRange(incident, operators.all());
        log.info("New {} at {},{} ({}) → matched {} operator(s)",
                incident.type(), incident.lat(), incident.lng(),
                incident.street() != null ? incident.street() : "?", matched.size());
        stream.broadcast(incident);
    }
}
