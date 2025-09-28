package org.example.formulaone.service;

import lombok.extern.slf4j.Slf4j;
import org.example.formulaone.dto.PlaceBetRequestDto;
import org.example.formulaone.dto.PlaceBetResponseDto;
import org.example.formulaone.entity.Bet;
import org.example.formulaone.entity.Event;
import org.example.formulaone.entity.EventDriver;
import org.example.formulaone.entity.User;
import org.example.formulaone.repository.BetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class BettingService {
    private final UserService userService;
    private final BetRepository betRepository;
    private final EventService eventService;

    @Autowired
    public BettingService(final UserService userService, final BetRepository betRepository,
            final EventService eventService) {
        this.userService = userService;
        this.betRepository = betRepository;
        this.eventService = eventService;
    }

    @Transactional
    public PlaceBetResponseDto placeBet(PlaceBetRequestDto placeBetRequestDto) {
        BigDecimal stake = placeBetRequestDto.getStake();
        if (stake.signum() <= 0)
            throw new IllegalArgumentException("stake must be > 0");

        log.info("Ensure user exists, if not create with gift 100");
        User user = userService.ensureUserExists(placeBetRequestDto.getUserId());

        log.info("Checking if event exists as first listing events should be called first before this");
        Event event = eventService.findEvent(placeBetRequestDto.getEventId());

        log.info("Fetch existing drivers for the event or calling again API to fetch latest drivers for event");
        EventDriver ed = eventService.findOrCreateEventDriver(placeBetRequestDto.getEventId(),
                placeBetRequestDto.getDriverId());

        int updated = userService.withdrawIfSufficient(user.getId(), stake);
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
}
