package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PlaceBetResponseDto {
    private UUID betId;
    private String status;
    private Integer odds;
    private String message;
}
