package org.example.formulaone.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    private UUID id;

    private String name;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public User() {}
    public User(UUID id, BigDecimal balance) {
        this.id = id; this.balance = balance;
    }
}
