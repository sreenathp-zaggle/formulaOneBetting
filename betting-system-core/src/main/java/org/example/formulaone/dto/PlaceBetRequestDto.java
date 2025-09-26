package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PlaceBetRequestDto {
    private UUID userId;
    private UUID eventId;
    private Integer driverId;
    private BigDecimal stake;
}
