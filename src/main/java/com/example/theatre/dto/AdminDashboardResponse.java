package com.example.theatre.dto;

public record AdminDashboardResponse(
        long totalBookings,
        long confirmedBookings,
        long cancelledBookings,
        long pendingCashPayments,
        long upcomingShows
) {
}
