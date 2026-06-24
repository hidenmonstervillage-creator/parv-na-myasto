package com.parvnamyasto.operator;

import java.util.List;

/**
 * The starter set of operators across Bulgaria, used to seed either store so the
 * matching is visible out of the box. In a real deployment this would be replaced
 * by an operator signup flow.
 */
public final class DefaultOperators {

    private DefaultOperators() {
    }

    public static List<Operator> list() {
        return List.of(
                new Operator("sofia-1",    "Пътна помощ София-Център",  "София",          42.6977, 23.3219, 12),
                new Operator("sofia-2",    "АвтоСервиз Люлин 24/7",      "София",          42.7150, 23.2600, 10),
                new Operator("plovdiv-1",  "Пътна помощ Тракия",          "Пловдив",        42.1354, 24.7453, 15),
                new Operator("varna-1",    "Морска Пътна помощ",          "Варна",          43.2141, 27.9147, 18),
                new Operator("burgas-1",   "Пътна помощ Бургас",          "Бургас",         42.5048, 27.4626, 15),
                new Operator("vtarnovo-1", "Помощ на пътя В. Търново",    "Велико Търново", 43.0757, 25.6172, 20),
                new Operator("ruse-1",     "Пътна помощ Русе Дунав",      "Русе",           43.8356, 25.9657, 15)
        );
    }
}
