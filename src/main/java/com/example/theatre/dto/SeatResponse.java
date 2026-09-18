package com.example.theatre.dto;

import com.example.theatre.entity.SeatType;
import java.math.BigDecimal;

public record SeatResponse(
        Long id,
        String seatNumber,
        String rowNumber,
        SeatType seatType,
        BigDecimal price,
        Long screenId
) {
}
