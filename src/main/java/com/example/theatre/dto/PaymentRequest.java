package com.example.theatre.dto;

import jakarta.validation.constraints.*;
import com.example.theatre.entity.PaymentMethod;

public record PaymentRequest(
        @NotNull @Positive Long bookingId,
        @NotNull PaymentMethod method
) {
}
