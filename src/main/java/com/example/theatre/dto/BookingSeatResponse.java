package com.example.theatre.dto;

import com.example.theatre.entity.SeatType;
import java.math.BigDecimal;

public record BookingSeatResponse(
        Long seatId,
        String seatNumber,
        String rowNumber,
        SeatType seatType,
        BigDecimal price
) {
}
