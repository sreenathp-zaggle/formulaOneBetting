package org.example.formulaone.repository;

import org.example.formulaone.entity.Bet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BetRepository extends JpaRepository<Bet, UUID> {
    List<Bet> findByEventIdAndStatus(String eventId, String status);
}
