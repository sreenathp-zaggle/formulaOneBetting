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

import java.util.List;

@Service
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

    public List<ListingEventsResponseDto> listEvents(Integer year, String country, String sessionType,
            String provider) {
        F1Provider externalProvider = f1ProviderFactory.getProvider(provider);

        return externalProvider.fetchSessions(year, country, sessionType);
    }

    /**
     * Ensure event exists in DB. If not present, try to fetch from default provider
     * and persist.
     */
    @Transactional
    public Event findOrCreateEvent(String eventId) {
        return eventRepository.findById(eventId).orElseGet(() -> {
            F1Provider p = f1ProviderFactory.getProvider(null);
            List<ListingEventsResponseDto> sessions = p.fetchSessions(null, null, null);
            return sessions.stream()
                    .filter(s -> eventId.equals(s.getEventId()))
                    .findFirst()
                    .map(dto -> {
                        Event ev = new Event();
                        ev.setId(dto.getEventId());
                        ev.setName(dto.getName());
                        ev.setCountry(dto.getCountry());
                        ev.setEventYear(dto.getYear());
                        ev.setSessionType(dto.getSessionType());
                        ev.setStartTime(dto.getStartTime());
                        return eventRepository.save(ev);
                    })
                    .orElseThrow(() -> new IllegalArgumentException("event not found: " + eventId));
        });
    }

    /**
     * Ensure an EventDriver row exists (persisting driver + odds if required).
     * Returns the DB EventDriver (with odds).
     */
    @Transactional
    public EventDriver findOrCreateEventDriver(String eventId, Integer driverId) {
        return eventDriverRepository.findByEventIdAndDriverId(eventId, driverId).orElseGet(() -> {
            F1Provider p = f1ProviderFactory.getProvider(null);
            List<DriverDto> drivers = p.fetchDriversForSession(eventId);
            return drivers.stream()
                    .filter(d -> driverId.equals(d.getDriverId()))
                    .findFirst()
                    .map(d -> {
                        EventDriver ed = new EventDriver();
                        ed.setId(eventId + ":" + d.getDriverId().toString());
                        ed.setEventId(eventId);
                        ed.setDriverId(d.getDriverId());
                        ed.setFullName(d.getFullName());
                        ed.setOdds(d.getOdds());
                        return eventDriverRepository.save(ed);
                    })
                    .orElseThrow(() -> new IllegalArgumentException("driver not found for event: " + driverId));
        });
    }
}
