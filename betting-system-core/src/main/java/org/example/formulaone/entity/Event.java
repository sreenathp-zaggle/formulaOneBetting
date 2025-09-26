package org.example.formulaone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
public class Event {
    @Id
    private UUID id; // provider id or internal
    private String name;
    private String country;
    @Column(name = "event_year")
    private Integer eventYear;

    @Column(name = "session_type")
    private String sessionType;
    @Column(name = "start_time", columnDefinition = "TIMESTAMP")
    private Instant startTime;
    @Column(name = "outcome_driver_id")
    private Integer outcomeDriverId; // nullable - set when outcome reported
}
