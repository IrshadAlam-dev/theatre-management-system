package com.example.theatre.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record BookingRequest(
        @NotNull @Positive Long showId,
        @NotEmpty List<@NotNull @Positive Long> seatIds
) {
}
