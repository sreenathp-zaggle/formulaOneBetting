package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OutcomeRequestDto {
    @NotNull(message = "Winner driver ID is required")
    private Integer winnerDriverId;
}
