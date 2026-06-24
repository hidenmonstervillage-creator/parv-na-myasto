package com.parvnamyasto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Пръв на Място — real-time incident radar for tow-truck operators.
 *
 * A single self-contained Spring Boot app that owns the whole pipeline:
 *
 *   poll (live TomTom traffic) → keep new incidents → match by proximity → push to the map
 *
 * No external database or service: incidents and operators live in memory, the
 * proximity match is plain Java (haversine), and new incidents are pushed to the
 * browser over Server-Sent Events. The static map UI is served from
 * src/main/resources/static, so `mvn spring-boot:run` is the only step.
 */
// Datasource auto-wiring is off by default; SupabaseConfig provides the only
// DataSource, and only when app.storage=supabase — so the in-memory build needs no DB.
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@EnableScheduling
@ConfigurationPropertiesScan
public class ParvNaMyastoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParvNaMyastoApplication.class, args);
    }
}
