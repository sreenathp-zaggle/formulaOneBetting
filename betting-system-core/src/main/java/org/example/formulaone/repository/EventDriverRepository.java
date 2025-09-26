package org.example.formulaone.repository;

import org.example.formulaone.entity.EventDriver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventDriverRepository extends JpaRepository<EventDriver, UUID> {
    Optional<EventDriver> findByEventIdAndDriverId(UUID eventId, Integer driverId);
}
