package com.parvnamyasto.web;

import com.parvnamyasto.incident.IncidentStore;
import com.parvnamyasto.match.GeoMatcher;
import com.parvnamyasto.match.IncidentMatch;
import com.parvnamyasto.operator.Operator;
import com.parvnamyasto.operator.OperatorStore;
import com.parvnamyasto.incident.Incident;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * The HTTP surface the map UI talks to.
 *
 * <ul>
 *   <li>GET /api/operators                  — the operators and their coverage circles</li>
 *   <li>GET /api/incidents                  — every incident seen so far</li>
 *   <li>GET /api/operators/{id}/incidents   — incidents inside one operator's radius (the match)</li>
 *   <li>GET /api/stream                      — live feed of new incidents (SSE)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final OperatorStore operators;
    private final IncidentStore incidents;
    private final GeoMatcher matcher;
    private final IncidentStream stream;

    public ApiController(OperatorStore operators, IncidentStore incidents,
                         GeoMatcher matcher, IncidentStream stream) {
        this.operators = operators;
        this.incidents = incidents;
        this.matcher = matcher;
        this.stream = stream;
    }

    @GetMapping("/operators")
    public List<Operator> operators() {
        return operators.all();
    }

    @GetMapping("/incidents")
    public List<?> incidents() {
        return incidents.all();
    }

    /** The core query: which incidents fall inside this operator's coverage, nearest first. */
    @GetMapping("/operators/{id}/incidents")
    public List<IncidentMatch> incidentsForOperator(@PathVariable String id) {
        Operator operator = operators.byId(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Unknown operator: " + id));
        return matcher.incidentsInRange(operator, incidents.all());
    }

    /**
     * Marks (or clears) "a roadside helper is on scene" — our rebuild of Waze's
     * roadside-help button. The updated incident is broadcast so every other
     * operator's map relabels it live. The incident stays visible to all: the flag
     * informs, it never hides the job.
     */
    @PostMapping("/incidents/{id}/on-scene")
    public Incident setOnScene(@PathVariable String id, @RequestBody OnSceneRequest body) {
        Incident updated = incidents.setOnScene(id, body.onScene(), body.operator())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Unknown incident: " + id));
        stream.broadcast(updated);
        return updated;
    }

    /** Body for {@link #setOnScene}: the new flag and which operator is setting it. */
    public record OnSceneRequest(boolean onScene, String operator) {
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return stream.register();
    }
}
