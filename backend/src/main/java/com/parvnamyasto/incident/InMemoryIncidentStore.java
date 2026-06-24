package com.parvnamyasto.incident;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory incident store (default). Keyed by incident id, so re-seeing the same
 * live incident on the next poll is a no-op.
 *
 * <p>Active unless {@code app.storage=supabase}, in which case
 * {@link JdbcIncidentStore} takes over.
 */
@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryIncidentStore implements IncidentStore {

    private final ConcurrentHashMap<String, Incident> byId = new ConcurrentHashMap<>();

    @Override
    public Optional<Incident> addIfNew(Incident incident) {
        Incident previous = byId.putIfAbsent(incident.id(), incident);
        return previous == null ? Optional.of(incident) : Optional.empty();
    }

    @Override
    public List<Incident> all() {
        return byId.values().stream()
                .sorted(Comparator.comparing(Incident::reportedAt).reversed())
                .toList();
    }

    @Override
    public Optional<Incident> setOnScene(String id, boolean onScene, String onSceneBy) {
        return Optional.ofNullable(
                byId.computeIfPresent(id, (k, inc) -> inc.withOnScene(onScene, onSceneBy)));
    }
}
