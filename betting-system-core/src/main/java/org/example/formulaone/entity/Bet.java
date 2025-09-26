package org.example.formulaone.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bets")
@Data
public class Bet {
    @Id
    private UUID id;
    private UUID userId;
    private UUID eventId;
    private Integer driverId;
    private BigDecimal stake;
    private Integer odds;
    private String status;
    private Instant placedAt = Instant.now();
    private Instant settledAt;
}
