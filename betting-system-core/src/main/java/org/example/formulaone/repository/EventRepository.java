package org.example.formulaone.repository;

import org.example.formulaone.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {
    /**
     * Finds events with optional filtering parameters.
     * If a parameter is null, that filter is ignored.
     *
     * @param year        The year to filter by (optional)
     * @param country     The country to filter by (optional)
     * @param sessionType The session type to filter by (optional)
     * @return List of events matching the provided filters
     */
    @Query("SELECT e FROM Event e WHERE " +
            "(:year IS NULL OR e.eventYear = :year) AND " +
            "(:country IS NULL OR e.country = :country) AND " +
            "(:sessionType IS NULL OR e.sessionType = :sessionType)")
    List<Event> findEventsWithOptionalFilters(@Param("year") Integer year,
            @Param("country") String country,
            @Param("sessionType") String sessionType);

    /**
     * Atomically set outcome_driver_id if it's not set yet.
     * Returns number of rows updated (1 if success, 0 if already set).
     */
    @Transactional
    @Modifying
    @Query("update Event e set e.outcomeDriverId = :winner where e.id = :eventId and e.outcomeDriverId is null")
    int setOutcomeIfNotSet(@Param("eventId") String eventId, @Param("winner") Integer winner);
}
