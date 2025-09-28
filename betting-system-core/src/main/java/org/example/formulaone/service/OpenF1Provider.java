package org.example.formulaone.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.formulaone.exceptions.HttpClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * Fetches raw session data from OpenF1 API without any business logic
     * processing.
     * Returns raw JsonNode array that can be processed by service layer.
     */
    public JsonNode fetchRawSessions(Integer year, String country, String sessionType) {
        if (!enabled) {
            log.debug("OpenF1 provider is disabled. Returning null.");
            return null;
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
                log.warn("Rate limit exceeded for OpenF1 sessions API. Returning null.");
            } else {
                log.warn("OpenF1 sessions fetch failed: {}", ex.getMessage());
            }
            return null;
        } catch (Exception ex) {
            log.error("Unexpected error fetching sessions: ", ex);
            return null;
        }

        JsonNode sessionsArray = (root != null && root.isArray()) ? root : (root != null ? root.path("data") : null);
        if (sessionsArray == null || !sessionsArray.isArray()) {
            log.debug("No sessions array present in provider response");
            return null;
        }

        return sessionsArray;
    }

    /**
     * Fetches raw driver data from OpenF1 API without any business logic processing.
     * Returns raw JsonNode array that can be processed by service layer.
     */
    public JsonNode fetchRawDriversForSession(String sessionKey) {
        if (!enabled) {
            log.debug("OpenF1 provider is disabled. Returning null.");
            return null;
        }

        if (sessionKey == null) {
            return null;
        }

        String url = baseUrl + "/v1/drivers";
        Map<String, String> queryParams = Collections.singletonMap("session_key", sessionKey);

        JsonNode root;
        try {
            root = httpClient.getJson(url, queryParams);
        } catch (HttpClientException ex) {
            if (ex.getMessage().contains("429")) {
                log.warn("Rate limit exceeded for OpenF1 drivers API. Returning null for session: {}",
                        sessionKey);
            } else {
                log.warn("Failed to call OpenF1 drivers for session {}: {}", sessionKey, ex.getMessage());
            }
            return null;
        } catch (Exception ex) {
            log.error("Unexpected error calling OpenF1 drivers for session " + sessionKey, ex);
            return null;
        }

        JsonNode driversArray = (root != null && root.isArray()) ? root : (root != null ? root.path("data") : null);
        if (driversArray == null || !driversArray.isArray()) {
            log.debug("No drivers array in provider response for session {}", sessionKey);
            return null;
        }

        return driversArray;
    }

    @Override
    public String getName() {
        return "openf1";
    }
}