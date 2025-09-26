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
        if (year != null)
            filters.put("year", String.valueOf(year));
        if (country != null && !country.isBlank())
            filters.put("country_name", country);
        if (sessionType != null && !sessionType.isBlank())
            filters.put("session_name", sessionType);

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

        for (JsonNode s : sessionsArray) {
            try {
                String sessionKey = s.path("session_key").isMissingNode() ? null : s.path("session_key").asText();
                String sessionName = textOrNull(s, "session_name");
                String sessionTypeVal = textOrNull(s, "session_type");
                String countryName = textOrNull(s, "country_name");
                Integer yyyy = s.has("year") && s.get("year").canConvertToInt() ? s.get("year").intValue() : null;
                String dateStart = textOrNull(s, "date_start");
                String dateEnd = textOrNull(s, "date_end");
                String circuit = textOrNull(s, "circuit_short_name"); // optional

                ListingEventsResponseDto eventsResponseDto = new ListingEventsResponseDto();

                eventsResponseDto.setEventId(sessionKey != null ? sessionKey : UUID.randomUUID().toString());
                eventsResponseDto.setName(
                        (sessionName != null ? sessionName : "Session") + (circuit != null ? " - " + circuit : ""));
                eventsResponseDto.setCountry(countryName);
                eventsResponseDto.setYear(yyyy);
                eventsResponseDto.setSessionType(sessionTypeVal);

                if (dateStart != null) {
                    try {
                        OffsetDateTime odt = OffsetDateTime.parse(dateStart);
                        eventsResponseDto.setStartTime(odt.toInstant());
                    } catch (DateTimeParseException ex) {
                        // try parsing as Instant directly as fallback
                        try {
                            eventsResponseDto.setStartTime(Instant.parse(dateStart));
                        } catch (Exception ignore) {
                        }
                    }
                }

                // fetch drivers for this sessionKey (if sessionKey missing we skip drivers)
                List<DriverDto> drivers = fetchDriversForSession(sessionKey);
                eventsResponseDto.setDrivers(drivers);

                eventsResponseDtos.add(eventsResponseDto);
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

        if (sessionKey == null)
            return Collections.emptyList();

        String url = baseUrl + "/v1/drivers";
        Map<String, String> q = Collections.singletonMap("session_key", sessionKey);

        JsonNode root;
        try {
            root = httpClient.getJson(url, q);
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

        List<DriverDto> out = new ArrayList<>();
        for (JsonNode d : driversArray) {
            try {
                // prefer driver_number, fall back to id
                String driverNumber = textOrNull(d, "driver_number");
                if (driverNumber == null)
                    driverNumber = textOrNull(d, "id");

                // prefer full_name, else compose first_name + last_name
                String fullName = textOrNull(d, "full_name");
                if (fullName == null) {
                    String first = textOrNull(d, "first_name");
                    String last = textOrNull(d, "last_name");
                    if (first != null || last != null)
                        fullName = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
                }

                DriverDto dd = new DriverDto();
                dd.setDriverId(driverNumber != null ? Integer.parseInt(driverNumber) : (int) (Math.random() * 1000));
                dd.setFullName(fullName != null ? fullName : "Driver " + dd.getDriverId());
                dd.setOdds(RandomOdds.pick()); // 2 or 3 or 4

                out.add(dd);
            } catch (Exception ex) {
                log.warn("Skipping driver mapping due to error: {}", ex.getMessage());
            }
        }

        return out;
    }

    @Override
    public String getName() {
        return "openf1";
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
