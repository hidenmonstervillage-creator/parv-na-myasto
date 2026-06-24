package com.parvnamyasto.web;

import com.parvnamyasto.incident.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pushes new incidents to every connected browser over Server-Sent Events.
 *
 * <p>This is the live channel that makes the product work: the moment the poller
 * stores a fresh incident it is broadcast here, and each operator's map updates
 * without polling. It replaces the prototype's Supabase Realtime (a hosted
 * Postgres-replication WebSocket) with a few lines of plain Spring — no external
 * service, same "instant push" behaviour.
 */
@Component
public class IncidentStream {

    private static final Logger log = LoggerFactory.getLogger(IncidentStream.class);

    private final List<SseEmitter> clients = new CopyOnWriteArrayList<>();

    /** Registers a new browser connection. Called by the /api/stream endpoint. */
    public SseEmitter register() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // keep open
        emitter.onCompletion(() -> clients.remove(emitter));
        emitter.onTimeout(() -> clients.remove(emitter));
        emitter.onError(e -> clients.remove(emitter));
        clients.add(emitter);
        log.debug("Client connected ({} total)", clients.size());
        return emitter;
    }

    /** Sends one incident to every connected client as a named "incident" event. */
    public void broadcast(Incident incident) {
        for (SseEmitter client : clients) {
            try {
                client.send(SseEmitter.event().name("incident").data(incident));
            } catch (IOException e) {
                clients.remove(client); // client went away; drop it
            }
        }
    }
}
