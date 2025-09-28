package org.example.formulaone.service;

import lombok.extern.slf4j.Slf4j;
import org.example.formulaone.dto.OutcomeRequestDto;
import org.example.formulaone.dto.OutcomeResponseDto;
import org.example.formulaone.dto.PlaceBetRequestDto;
import org.example.formulaone.dto.PlaceBetResponseDto;
import org.example.formulaone.entity.Bet;
import org.example.formulaone.entity.Event;
import org.example.formulaone.entity.EventDriver;
import org.example.formulaone.entity.User;
import org.example.formulaone.repository.BetRepository;
import org.example.formulaone.repository.EventDriverRepository;
import org.example.formulaone.repository.EventRepository;
import org.example.formulaone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class BettingService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BetRepository betRepository;
    private final EventService eventService;

    @Autowired
    public BettingService(final UserRepository userRepository, final EventRepository eventRepository,
                          final BetRepository betRepository,
                          final EventService eventService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.betRepository = betRepository;
    }

    @Transactional
    public PlaceBetResponseDto placeBet(@RequestBody PlaceBetRequestDto placeBetRequestDto) {
        if (placeBetRequestDto.getUserId() == null || placeBetRequestDto.getEventId() == null ||
                placeBetRequestDto.getDriverId() == null || placeBetRequestDto.getStake() == null) {
            throw new IllegalArgumentException("missing required fields");
        }

        BigDecimal stake = placeBetRequestDto.getStake();
        if (stake.signum() <= 0)
            throw new IllegalArgumentException("stake must be > 0");

        log.info("Ensure user exists, if not create with gift 100");
        User user = userRepository.findById(placeBetRequestDto.getUserId()).orElseGet(() -> {
            User u = new User(placeBetRequestDto.getUserId(), new BigDecimal("100.00"));
            return userRepository.save(u);
        });

        log.info("Checking if event exists as first listing events should be called first before this");
        Event event = eventService.findEvent(placeBetRequestDto.getEventId());

        log.info("Fetch existing drivers for the event or calling again API to fetch latest drivers for event");
        EventDriver ed = eventService.findOrCreateEventDriver(placeBetRequestDto.getEventId(),
                placeBetRequestDto.getDriverId());

        int updated = userRepository.withdrawIfSufficient(user.getId(), stake);
        if (updated == 0) {
            PlaceBetResponseDto r = new PlaceBetResponseDto();
            r.setBetId(null);
            r.setStatus("FAILED");
            r.setMessage("insufficient_balance");
            r.setOdds(ed.getOdds());
            return r;
        }

        log.info("Saving bet record");

        UUID betId = UUID.randomUUID();
        Bet bet = new Bet();
        bet.setId(betId);
        bet.setUserId(user.getId());
        bet.setEventId(placeBetRequestDto.getEventId());
        bet.setDriverId(placeBetRequestDto.getDriverId());
        bet.setStake(stake);
        bet.setOdds(ed.getOdds());
        bet.setStatus("PENDING");
        bet.setSettledAt(null);
        betRepository.save(bet);

        PlaceBetResponseDto resp = new PlaceBetResponseDto();
        resp.setBetId(betId);
        resp.setStatus(bet.getStatus());
        resp.setOdds(bet.getOdds());
        resp.setMessage("placed Bet");
        return resp;
    }

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

        for (Bet b : bets) {
            b.setSettledAt(java.time.Instant.now());
            if (req.getWinnerDriverId().equals(b.getDriverId())) {
                b.setStatus("WON");

                log.info("Calculate payout stake* odds and credit it to user");
                BigDecimal payout = b.getStake().multiply(BigDecimal.valueOf(b.getOdds()));
                User u = userRepository.findById(b.getUserId()).orElseThrow();
                u.setBalance(u.getBalance().add(payout));
                userRepository.save(u);

                totalPayout = totalPayout.add(payout);
            } else {
                b.setStatus("LOST");
            }
            betRepository.save(b);
            settled++;
        }

        OutcomeResponseDto resp = new OutcomeResponseDto();
        resp.setEventId(eventId);
        resp.setWinnerDriverId(req.getWinnerDriverId());
        resp.setBetsSettled(settled);
        resp.setTotalPayout(totalPayout);
        return resp;
    }
}
