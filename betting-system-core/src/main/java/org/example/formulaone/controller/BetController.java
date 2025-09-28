package org.example.formulaone.controller;

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
public class BetController {
    private final BettingService bettingService;

    @Autowired
    public BetController(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    @PostMapping
    public ResponseEntity<PlaceBetResponseDto> placeBet(@Valid @RequestBody PlaceBetRequestDto placeBetRequestDto) {

        PlaceBetResponseDto response = bettingService.placeBet(placeBetRequestDto);
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
