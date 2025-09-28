package org.example.formulaone.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.formulaone.dto.ListingEventsResponseDto;
import org.example.formulaone.dto.OutcomeRequestDto;
import org.example.formulaone.dto.OutcomeResponseDto;
import org.example.formulaone.service.EventService;
import org.example.formulaone.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST controller for managing Formula 1 events and outcomes.
 * Provides endpoints for listing events and settling event outcomes.
 */
@RestController
@Slf4j
@RequestMapping("/events")
public class EventController {
    private final EventService eventService;
    private final SettlementService settlementService;

    @Autowired
    public EventController(final EventService eventService, final SettlementService settlementService) {
        this.eventService = eventService;
        this.settlementService = settlementService;
    }

    /**
     * Retrieves a list of Formula 1 events based on optional filters.
     *
     * @param year        The year to filter events (optional)
     * @param country     The country to filter events (optional)
     * @param sessionType The type of session to filter (optional)
     * @param provider    The data provider to use (defaults to "openf1")
     * @return List of events matching the criteria
     */
    @GetMapping("/list")
    public ResponseEntity<List<ListingEventsResponseDto>> listEvents(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "sessionType", required = false) String sessionType,
            @RequestParam(value = "provider", defaultValue = "openf1") String provider) {

        List<ListingEventsResponseDto> events = eventService.listEvents(year, country, sessionType, provider);
        return ResponseEntity.ok(events);
    }

    /**
     * Settles the outcome of a specific event.
     *
     * @param eventId        The ID of the event to settle
     * @param outcomeRequest The outcome details
     * @return Response indicating success or failure
     */
    @PostMapping("/{eventId}/outcome")
    public ResponseEntity<?> settleEvent(
            @PathVariable("eventId") String eventId,
            @Valid @RequestBody OutcomeRequestDto outcomeRequest) {
        OutcomeResponseDto response = settlementService.settleEvent(eventId, outcomeRequest);
        return ResponseEntity.ok(response);
    }
}
