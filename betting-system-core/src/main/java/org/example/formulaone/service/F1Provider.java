package org.example.formulaone.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface F1Provider {
    JsonNode fetchRawSessions(Integer year, String country, String sessionType);
    JsonNode fetchRawDriversForSession(String sessionKey);
    String getName();
}
