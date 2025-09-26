package org.example.formulaone.service;

import org.example.formulaone.dto.DriverDto;
import org.example.formulaone.dto.ListingEventsResponseDto;

import java.util.List;

public interface F1Provider {
    List<ListingEventsResponseDto> fetchSessions(Integer year, String country, String sessionType);

    List<DriverDto> fetchDriversForSession(String sessionKey);

    String getName();
}
