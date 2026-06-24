package com.parvnamyasto.incident;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Supabase Postgres-backed incident store, active when {@code app.storage=supabase}.
 *
 * <p>Same contract as {@link InMemoryIncidentStore}: deduplication is enforced by
 * the primary key with {@code ON CONFLICT (id) DO NOTHING}, so a new incident is
 * reported back from {@link #addIfNew} only on its first insert. Incidents now
 * survive restarts.
 */
@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "supabase")
public class JdbcIncidentStore implements IncidentStore {

    private static final RowMapper<Incident> MAPPER = (rs, n) -> new Incident(
            rs.getString("id"),
            rs.getString("type"),
            rs.getString("subtype"),
            rs.getDouble("lat"),
            rs.getDouble("lng"),
            rs.getString("street"),
            rs.getString("city"),
            rs.getObject("reported_at", OffsetDateTime.class).toInstant(),
            rs.getBoolean("on_scene"),
            rs.getString("on_scene_by")
    );

    private final JdbcTemplate jdbc;

    public JdbcIncidentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void ensureSchema() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS incidents (
                    id          TEXT PRIMARY KEY,
                    type        TEXT NOT NULL,
                    subtype     TEXT,
                    lat         DOUBLE PRECISION NOT NULL,
                    lng         DOUBLE PRECISION NOT NULL,
                    street      TEXT,
                    city        TEXT,
                    reported_at TIMESTAMPTZ NOT NULL
                )
                """);
        // On-scene columns, added separately so an existing table is migrated in place.
        jdbc.execute("ALTER TABLE incidents ADD COLUMN IF NOT EXISTS on_scene BOOLEAN NOT NULL DEFAULT FALSE");
        jdbc.execute("ALTER TABLE incidents ADD COLUMN IF NOT EXISTS on_scene_by TEXT");
    }

    @Override
    public Optional<Incident> addIfNew(Incident incident) {
        int rows = jdbc.update("""
                        INSERT INTO incidents (id, type, subtype, lat, lng, street, city, reported_at, on_scene, on_scene_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (id) DO NOTHING
                        """,
                incident.id(), incident.type(), incident.subtype(),
                incident.lat(), incident.lng(), incident.street(), incident.city(),
                OffsetDateTime.ofInstant(incident.reportedAt(), ZoneOffset.UTC),
                incident.onScene(), incident.onSceneBy());
        return rows == 1 ? Optional.of(incident) : Optional.empty();
    }

    @Override
    public List<Incident> all() {
        return jdbc.query("SELECT * FROM incidents ORDER BY reported_at DESC", MAPPER);
    }

    @Override
    public Optional<Incident> setOnScene(String id, boolean onScene, String onSceneBy) {
        return jdbc.query("""
                        UPDATE incidents SET on_scene = ?, on_scene_by = ?
                        WHERE id = ?
                        RETURNING *
                        """,
                MAPPER, onScene, onScene ? onSceneBy : null, id)
                .stream().findFirst();
    }
}
