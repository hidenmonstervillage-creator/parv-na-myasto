package com.parvnamyasto.operator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * In-memory operator store (default), seeded from {@link DefaultOperators}.
 * Active unless {@code app.storage=supabase}.
 */
@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryOperatorStore implements OperatorStore {

    private final List<Operator> operators = DefaultOperators.list();

    @Override
    public List<Operator> all() {
        return operators;
    }

    @Override
    public Optional<Operator> byId(String id) {
        return operators.stream().filter(o -> o.id().equals(id)).findFirst();
    }
}
