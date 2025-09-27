package org.example.formulaone.service;

import org.example.formulaone.dto.DriverDto;
import org.example.formulaone.dto.ListingEventsResponseDto;
import org.example.formulaone.entity.Event;
import org.example.formulaone.entity.EventDriver;
import org.example.formulaone.repository.EventDriverRepository;
import org.example.formulaone.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EventService {
    private final F1ProviderFactory f1ProviderFactory;
    private final EventRepository eventRepository;
    private final EventDriverRepository eventDriverRepository;

    @Autowired
    public EventService(F1ProviderFactory f1ProviderFactory, EventRepository eventRepository,
            EventDriverRepository eventDriverRepository) {
        this.f1ProviderFactory = f1ProviderFactory;
        this.eventRepository = eventRepository;
        this.eventDriverRepository = eventDriverRepository;
    }

    @Transactional
    public List<ListingEventsResponseDto> listEvents(Integer year, String country, String sessionType,
            String provider) {

        // 1) attempt DB query first
        List<Event> eventsFromDb = eventRepository.findByEventYearAndCountryAndSessionType(year, country, sessionType);

        if (!eventsFromDb.isEmpty()) {

            List<String> eventIds = eventsFromDb.stream()
                    .map(Event::getId)
                    .collect(Collectors.toList());

            List<EventDriver> allDrivers = eventDriverRepository.findByEventIdIn(eventIds);

            Map<String, List<EventDriver>> driversByEvent = allDrivers.stream()
                    .collect(Collectors.groupingBy(EventDriver::getEventId));

            return eventsFromDb.stream()
                    .map(ev -> {
                        ListingEventsResponseDto dto = new ListingEventsResponseDto();
                        dto.setEventId(ev.getId());
                        dto.setName(ev.getName());
                        dto.setCountry(ev.getCountry());
                        dto.setYear(ev.getEventYear());
                        dto.setSessionType(ev.getSessionType());
                        dto.setStartTime(ev.getStartTime());

                        List<EventDriver> eds = driversByEvent.getOrDefault(ev.getId(), Collections.emptyList());
                        List<DriverDto> drivers = eds.stream().map(ed -> {
                            DriverDto dd = new DriverDto();
                            dd.setDriverId(ed.getDriverId()); // Integer or String based on your DTO
                            dd.setFullName(ed.getFullName());
                            dd.setOdds(ed.getOdds());
                            return dd;
                        }).collect(Collectors.toList());

                        dto.setDrivers(drivers);
                        return dto;
                    })
                    .collect(Collectors.toList());
        }

        F1Provider externalProvider = f1ProviderFactory.getProvider(provider);
        List<ListingEventsResponseDto> events = externalProvider.fetchSessions(year, country, sessionType);

        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        // Store events in database for future betting
        for (ListingEventsResponseDto eventDto : events) {
            storeEventIfNotExists(eventDto);
        }

        return events;
    }

    private void storeEventIfNotExists(ListingEventsResponseDto eventDto) {
        String eventId = eventDto.getEventId();
        if (eventId != null && !eventRepository.existsById(eventId)) {
            Event event = new Event();
            event.setId(eventId);
            event.setName(eventDto.getName());
            event.setCountry(eventDto.getCountry());
            event.setEventYear(eventDto.getYear());
            event.setSessionType(eventDto.getSessionType());
            event.setStartTime(eventDto.getStartTime());
            event.setOutcomeDriverId(null); // Not settled yet

            eventRepository.save(event);
            log.debug("Stored new event in database: {} - {}", eventId, eventDto.getName());
        }
    }

    /**
     * Find event in DB. Since events are now stored when calling listEvents,
     * we should only need to find, not create.
     */
    @Transactional
    public Event findEvent(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId +
                        ". Please call /events API first to load events into the database."));
    }

    /**
     * Ensure an EventDriver row exists (persisting driver + odds if required).
     * Returns the DB EventDriver (with odds).
     */
    @Transactional
    public EventDriver findOrCreateEventDriver(String eventId, Integer driverId) {
        return eventDriverRepository.findByEventIdAndDriverId(eventId, driverId)
                .orElseGet(() -> {
                    // fetch from provider if not in DB
                    F1Provider provider = f1ProviderFactory.getProvider(null);
                    List<DriverDto> drivers = provider.fetchDriversForSession(eventId);

                    if (drivers == null || drivers.isEmpty()) {
                        throw new IllegalArgumentException("no drivers returned by provider for event: " + eventId);
                    }

                    // save all drivers for this event
                    List<EventDriver> saved = eventDriverRepository.saveAll(
                            drivers.stream().map(d -> {
                                EventDriver ed = new EventDriver();
                                ed.setId(eventId + ":" + d.getDriverId());
                                ed.setEventId(eventId);
                                ed.setDriverId(d.getDriverId());
                                ed.setFullName(d.getFullName());
                                ed.setOdds(d.getOdds());
                                return ed;
                            }).toList());

                    return saved.stream()
                            .filter(ed -> driverId.equals(ed.getDriverId()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "driver not found for event after provider fetch: " + driverId));

                });
    }
}