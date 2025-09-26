package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OutcomeResponseDto {
    private String eventId;
    private Integer winnerDriverId;
    private int betsSettled;
    private BigDecimal totalPayout;
}
