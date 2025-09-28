package org.example.formulaone.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.formulaone.dto.DriverDto;
import org.example.formulaone.dto.ListingEventsResponseDto;
import org.example.formulaone.exceptions.HttpClientException;
import org.example.formulaone.util.RandomOdds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenF1 provider adapter.
 * Calls:
 * - GET {baseUrl}/v1/sessions
 * - GET {baseUrl}/v1/drivers?session_key={sessionKey}
 *
 * Maps provider JSON to EventDto and DriverDto (assigns random odds from
 * {2,3,4}).
 */
@Service
@Slf4j
public class OpenF1Provider implements F1Provider {
    private final IHttpClient httpClient;
    private final String baseUrl;
    private final boolean enabled;

    public OpenF1Provider(final IHttpClient httpClient,
            @Value("${openf1.base-url:https://api.openf1.org}") final String baseUrl,
            @Value("${openf1.enabled:true}") final boolean enabled) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl != null && baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : (baseUrl == null ? "https://api.openf1.org" : baseUrl);
        this.enabled = enabled;
    }

    @Override
    public List<ListingEventsResponseDto> fetchSessions(Integer year, String country, String sessionType) {
        if (!enabled) {
            log.debug("OpenF1 provider is disabled. Returning empty list.");
            return Collections.emptyList();
        }

        Map<String, String> filters = new HashMap<>();
        if (year != null) {
            filters.put("year", String.valueOf(year));
        }
        if (country != null && !country.isBlank()) {
            filters.put("country_name", country);
        }
        if (sessionType != null && !sessionType.isBlank()) {
            filters.put("session_name", sessionType);
        }

        String url = baseUrl + "/v1/sessions";
        JsonNode root;
        try {
            root = httpClient.getJson(url, filters);
        } catch (HttpClientException ex) {
            if (ex.getMessage().contains("429")) {
                log.warn("Rate limit exceeded for OpenF1 sessions API. Returning empty list.");
            } else {
                log.warn("OpenF1 sessions fetch failed: {}", ex.getMessage());
            }
            return Collections.emptyList();
        } catch (Exception ex) {
            log.error("Unexpected error fetching sessions: ", ex);
            return Collections.emptyList();
        }

        JsonNode sessionsArray = (root != null && root.isArray()) ? root : (root != null ? root.path("data") : null);
        if (sessionsArray == null || !sessionsArray.isArray()) {
            log.debug("No sessions array present in provider response");
            return Collections.emptyList();
        }

        List<ListingEventsResponseDto> eventsResponseDtos = new ArrayList<>();

        for (JsonNode sessionNode : sessionsArray) {
            try {
                String sessionKey = sessionNode.path("session_key").isMissingNode() ? null
                        : sessionNode.path("session_key").asText();
                String sessionName = textOrNull(sessionNode, "session_name");
                String sessionTypeVal = textOrNull(sessionNode, "session_type");
                String countryName = textOrNull(sessionNode, "country_name");
                Integer yearVal = sessionNode.has("year") && sessionNode.get("year").canConvertToInt()
                        ? sessionNode.get("year").intValue()
                        : null;
                String circuit = textOrNull(sessionNode, "circuit_short_name");

                ListingEventsResponseDto eventDto = new ListingEventsResponseDto();
                eventDto.setEventId(sessionKey != null ? sessionKey : UUID.randomUUID().toString());
                eventDto.setName(buildEventName(sessionName, circuit));
                eventDto.setCountry(countryName);
                eventDto.setYear(yearVal);
                eventDto.setSessionType(sessionTypeVal);
                eventDto.setStartTime(parseStartTime(sessionNode));

                // Fetch drivers for this session
                List<DriverDto> drivers = fetchDriversForSession(sessionKey);
                eventDto.setDrivers(drivers);

                eventsResponseDtos.add(eventDto);
            } catch (Exception ex) {
                log.warn("Skipped a session due to mapping error: {}", ex.getMessage());
            }
        }

        return eventsResponseDtos;
    }

    @Override
    public List<DriverDto> fetchDriversForSession(String sessionKey) {
        if (!enabled) {
            log.debug("OpenF1 provider is disabled. Returning empty list.");
            return Collections.emptyList();
        }

        if (sessionKey == null) {
            return Collections.emptyList();
        }

        String url = baseUrl + "/v1/drivers";
        Map<String, String> queryParams = Collections.singletonMap("session_key", sessionKey);

        JsonNode root;
        try {
            root = httpClient.getJson(url, queryParams);
        } catch (HttpClientException ex) {
            if (ex.getMessage().contains("429")) {
                log.warn("Rate limit exceeded for OpenF1 drivers API. Returning empty list for session: {}",
                        sessionKey);
            } else {
                log.warn("Failed to call OpenF1 drivers for session {}: {}", sessionKey, ex.getMessage());
            }
            return Collections.emptyList();
        } catch (Exception ex) {
            log.error("Unexpected error calling OpenF1 drivers for session " + sessionKey, ex);
            return Collections.emptyList();
        }

        JsonNode driversArray = (root != null && root.isArray()) ? root : (root != null ? root.path("data") : null);
        if (driversArray == null || !driversArray.isArray()) {
            log.debug("No drivers array in provider response for session {}", sessionKey);
            return Collections.emptyList();
        }

        List<DriverDto> drivers = new ArrayList<>();
        for (JsonNode driverNode : driversArray) {
            try {
                String driverNumber = textOrNull(driverNode, "driver_number");
                if (driverNumber == null) {
                    driverNumber = textOrNull(driverNode, "id");
                }

                String fullName = textOrNull(driverNode, "full_name");
                if (fullName == null) {
                    String firstName = textOrNull(driverNode, "first_name");
                    String lastName = textOrNull(driverNode, "last_name");
                    if (firstName != null || lastName != null) {
                        fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : ""))
                                .trim();
                    }
                }

                DriverDto driverDto = new DriverDto();
                driverDto.setDriverId(Integer.parseInt(driverNumber));
                driverDto.setFullName(fullName != null ? fullName : "Driver " + driverDto.getDriverId());
                driverDto.setOdds(RandomOdds.pick());

                drivers.add(driverDto);
            } catch (Exception ex) {
                log.warn("Skipping driver mapping due to error: {}", ex.getMessage());
            }
        }

        return drivers;
    }

    @Override
    public String getName() {
        return "openf1";
    }

    /**
     * Builds event name from session name and circuit.
     */
    private String buildEventName(String sessionName, String circuit) {
        String name = sessionName != null ? sessionName : "Session";
        return circuit != null ? name + " - " + circuit : name;
    }

    /**
     * Parses start time from session node.
     */
    private Instant parseStartTime(JsonNode sessionNode) {
        String dateStart = textOrNull(sessionNode, "date_start");
        if (dateStart == null) {
            return null;
        }

        try {
            OffsetDateTime odt = OffsetDateTime.parse(dateStart);
            return odt.toInstant();
        } catch (DateTimeParseException ex) {
            try {
                return Instant.parse(dateStart);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    private static String textOrNull(JsonNode n, String key) {
        if (n == null || key == null)
            return null;
        JsonNode v = n.get(key);
        if (v == null || v.isNull())
            return null;
        String t = v.asText().trim();
        return t.isEmpty() ? null : t;
    }
}