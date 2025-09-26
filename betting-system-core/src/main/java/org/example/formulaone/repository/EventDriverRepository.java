package org.example.formulaone.repository;

import org.example.formulaone.entity.EventDriver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventDriverRepository extends JpaRepository<EventDriver, String> {
    Optional<EventDriver> findByEventIdAndDriverId(String eventId, Integer driverId);
}
