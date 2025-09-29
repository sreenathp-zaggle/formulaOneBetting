package org.example.formulaone.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.formulaone.dto.DriverDto;
import org.example.formulaone.dto.ListingEventsResponseDto;
import org.example.formulaone.entity.Event;
import org.example.formulaone.entity.EventDriver;
import org.example.formulaone.repository.EventDriverRepository;
import org.example.formulaone.repository.EventRepository;
import org.example.formulaone.util.RandomOdds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EventService {
    private final F1ProviderFactory providerFactory;
    private final EventRepository eventRepository;
    private final EventDriverRepository eventDriverRepository;

    @Autowired
    public EventService(F1ProviderFactory providerFactory, EventRepository eventRepository,
            EventDriverRepository eventDriverRepository) {
        this.providerFactory = providerFactory;
        this.eventRepository = eventRepository;
        this.eventDriverRepository = eventDriverRepository;
    }

    /**
     * Gets the appropriate F1 provider based on the provider name.
     */
    private F1Provider getProvider(String providerName) {
        return providerFactory.getProvider(providerName);
    }

    @Transactional
    public List<ListingEventsResponseDto> listEvents(Integer year, String country, String sessionType,
            String provider) {
        log.info("Fetching events from database first. If no events found, fetching it from openf1 API again");

        List<Event> eventsFromDb = eventRepository.findEventsWithOptionalFilters(year, country, sessionType);

        if (!eventsFromDb.isEmpty()) {
            return convertEventsToListingResponseDtos(eventsFromDb);
        }

        return fetchAndStoreEventsFromProvider(year, country, sessionType, provider);
    }

    public Event findEvent(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId +
                        ". Please call /events API first to load events into the database."));
    }

    @Transactional
    public EventDriver findOrCreateEventDriver(String eventId, Integer driverId) {
        // 1) Try to find existing driver for this event
        Optional<EventDriver> existingDriver = eventDriverRepository.findByEventIdAndDriverId(eventId, driverId);
        if (existingDriver.isPresent()) {
            return existingDriver.get();
        }

        // 2) Check if event has any drivers already stored
        List<EventDriver> existingDrivers = eventDriverRepository.findByEventId(eventId);
        if (!existingDrivers.isEmpty()) {
            // Event has drivers but requested driver is not one of them
            String availableDriverIds = existingDrivers.stream()
                    .map(EventDriver::getDriverId)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    String.format("Driver %d not found for event %s. Available drivers: %s",
                            driverId, eventId, availableDriverIds));
        }

        // 3) No drivers for this event -> fetch from provider and create
        return createEventDriverFromProvider(eventId, driverId);
    }

    /**
     * Converts Event entities to ListingEventsResponseDto objects.
     */
    private List<ListingEventsResponseDto> convertEventsToListingResponseDtos(List<Event> eventsFromDb) {
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
        F1Provider f1Provider = getProvider(provider);
        JsonNode sessionsArray = f1Provider.fetchRawSessions(year, country, sessionType);

        if (sessionsArray == null || !sessionsArray.isArray()) {
            return Collections.emptyList();
        }

        List<ListingEventsResponseDto> events = new ArrayList<>();
        for (JsonNode sessionNode : sessionsArray) {
            try {
                ListingEventsResponseDto eventDto = buildResponseForListAPI(sessionNode);
                events.add(eventDto);
            } catch (Exception ex) {
                log.warn("Skipped a session due to mapping error: {}", ex.getMessage());
            }
        }

        if (events.isEmpty()) {
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
        List<DriverDto> drivers = buildDriversForSession(eventId);

        if (drivers.isEmpty()) {
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

    /**
     * Builds a ListingEventsResponseDto from a session JSON node.
     * This method handles the business logic of converting raw API data to frontend
     * response format.
     */
    private ListingEventsResponseDto buildResponseForListAPI(JsonNode sessionNode) {
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
        List<DriverDto> drivers = buildDriversForSession(sessionKey);
        eventDto.setDrivers(drivers);

        return eventDto;
    }

    /**
     * Builds drivers for a session by fetching raw data and converting to
     * DriverDto.
     * This method handles the business logic of converting raw API data to frontend
     * response format.
     */
    private List<DriverDto> buildDriversForSession(String sessionKey) {
        F1Provider f1Provider = getProvider("openf1");
        JsonNode driversArray = f1Provider.fetchRawDriversForSession(sessionKey);

        if (driversArray == null || !driversArray.isArray()) {
            return Collections.emptyList();
        }

        List<DriverDto> drivers = new ArrayList<>();
        for (JsonNode driverNode : driversArray) {
            try {
                DriverDto driverDto = buildDriverFromRawData(driverNode);
                drivers.add(driverDto);
            } catch (Exception ex) {
                log.warn("Skipping driver mapping due to error: {}", ex.getMessage());
            }
        }

        return drivers;
    }

    /**
     * Builds a DriverDto from raw driver JSON node.
     */
    private DriverDto buildDriverFromRawData(JsonNode driverNode) {
        String driverNumber = textOrNull(driverNode, "driver_number");
        DriverDto driverDto = new DriverDto();
        driverDto.setDriverId(Integer.parseInt(driverNumber));
        driverDto.setFullName(textOrNull(driverNode, "full_name"));
        driverDto.setOdds(RandomOdds.pick());

        return driverDto;
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