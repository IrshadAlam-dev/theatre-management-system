package com.example.theatre.service;

import com.example.theatre.dto.AdminDashboardResponse;
import com.example.theatre.entity.BookingStatus;
import com.example.theatre.entity.PaymentMethod;
import com.example.theatre.entity.PaymentStatus;
import com.example.theatre.repository.BookingRepository;
import com.example.theatre.repository.PaymentRepository;
import com.example.theatre.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final ShowRepository shows;

    public AdminDashboardService(BookingRepository bookings, PaymentRepository payments, ShowRepository shows) {
        this.bookings = bookings;
        this.payments = payments;
        this.shows = shows;
    }

    public AdminDashboardResponse getSummary() {
        LocalDateTime now = LocalDateTime.now();
        return new AdminDashboardResponse(bookings.count(),
                bookings.countByStatus(BookingStatus.CONFIRMED),
                bookings.countByStatus(BookingStatus.CANCELLED),
                payments.countByStatusAndMethod(PaymentStatus.PENDING, PaymentMethod.CASH),
                shows.countUpcoming(now.toLocalDate(), now.toLocalTime()));
    }
}
