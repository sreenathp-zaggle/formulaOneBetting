package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DriverDto {
    private Integer driverId;
    private String fullName;
    private Integer odds;
}
