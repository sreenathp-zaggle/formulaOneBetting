package org.example.formulaone.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface F1Provider {
    JsonNode fetchRawSessions(Integer year, String country, String sessionType);
    JsonNode fetchRawDriversForSession(String sessionKey);
    String getName();
}
