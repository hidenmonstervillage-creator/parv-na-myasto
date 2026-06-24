package com.parvnamyasto.operator;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Supabase Postgres-backed operator store, active when {@code app.storage=supabase}.
 * Creates the {@code operators} table if absent and seeds it once from
 * {@link DefaultOperators}.
 */
@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "supabase")
public class JdbcOperatorStore implements OperatorStore {

    private static final RowMapper<Operator> MAPPER = (rs, n) -> new Operator(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("city"),
            rs.getDouble("base_lat"),
            rs.getDouble("base_lng"),
            rs.getDouble("radius_km")
    );

    private final JdbcTemplate jdbc;

    public JdbcOperatorStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void ensureSchemaAndSeed() {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS operators (
                    id        TEXT PRIMARY KEY,
                    name      TEXT NOT NULL,
                    city      TEXT,
                    base_lat  DOUBLE PRECISION NOT NULL,
                    base_lng  DOUBLE PRECISION NOT NULL,
                    radius_km DOUBLE PRECISION NOT NULL
                )
                """);
        Integer count = jdbc.queryForObject("SELECT count(*) FROM operators", Integer.class);
        if (count == null || count == 0) {
            for (Operator o : DefaultOperators.list()) {
                jdbc.update("""
                                INSERT INTO operators (id, name, city, base_lat, base_lng, radius_km)
                                VALUES (?, ?, ?, ?, ?, ?)
                                ON CONFLICT (id) DO NOTHING
                                """,
                        o.id(), o.name(), o.city(), o.baseLat(), o.baseLng(), o.radiusKm());
            }
        }
    }

    @Override
    public List<Operator> all() {
        return jdbc.query("SELECT * FROM operators ORDER BY id", MAPPER);
    }

    @Override
    public Optional<Operator> byId(String id) {
        return jdbc.query("SELECT * FROM operators WHERE id = ?", MAPPER, id).stream().findFirst();
    }
}
