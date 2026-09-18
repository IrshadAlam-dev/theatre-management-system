package com.example.theatre.dto;

import jakarta.validation.constraints.*;
import com.example.theatre.entity.SeatType;
import java.math.BigDecimal;

public record SeatRequest(
        @NotBlank @Size(max = 10) String seatNumber,
        @NotBlank @Size(max = 10) String rowNumber,
        @NotNull SeatType seatType,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull @Positive Long screenId
) {
}
