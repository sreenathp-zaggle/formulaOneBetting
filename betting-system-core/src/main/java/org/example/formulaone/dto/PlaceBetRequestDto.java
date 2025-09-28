package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PlaceBetRequestDto {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotNull(message = "Driver ID is required")
    private Integer driverId;

    @NotNull(message = "Stake is required")
    @DecimalMin(value = "0.01", message = "Stake must be greater than 0")
    private BigDecimal stake;
}
