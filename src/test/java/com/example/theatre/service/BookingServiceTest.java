package com.example.theatre.service;

import com.example.theatre.dto.BookingRequest;
import com.example.theatre.entity.Booking;
import com.example.theatre.entity.BookingStatus;
import com.example.theatre.entity.Payment;
import com.example.theatre.entity.PaymentStatus;
import com.example.theatre.entity.Screen;
import com.example.theatre.entity.Seat;
import com.example.theatre.entity.Show;
import com.example.theatre.entity.User;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.exception.InvalidRequestException;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.repository.BookingRepository;
import com.example.theatre.repository.BookingSeatRepository;
import com.example.theatre.repository.SeatRepository;
import com.example.theatre.repository.ShowRepository;
import com.example.theatre.repository.UserRepository;
import com.example.theatre.repository.PaymentRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock private BookingRepository bookings;
    @Mock private BookingSeatRepository bookingSeats;
    @Mock private ShowRepository shows;
    @Mock private SeatRepository seats;
    @Mock private UserRepository users;
    @Mock private PaymentRepository payments;
    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, bookingSeats, shows, seats, users, payments);
    }

    @Test
    void rejectsRepeatedSeatIds() {
        assertThatThrownBy(() -> service.create(1L, new BookingRequest(2L, List.of(3L, 3L))))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsSeatFromAnotherScreen() {
        setupFutureShow();
        Seat seat = seat(5L);
        when(seats.findById(3L)).thenReturn(Optional.of(seat));

        assertThatThrownBy(() -> service.create(1L, new BookingRequest(2L, List.of(3L))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void rejectsAlreadyBookedSeat() {
        setupFutureShow();
        when(seats.findById(3L)).thenReturn(Optional.of(seat(4L)));
        when(bookingSeats.existsByShowIdAndSeatId(2L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, new BookingRequest(2L, List.of(3L))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    void createsBookingWithSeatPriceSnapshot() {
        setupFutureShow();
        Seat seat = seat(4L);
        ReflectionTestUtils.setField(seat, "id", 3L);
        seat.setSeatNumber("1");
        seat.setRowNumber("A");
        when(seats.findById(3L)).thenReturn(Optional.of(seat));
        when(bookings.save(any(Booking.class))).thenAnswer(call -> {
            Booking saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 8L);
            return saved;
        });
        List<com.example.theatre.entity.BookingSeat> savedSeats = new ArrayList<>();
        when(bookingSeats.save(any())).thenAnswer(call -> {
            com.example.theatre.entity.BookingSeat saved = call.getArgument(0);
            savedSeats.add(saved);
            return saved;
        });
        when(bookingSeats.findByBookingId(8L)).thenAnswer(call -> savedSeats);

        var response = service.create(1L, new BookingRequest(2L, List.of(3L)));

        assertThat(response.totalAmount()).isEqualByComparingTo("10.00");
        assertThat(response.seats()).hasSize(1);
        assertThat(response.seats().getFirst().price()).isEqualByComparingTo("10.00");
    }

    @Test
    void hidesAnotherUsersBooking() {
        Booking booking = new Booking();
        User owner = new User();
        ReflectionTestUtils.setField(owner, "id", 9L);
        booking.setUser(owner);
        when(bookings.findById(7L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> service.getById(1L, 7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void availableSeatsExcludeReservedOnes() {
        Show show = new Show();
        Screen screen = new Screen();
        ReflectionTestUtils.setField(screen, "id", 4L);
        show.setScreen(screen);
        when(shows.findById(2L)).thenReturn(Optional.of(show));
        Seat freeSeat = seat(4L);
        ReflectionTestUtils.setField(freeSeat, "id", 3L);
        when(seats.findAvailableForShow(4L, 2L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(freeSeat)));

        var result = service.availableSeats(2L, Pageable.unpaged());

        assertThat(result.getContent()).extracting("id").containsExactly(3L);
    }

    @Test
    void cancellationReleasesSeatsAndClosesPendingPayment() {
        Booking booking = cancellableBooking();
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.PENDING);
        when(bookings.findByIdForPayment(7L)).thenReturn(Optional.of(booking));
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(payment));
        com.example.theatre.entity.BookingSeat reserved = new com.example.theatre.entity.BookingSeat();
        when(bookingSeats.findByBookingId(7L)).thenReturn(List.of(reserved), List.of());

        var result = service.cancel(1L, 7L);

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.seats()).isEmpty();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(bookingSeats).deleteAll(List.of(reserved));
    }

    @Test
    void cancellationRefusesCompletedPayment() {
        Booking booking = cancellableBooking();
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.COMPLETED);
        when(bookings.findByIdForPayment(7L)).thenReturn(Optional.of(booking));
        when(payments.findByBookingId(7L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.cancel(1L, 7L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("refund");
    }

    private Booking cancellableBooking() {
        User owner = new User();
        ReflectionTestUtils.setField(owner, "id", 1L);
        Show show = new Show();
        ReflectionTestUtils.setField(show, "id", 2L);
        show.setShowDate(LocalDate.now().plusDays(1));
        show.setStartTime(LocalTime.NOON);
        Booking booking = new Booking();
        ReflectionTestUtils.setField(booking, "id", 7L);
        booking.setUser(owner);
        booking.setShow(show);
        booking.setTotalAmount(BigDecimal.TEN);
        return booking;
    }

    private void setupFutureShow() {
        User user = new User();
        Show show = new Show();
        ReflectionTestUtils.setField(show, "id", 2L);
        show.setShowDate(LocalDate.now().plusDays(1));
        show.setStartTime(LocalTime.NOON);
        Screen screen = new Screen();
        ReflectionTestUtils.setField(screen, "id", 4L);
        show.setScreen(screen);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(shows.findByIdForBooking(2L)).thenReturn(Optional.of(show));
    }

    private Seat seat(Long screenId) {
        Screen screen = new Screen();
        ReflectionTestUtils.setField(screen, "id", screenId);
        Seat seat = new Seat();
        seat.setScreen(screen);
        seat.setPrice(BigDecimal.TEN);
        return seat;
    }
}
