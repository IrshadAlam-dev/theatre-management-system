package com.example.theatre.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ShowResponse(
        Long id,
        LocalDate showDate,
        LocalTime startTime,
        LocalTime endTime,
        Long movieId,
        Long screenId
) {
}
