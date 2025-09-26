package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OutcomeResponseDto {
    private UUID eventId;
    private Integer winnerDriverId;
    private int betsSettled;
    private BigDecimal totalPayout;
}
