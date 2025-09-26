package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OutcomeRequestDto {
    private UUID eventId;
    private Integer winnerDriverId;
}
