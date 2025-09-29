package org.example.formulaone.service;

import lombok.extern.slf4j.Slf4j;
import org.example.formulaone.util.Constants;
import org.example.formulaone.dto.PlaceBetRequestDto;
import org.example.formulaone.dto.PlaceBetResponseDto;
import org.example.formulaone.entity.Bet;
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
        eventService.findEvent(placeBetRequestDto.getEventId()); // Validation only

        log.info("Fetch existing drivers for the event or calling again API to fetch latest drivers for event");
        EventDriver ed = eventService.findOrCreateEventDriver(placeBetRequestDto.getEventId(),
                placeBetRequestDto.getDriverId());

        int updated = userService.withdrawIfSufficient(user.getId(), stake);
        if (updated == 0) {
            log.warn("Insufficient balance for user: {}, required: {}", user.getId(), stake);
            PlaceBetResponseDto r = new PlaceBetResponseDto();
            r.setBetId(null);
            r.setStatus(Constants.BET_STATUS_FAILED);
            r.setMessage(Constants.ERROR_INSUFFICIENT_BALANCE);
            r.setOdds(ed.getOdds());
            return r;
        }

        log.info("Creating bet record for user: {}", user.getId());

        UUID betId = UUID.randomUUID();
        Bet bet = new Bet();
        bet.setId(betId);
        bet.setUserId(user.getId());
        bet.setEventId(placeBetRequestDto.getEventId());
        bet.setDriverId(placeBetRequestDto.getDriverId());
        bet.setStake(stake);
        bet.setOdds(ed.getOdds());
        bet.setStatus(Constants.BET_STATUS_PENDING);
        bet.setSettledAt(null);
        betRepository.save(bet);

        log.info("Bet placed successfully with ID: {}", betId);

        PlaceBetResponseDto resp = new PlaceBetResponseDto();
        resp.setBetId(betId);
        resp.setStatus(bet.getStatus());
        resp.setOdds(bet.getOdds());
        resp.setMessage(Constants.SUCCESS_BET_PLACED);
        return resp;
    }
}