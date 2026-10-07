package com.example.theatre.dto;

public record AdminBookingResponse(
        BookingResponse booking,
        String customerName,
        String customerEmail
) {
}
