package org.example.formulaone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ListingEventsResponseDto {
    private String eventId;
    private String name;
    private String country;
    private Integer year;
    private String sessionType;
    private Instant startTime;
    private List<DriverDto> drivers;
}
