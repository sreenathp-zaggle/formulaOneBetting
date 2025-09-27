package org.example.formulaone.repository;

import org.example.formulaone.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, String> {
    List<Event> findByEventYearAndCountryAndSessionType(Integer eventYear, String country, String sessionType);
}
