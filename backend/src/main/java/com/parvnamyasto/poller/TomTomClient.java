package com.parvnamyasto.poller;

import com.fasterxml.jackson.databind.JsonNode;
import com.parvnamyasto.config.TomTomProperties;
import com.parvnamyasto.incident.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fetches live traffic incidents from the TomTom Traffic Incident Details API
 * and maps them to our {@link Incident} model.
 *
 * <p>Only the two categories that are actual tow jobs are kept: <b>accidents</b>
 * (crashes) and <b>broken-down vehicles</b>. Everything else — road works, road/lane
 * closures, dangerous conditions, ice, flooding, jams, weather and unknowns — is
 * dropped as noise, not a job.
 *
 * <p>TomTom caps a single request's {@code bbox} at 10,000 km², far smaller than
 * Bulgaria. So the configured box is split into a grid of ~1°×1° tiles (each well
 * under the cap), every tile is queried, and the results are merged and deduped.
 *
 * @see <a href="https://developer.tomtom.com/traffic-api/documentation/traffic-incidents/incident-details">TomTom docs</a>
 */
@Component
public class TomTomClient implements IncidentSource {

    private static final Logger log = LoggerFactory.getLogger(TomTomClient.class);

    /** Tile size in degrees. ~1°×1° is ≈9,200 km² across Bulgaria — safely under TomTom's 10,000 km² cap. */
    private static final double TILE_DEGREES = 1.0;

    /**
     * TomTom iconCategory → our type. Only tow jobs are kept:
     * 1 = Accident (a crash); 14 = Broken-down vehicle (still a tow job, mapped to HAZARD).
     * Every other category (weather, jams, road works, closures, …) is dropped.
     */
    private static final Set<Integer> HAZARD_CATEGORIES = Set.of(14);
    private static final Map<Integer, String> SUBTYPES = Map.of(
            1, "ACCIDENT",
            14, "BROKEN_DOWN_VEHICLE"
    );

    /** Field selection — keep the payload small, ask only for what we map. */
    private static final String FIELDS =
            "{incidents{geometry{type,coordinates},properties{id,iconCategory,startTime,from,roadNumbers,events{description}}}}";

    private final RestClient http = RestClient.create();
    private final TomTomProperties props;
    private boolean warnedNoKey = false;

    public TomTomClient(TomTomProperties props) {
        this.props = props;
    }

    @Override
    public List<Incident> fetch() {
        if (!props.hasKey()) {
            if (!warnedNoKey) {
                log.warn("TOMTOM_API_KEY is not set — no live data. Get a free key at "
                        + "https://developer.tomtom.com and set the TOMTOM_API_KEY environment variable.");
                warnedNoKey = true;
            }
            return List.of();
        }

        // Merge across tiles, deduped by incident id (an incident on a tile border
        // can appear in two tiles).
        Map<String, Incident> byId = new LinkedHashMap<>();
        String firstError = null;
        for (double[] tile : tiles()) {
            try {
                for (Incident inc : fetchTile(tile)) {
                    byId.putIfAbsent(inc.id(), inc);
                }
            } catch (Exception e) {
                if (firstError == null) {
                    firstError = e.getClass().getSimpleName() + ": " + trim(e.getMessage());
                }
            }
        }
        if (byId.isEmpty() && firstError != null) {
            log.warn("TomTom fetch failed: {}", firstError);
        }
        return new ArrayList<>(byId.values());
    }

    /** Split the configured bounding box into a grid of tiles under TomTom's area cap. */
    private List<double[]> tiles() {
        List<double[]> tiles = new ArrayList<>();
        for (double lat = props.bottom(); lat < props.top(); lat += TILE_DEGREES) {
            for (double lon = props.left(); lon < props.right(); lon += TILE_DEGREES) {
                tiles.add(new double[]{
                        lon,                                       // minLon
                        lat,                                       // minLat
                        Math.min(lon + TILE_DEGREES, props.right()), // maxLon
                        Math.min(lat + TILE_DEGREES, props.top())    // maxLat
                });
            }
        }
        return tiles;
    }

    private List<Incident> fetchTile(double[] tile) {
        String bbox = tile[0] + "," + tile[1] + "," + tile[2] + "," + tile[3];
        String url = props.baseUrl()
                + "?key=" + enc(props.apiKey())
                + "&bbox=" + bbox
                + "&fields=" + enc(FIELDS)
                + "&language=en-GB&timeValidityFilter=present";

        JsonNode root = http.get().uri(URI.create(url)).retrieve().body(JsonNode.class);

        List<Incident> out = new ArrayList<>();
        if (root == null || !root.has("incidents")) {
            return out;
        }
        for (JsonNode feature : root.get("incidents")) {
            JsonNode p = feature.get("properties");
            if (p == null) {
                continue;
            }
            int category = p.path("iconCategory").asInt(0);
            String type = typeFor(category);
            if (type == null) {
                continue;
            }
            double[] lonLat = firstCoordinate(feature.get("geometry"));
            if (lonLat == null) {
                continue;
            }
            out.add(Incident.reported(
                    idOf(p, lonLat),
                    type,
                    SUBTYPES.get(category),
                    lonLat[1],   // latitude
                    lonLat[0],   // longitude
                    street(p),
                    null,
                    reportedAt(p)
            ));
        }
        return out;
    }

    /** Maps a TomTom iconCategory to our type, or {@code null} for a category we drop. */
    static String typeFor(int iconCategory) {
        if (iconCategory == 1) {
            return "ACCIDENT";
        }
        return HAZARD_CATEGORIES.contains(iconCategory) ? "HAZARD" : null;
    }

    /** TomTom geometry may be a Point or a (Multi)LineString — descend to the first [lon,lat]. */
    private static double[] firstCoordinate(JsonNode geometry) {
        if (geometry == null) {
            return null;
        }
        JsonNode c = geometry.get("coordinates");
        while (c != null && c.isArray() && c.size() > 0 && c.get(0).isArray()) {
            c = c.get(0);
        }
        if (c == null || !c.isArray() || c.size() < 2) {
            return null;
        }
        return new double[]{c.get(0).asDouble(), c.get(1).asDouble()};
    }

    private static String idOf(JsonNode props, double[] lonLat) {
        JsonNode id = props.get("id");
        if (id != null && !id.isNull() && !id.asText().isBlank()) {
            return id.asText();
        }
        // Stable fallback so the same incident dedups across polls.
        return String.format("%.5f,%.5f", lonLat[1], lonLat[0]);
    }

    private static String street(JsonNode props) {
        String from = text(props, "from");
        if (from != null) {
            return from;
        }
        JsonNode events = props.get("events");
        if (events != null && events.isArray() && events.size() > 0) {
            String desc = text(events.get(0), "description");
            if (desc != null) {
                return desc;
            }
        }
        JsonNode roads = props.get("roadNumbers");
        if (roads != null && roads.isArray() && roads.size() > 0) {
            return roads.get(0).asText();
        }
        return null;
    }

    private static Instant reportedAt(JsonNode props) {
        String start = text(props, "startTime");
        if (start != null) {
            try {
                return Instant.parse(start);
            } catch (Exception ignored) {
                // fall through to now()
            }
        }
        return Instant.now();
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull() || v.asText().isBlank()) ? null : v.asText();
    }

    private static String trim(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
