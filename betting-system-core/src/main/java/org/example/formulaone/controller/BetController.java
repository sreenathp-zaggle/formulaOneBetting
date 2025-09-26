package org.example.formulaone.controller;

import org.example.formulaone.dto.PlaceBetRequestDto;
import org.example.formulaone.dto.PlaceBetResponseDto;
import org.example.formulaone.service.BettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BetController {
    private final BettingService bettingService;

    @Autowired
    public BetController(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    @PostMapping("/place")
    public ResponseEntity<PlaceBetResponseDto> placeBet(@RequestBody PlaceBetRequestDto placeBetRequestDto) {
        try {
            PlaceBetResponseDto responseDto = bettingService.placeBet(placeBetRequestDto);
            if ("FAILED".equals(responseDto.getStatus()))
                return ResponseEntity.badRequest().body(responseDto);
            return ResponseEntity.status(201).body(responseDto);
        } catch (IllegalArgumentException ex) {
            PlaceBetResponseDto err = new PlaceBetResponseDto();
            err.setBetId(null);
            err.setStatus("FAILED");
            err.setMessage(ex.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception ex) {
            PlaceBetResponseDto err = new PlaceBetResponseDto();
            err.setBetId(null);
            err.setStatus("FAILED");
            err.setMessage(ex.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }
}
