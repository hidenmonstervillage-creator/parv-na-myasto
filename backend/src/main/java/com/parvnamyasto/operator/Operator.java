package com.parvnamyasto.operator;

/**
 * A tow-truck operator and the area they cover.
 *
 * <p>An operator sits at a base point and serves everything within
 * {@code radiusKm} of it. That circle is the whole matching rule: an incident
 * belongs to an operator when it falls inside the circle.
 */
public record Operator(
        String id,
        String name,
        String city,
        double baseLat,
        double baseLng,
        double radiusKm
) {
}
