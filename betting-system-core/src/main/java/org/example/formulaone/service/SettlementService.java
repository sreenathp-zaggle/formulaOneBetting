package org.example.formulaone.service;

import lombok.extern.slf4j.Slf4j;
import org.example.formulaone.dto.OutcomeRequestDto;
import org.example.formulaone.dto.OutcomeResponseDto;
import org.example.formulaone.entity.Bet;
import org.example.formulaone.repository.BetRepository;
import org.example.formulaone.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service for settling event outcomes and calculating payouts.
 */
@Service
@Slf4j
public class SettlementService {
    private final EventRepository eventRepository;
    private final BetRepository betRepository;
    private final UserService userService;

    @Autowired
    public SettlementService(EventRepository eventRepository, BetRepository betRepository,
            UserService userService) {
        this.eventRepository = eventRepository;
        this.betRepository = betRepository;
        this.userService = userService;
    }

    /**
     * Settles an event outcome and processes all related bets.
     */
    @Transactional
    public OutcomeResponseDto settleEvent(String eventId, OutcomeRequestDto req) {
        if (eventId == null || req == null || req.getWinnerDriverId() == null) {
            throw new IllegalArgumentException("missing eventId or winnerDriverId");
        }

        // Attempt to set event outcome atomically; returns 0 if outcome already set
        int updated = eventRepository.setOutcomeIfNotSet(eventId, req.getWinnerDriverId());
        if (updated == 0) {
            throw new IllegalStateException("outcome already set");
        }

        List<Bet> bets = betRepository.findByEventIdAndStatus(eventId, "PENDING");
        int settled = 0;
        BigDecimal totalPayout = BigDecimal.ZERO;

        for (Bet bet : bets) {
            bet.setSettledAt(java.time.Instant.now());

            if (req.getWinnerDriverId().equals(bet.getDriverId())) {
                bet.setStatus("WON");
                BigDecimal payout = calculatePayout(bet);
                userService.creditBalance(bet.getUserId(), payout);
                totalPayout = totalPayout.add(payout);
                log.info("Bet {} won, payout: {}", bet.getId(), payout);
            } else {
                bet.setStatus("LOST");
                log.info("Bet {} lost", bet.getId());
            }

            betRepository.save(bet);
            settled++;
        }

        OutcomeResponseDto response = new OutcomeResponseDto();
        response.setEventId(eventId);
        response.setWinnerDriverId(req.getWinnerDriverId());
        response.setBetsSettled(settled);
        response.setTotalPayout(totalPayout);

        log.info("Settled {} bets for event {}, total payout: {}", settled, eventId, totalPayout);
        return response;
    }

    /**
     * Calculates payout for a winning bet.
     */
    private BigDecimal calculatePayout(Bet bet) {
        return bet.getStake().multiply(BigDecimal.valueOf(bet.getOdds()));
    }

}
