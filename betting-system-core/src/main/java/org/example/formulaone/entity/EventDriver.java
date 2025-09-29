package org.example.formulaone.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(name = "event_drivers", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "eventId", "driverId" })
})
@Data
public class EventDriver {
    // composite key
    @Id
    private String id;
    private String eventId;
    private Integer driverId;
    private String fullName;
    private Integer odds;
}
