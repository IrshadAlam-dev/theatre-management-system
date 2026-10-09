package com.example.theatre.dto;

import com.example.theatre.entity.SeatType;
import java.math.BigDecimal;

public record SeatAvailabilityResponse(
        Long id,
        String seatNumber,
        String rowNumber,
        SeatType seatType,
        BigDecimal price,
        Long screenId,
        boolean available
) {
}
