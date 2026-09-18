package com.example.theatre.dto;

import com.example.theatre.entity.PaymentMethod;
import com.example.theatre.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long bookingId,
        BigDecimal amount,
        PaymentStatus status,
        PaymentMethod method,
        String transactionReference,
        LocalDateTime createdAt,
        Long confirmedByUserId,
        LocalDateTime confirmedAt
) {
}
