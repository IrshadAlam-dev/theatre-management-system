package com.example.theatre.dto;

import com.example.theatre.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        Long id,
        String bookingReference,
        LocalDateTime bookingDate,
        BigDecimal totalAmount,
        BookingStatus status,
        Long userId,
        Long showId,
        List<BookingSeatResponse> seats,
        Long paymentId
) {
}
