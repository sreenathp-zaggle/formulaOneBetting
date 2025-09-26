package org.example.formulaone.controller;

import org.example.formulaone.dto.ListingEventsResponseDto;
import org.example.formulaone.dto.OutcomeRequestDto;
import org.example.formulaone.dto.OutcomeResponseDto;
import org.example.formulaone.service.BettingService;
import org.example.formulaone.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class EventController {
    private final EventService eventService;
    private final BettingService bettingService;

    @Autowired
    public EventController(final EventService eventService, final BettingService bettingService) {
        this.eventService = eventService;
        this.bettingService = bettingService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<ListingEventsResponseDto>> listEvents(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String sessionType,
            @RequestParam(defaultValue = "openf1") String provider

    ) {
        List<ListingEventsResponseDto> events = eventService.listEvents(year, country, sessionType, provider);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/{eventId}/outcome")
    public ResponseEntity<?> settleEvent(@PathVariable UUID eventId, @RequestBody OutcomeRequestDto req) {
        try {
            OutcomeResponseDto resp = bettingService.settleEvent(eventId, req);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ex.getMessage());
        }
    }
}
