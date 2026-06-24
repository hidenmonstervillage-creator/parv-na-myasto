package com.parvnamyasto.operator;

import java.util.List;
import java.util.Optional;

/**
 * Storage for operators. Backed either by {@link InMemoryOperatorStore} (default)
 * or {@link JdbcOperatorStore} (Supabase Postgres), chosen by {@code app.storage}.
 * Both are seeded from {@link DefaultOperators}.
 */
public interface OperatorStore {

    List<Operator> all();

    Optional<Operator> byId(String id);
}
