package com.example.theatre.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record ShowRequest(
        @NotNull @FutureOrPresent LocalDate showDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Positive Long movieId,
        @NotNull @Positive Long screenId
) {
}
