package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PlaceBetRequestDto {
    @NotNull
    private UUID userId;
    @NotBlank
    private String eventId;
    @NotNull
    private Integer driverId;
    private BigDecimal stake;
}
