package com.example.theatre.service;

import com.example.theatre.dto.PaymentRequest;
import com.example.theatre.dto.PaymentResponse;
import com.example.theatre.entity.Booking;
import com.example.theatre.entity.BookingStatus;
import com.example.theatre.entity.Payment;
import com.example.theatre.entity.PaymentMethod;
import com.example.theatre.entity.PaymentStatus;
import com.example.theatre.entity.User;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.exception.InvalidRequestException;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.repository.BookingRepository;
import com.example.theatre.repository.BookingSeatRepository;
import com.example.theatre.repository.PaymentRepository;
import com.example.theatre.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final UserRepository users;

    public PaymentService(PaymentRepository payments, BookingRepository bookings,
                          BookingSeatRepository bookingSeats, UserRepository users) {
        this.payments = payments;
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.users = users;
    }

    @Transactional
    public PaymentResponse create(Long userId, PaymentRequest request) {
        if (request.method() != PaymentMethod.CASH) {
            throw new InvalidRequestException("Only CASH payments are supported for now");
        }
        // Lock the booking so concurrent requests cannot create two payments for it.
        Booking booking = bookings.findByIdForPayment(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", request.bookingId()));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", request.bookingId());
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Cannot start payment for a cancelled booking");
        }
        if (payments.findByBookingId(booking.getId()).isPresent()) {
            throw new BusinessRuleException("A payment already exists for this booking");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setMethod(request.method());
        return toResponse(payments.save(payment));
    }

    @Transactional
    public PaymentResponse confirmCash(Long paymentId, Long adminId) {
        // Cancellation also locks the booking first, so the two operations cannot race.
        bookings.findByPaymentIdForConfirmation(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        Payment payment = payments.findByIdForConfirmation(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (payment.getMethod() != PaymentMethod.CASH) {
            throw new BusinessRuleException("Only cash payments can be confirmed here");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessRuleException("Payment is not pending");
        }
        if (payment.getBooking().getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Cannot confirm payment for a cancelled booking");
        }
        User admin = users.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminId));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionReference("CASH-" + UUID.randomUUID());
        payment.setConfirmedBy(admin);
        payment.setConfirmedAt(LocalDateTime.now());
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse refundCash(Long paymentId, Long adminId) {
        // Keep the same booking-then-payment lock order used by confirmation and cancellation.
        bookings.findByPaymentIdForConfirmation(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        Payment payment = payments.findByIdForConfirmation(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (payment.getMethod() != PaymentMethod.CASH) {
            throw new BusinessRuleException("Only cash payments can be refunded here");
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessRuleException("Only completed payments can be refunded");
        }
        User admin = users.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", adminId));
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedBy(admin);
        payment.setRefundedAt(LocalDateTime.now());
        Booking booking = payment.getBooking();
        if (booking.getStatus() == BookingStatus.CONFIRMED
                && LocalDateTime.of(booking.getShow().getShowDate(), booking.getShow().getStartTime())
                .isAfter(LocalDateTime.now())) {
            bookingSeats.deleteAll(bookingSeats.findByBookingId(booking.getId()));
            booking.setStatus(BookingStatus.CANCELLED);
        }
        return toResponse(payment);
    }

    public PaymentResponse getForAdmin(Long paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        return toResponse(payment);
    }

    public PaymentResponse getById(Long userId, Long paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        if (!payment.getBooking().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Payment", paymentId);
        }
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getBooking().getId(), payment.getAmount(),
                payment.getStatus(), payment.getMethod(), payment.getTransactionReference(),
                payment.getCreatedAt(), payment.getConfirmedBy() == null ? null : payment.getConfirmedBy().getId(),
                payment.getConfirmedAt(), payment.getRefundedBy() == null ? null : payment.getRefundedBy().getId(),
                payment.getRefundedAt());
    }
}
