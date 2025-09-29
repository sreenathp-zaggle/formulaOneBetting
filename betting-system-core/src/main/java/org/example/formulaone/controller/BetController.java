package org.example.formulaone.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.formulaone.util.Constants;
import org.example.formulaone.dto.PlaceBetRequestDto;
import org.example.formulaone.dto.PlaceBetResponseDto;
import org.example.formulaone.service.BettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/bets")
@Slf4j
public class BetController {
    private final BettingService bettingService;

    @Autowired
    public BetController(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    @PostMapping("/place")
    public ResponseEntity<PlaceBetResponseDto> placeBet(@Valid @RequestBody PlaceBetRequestDto placeBetRequestDto) {
        log.info("Received bet placement request for user: {}", placeBetRequestDto.getUserId());

        try {
            PlaceBetResponseDto response = bettingService.placeBet(placeBetRequestDto);

            if (Constants.BET_STATUS_FAILED.equals(response.getStatus())) {
                log.warn("Bet placement failed for user: {}, reason: {}",
                        placeBetRequestDto.getUserId(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Bet placed successfully for user: {}, bet ID: {}",
                    placeBetRequestDto.getUserId(), response.getBetId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid bet request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during bet placement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    private PlaceBetResponseDto createErrorResponse(String message) {
        PlaceBetResponseDto response = new PlaceBetResponseDto();
        response.setStatus(Constants.BET_STATUS_FAILED);
        response.setMessage(message);
        return response;
    }
}