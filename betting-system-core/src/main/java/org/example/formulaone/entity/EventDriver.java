package org.example.formulaone.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "event_drivers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"eventId", "driverId"})
})
@Data
public class EventDriver {
    // composite key
    @Id
    private UUID id;
    private UUID eventId;
    private Integer driverId;
    private String fullName;
    private Integer odds;
}
