package com.example.theatre.service;

import com.example.theatre.dto.PaymentRequest;
import com.example.theatre.entity.Booking;
import com.example.theatre.entity.BookingStatus;
import com.example.theatre.entity.Payment;
import com.example.theatre.entity.PaymentMethod;
import com.example.theatre.entity.PaymentStatus;
import com.example.theatre.entity.User;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.repository.BookingRepository;
import com.example.theatre.repository.PaymentRepository;
import com.example.theatre.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock private PaymentRepository payments;
    @Mock private BookingRepository bookings;
    @Mock private com.example.theatre.repository.BookingSeatRepository bookingSeats;
    @Mock private UserRepository users;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, bookings, bookingSeats, users);
    }

    @Test
    void createsPendingRecordUsingBookingTotal() {
        when(bookings.findByIdForPayment(3L)).thenReturn(Optional.of(booking(1L)));
        when(payments.save(any(Payment.class))).thenAnswer(call -> {
            Payment payment = call.getArgument(0);
            ReflectionTestUtils.setField(payment, "id", 5L);
            return payment;
        });

        var result = service.create(1L, new PaymentRequest(3L, PaymentMethod.CASH));

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.amount()).isEqualByComparingTo("25.00");
        assertThat(result.transactionReference()).isNull();
    }

    @Test
    void rejectsOtherUsersBooking() {
        when(bookings.findByIdForPayment(3L)).thenReturn(Optional.of(booking(2L)));

        assertThatThrownBy(() -> service.create(1L, new PaymentRequest(3L, PaymentMethod.CASH)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsDuplicatePayment() {
        when(bookings.findByIdForPayment(3L)).thenReturn(Optional.of(booking(1L)));
        when(payments.findByBookingId(3L)).thenReturn(Optional.of(new Payment()));

        assertThatThrownBy(() -> service.create(1L, new PaymentRequest(3L, PaymentMethod.CASH)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void rejectsCancelledBooking() {
        Booking booking = booking(1L);
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookings.findByIdForPayment(3L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.create(1L, new PaymentRequest(3L, PaymentMethod.CASH)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsNonCashMethodForNow() {
        assertThatThrownBy(() -> service.create(1L, new PaymentRequest(3L, PaymentMethod.CARD)))
                .isInstanceOf(com.example.theatre.exception.InvalidRequestException.class);
    }

    @Test
    void adminCanConfirmPendingCashAfterReceivingIt() {
        Payment payment = new Payment();
        payment.setBooking(booking(1L));
        payment.setMethod(PaymentMethod.CASH);
        payment.setAmount(new BigDecimal("25.00"));
        ReflectionTestUtils.setField(payment, "id", 5L);
        when(bookings.findByPaymentIdForConfirmation(5L)).thenReturn(Optional.of(payment.getBooking()));
        when(payments.findByIdForConfirmation(5L)).thenReturn(Optional.of(payment));
        User admin = new User();
        ReflectionTestUtils.setField(admin, "id", 7L);
        when(users.findById(7L)).thenReturn(Optional.of(admin));

        var result = service.confirmCash(5L, 7L);

        assertThat(result.status()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.transactionReference()).startsWith("CASH-");
        assertThat(result.confirmedByUserId()).isEqualTo(7L);
        assertThat(result.confirmedAt()).isNotNull();
    }

    @Test
    void cannotConfirmTheSameCashPaymentTwice() {
        Payment payment = new Payment();
        payment.setBooking(booking(1L));
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.COMPLETED);
        when(bookings.findByPaymentIdForConfirmation(5L)).thenReturn(Optional.of(payment.getBooking()));
        when(payments.findByIdForConfirmation(5L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.confirmCash(5L, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Payment is not pending");
    }

    private Booking booking(Long userId) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", userId);
        Booking booking = new Booking();
        ReflectionTestUtils.setField(booking, "id", 3L);
        booking.setUser(user);
        booking.setTotalAmount(new BigDecimal("25.00"));
        return booking;
    }
}
