package org.example.formulaone.service;

import org.example.formulaone.dto.DriverDto;
import org.example.formulaone.dto.ListingEventsResponseDto;
import org.example.formulaone.entity.Event;
import org.example.formulaone.entity.EventDriver;
import org.example.formulaone.repository.EventDriverRepository;
import org.example.formulaone.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EventService {
    private final OpenF1Provider openF1Provider;
    private final EventRepository eventRepository;
    private final EventDriverRepository eventDriverRepository;

    @Autowired
    public EventService(OpenF1Provider openF1Provider, EventRepository eventRepository,
            EventDriverRepository eventDriverRepository) {
        this.openF1Provider = openF1Provider;
        this.eventRepository = eventRepository;
        this.eventDriverRepository = eventDriverRepository;
    }

    @Transactional
    public List<ListingEventsResponseDto> listEvents(Integer year, String country, String sessionType,
            String provider) {
        log.info("Fetching events from database first. If no events found, fetching it from openf1 API again");

        List<Event> eventsFromDb = eventRepository.findEventsWithOptionalFilters(year, country, sessionType);

        if (!eventsFromDb.isEmpty()) {
            return convertEventsToResponseDtos(eventsFromDb);
        }

        return fetchAndStoreEventsFromProvider(year, country, sessionType, provider);
    }

    @Transactional
    public Event findEvent(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId +
                        ". Please call /events API first to load events into the database."));
    }

    @Transactional
    public EventDriver findOrCreateEventDriver(String eventId, Integer driverId) {
        // 1) Try to find existing driver for this event
        return eventDriverRepository.findByEventIdAndDriverId(eventId, driverId)
                .orElseGet(() -> {
                    // 2) If there are any drivers already stored for this event, the requested
                    // driver was not one of them.
                    // Do NOT call the external provider in this case — reject the request (client
                    // must use a valid driver id).
                    List<EventDriver> persistedForEvent = eventDriverRepository.findByEventId(eventId);
                    if (!persistedForEvent.isEmpty()) {
                        throw new IllegalArgumentException("Driver " + driverId + " not found for event " + eventId +
                                ". Available drivers: " + persistedForEvent.stream()
                                        .map(EventDriver::getDriverId)
                                        .map(String::valueOf)
                                        .collect(Collectors.joining(", ")));
                    }

                    // 3) No persisted drivers for this event -> cold event: call provider, persist
                    // provider drivers and try to satisfy request
                    return createEventDriverFromProvider(eventId, driverId);
                });
    }

    /**
     * Converts Event entities to ListingEventsResponseDto objects.
     */
    private List<ListingEventsResponseDto> convertEventsToResponseDtos(List<Event> eventsFromDb) {
        List<String> eventIds = eventsFromDb.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        List<EventDriver> allDrivers = eventDriverRepository.findByEventIdIn(eventIds);
        Map<String, List<EventDriver>> driversByEvent = allDrivers.stream()
                .collect(Collectors.groupingBy(EventDriver::getEventId));

        return eventsFromDb.stream()
                .map(event -> convertEventToResponseDto(event, driversByEvent))
                .collect(Collectors.toList());
    }

    /**
     * Converts a single Event to ListingEventsResponseDto.
     */
    private ListingEventsResponseDto convertEventToResponseDto(Event event,
            Map<String, List<EventDriver>> driversByEvent) {
        ListingEventsResponseDto dto = new ListingEventsResponseDto();
        dto.setEventId(event.getId());
        dto.setName(event.getName());
        dto.setCountry(event.getCountry());
        dto.setYear(event.getEventYear());
        dto.setSessionType(event.getSessionType());
        dto.setStartTime(event.getStartTime());

        List<EventDriver> eventDrivers = driversByEvent.getOrDefault(event.getId(), Collections.emptyList());
        List<DriverDto> drivers = eventDrivers.stream()
                .map(this::convertEventDriverToDriverDto)
                .collect(Collectors.toList());
        dto.setDrivers(drivers);

        return dto;
    }

    /**
     * Converts a single EventDriver to DriverDto.
     */
    private DriverDto convertEventDriverToDriverDto(EventDriver eventDriver) {
        DriverDto driverDto = new DriverDto();
        driverDto.setDriverId(eventDriver.getDriverId());
        driverDto.setFullName(eventDriver.getFullName());
        driverDto.setOdds(eventDriver.getOdds());
        return driverDto;
    }

    /**
     * Fetches events from external provider and stores them in database.
     */
    private List<ListingEventsResponseDto> fetchAndStoreEventsFromProvider(Integer year, String country,
            String sessionType, String provider) {
        List<ListingEventsResponseDto> events = openF1Provider.fetchSessions(year, country, sessionType);

        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Save events from external call in our database to reduce API calls.");
        events.forEach(this::storeEventIfNotExists);
        return events;
    }

    /**
     * Stores an event in the database if it doesn't already exist.
     * Also saves the drivers associated with the event.
     */
    private void storeEventIfNotExists(ListingEventsResponseDto eventDto) {
        String eventId = eventDto.getEventId();
        if (eventId != null && !eventRepository.existsById(eventId)) {
            Event event = createEventFromDto(eventDto);
            eventRepository.save(event);
            log.debug("Stored new event in database: {} - {}", eventId, eventDto.getName());

            // Save drivers for this event
            if (eventDto.getDrivers() != null && !eventDto.getDrivers().isEmpty()) {
                saveDriversForEvent(eventId, eventDto.getDrivers());
                log.debug("Stored {} drivers for event: {}", eventDto.getDrivers().size(), eventId);
            }
        }
    }

    /**
     * Creates an Event entity from ListingEventsResponseDto.
     */
    private Event createEventFromDto(ListingEventsResponseDto eventDto) {
        Event event = new Event();
        event.setId(eventDto.getEventId());
        event.setName(eventDto.getName());
        event.setCountry(eventDto.getCountry());
        event.setEventYear(eventDto.getYear());
        event.setSessionType(eventDto.getSessionType());
        event.setStartTime(eventDto.getStartTime());
        event.setOutcomeDriverId(null); // Not settled yet
        return event;
    }

    /**
     * Creates EventDriver from external provider data.
     */
    private EventDriver createEventDriverFromProvider(String eventId, Integer driverId) {
        List<DriverDto> drivers = openF1Provider.fetchDriversForSession(eventId);

        if (drivers == null || drivers.isEmpty()) {
            throw new IllegalArgumentException("No drivers returned by provider for event: " + eventId);
        }

        List<EventDriver> savedDrivers = saveDriversForEvent(eventId, drivers);
        return findDriverById(savedDrivers, driverId);
    }

    /**
     * Saves all drivers for an event to the database.
     */
    @Transactional
    List<EventDriver> saveDriversForEvent(String eventId, List<DriverDto> drivers) {
        if (drivers == null || drivers.isEmpty())
            return Collections.emptyList();

        List<EventDriver> toSave = drivers.stream().map(d -> {
            EventDriver ed = new EventDriver();
            ed.setId(eventId + ":" + d.getDriverId());
            ed.setEventId(eventId);
            ed.setDriverId(d.getDriverId());
            ed.setFullName(d.getFullName());
            ed.setOdds(d.getOdds());
            return ed;
        }).collect(Collectors.toList());

        try {
            return eventDriverRepository.saveAll(toSave);
        } catch (DataIntegrityViolationException ex) {
            // concurrent insert occurred - reload canonical rows from DB
            log.debug("Concurrent insert conflict while saving drivers for event {}: {}. Reloading from DB.",
                    eventId, ex.getMessage());
            return eventDriverRepository.findByEventId(eventId);
        }
    }

    /**
     * Finds a driver by ID from the list of saved drivers.
     */
    private EventDriver findDriverById(List<EventDriver> savedDrivers, Integer driverId) {
        return savedDrivers.stream()
                .filter(driver -> driverId.equals(driver.getDriverId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Driver not found for event after provider fetch: " + driverId));
    }
}