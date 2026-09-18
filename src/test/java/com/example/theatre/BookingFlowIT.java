package com.example.theatre;

import com.example.theatre.dto.BookingRequest;
import com.example.theatre.dto.PaymentRequest;
import com.example.theatre.entity.*;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.repository.*;
import com.example.theatre.service.BookingService;
import com.example.theatre.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "app.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Transactional
class BookingFlowIT {
    @Autowired private BookingService bookingService;
    @Autowired private PaymentService paymentService;
    @Autowired private UserRepository users;
    @Autowired private TheatreRepository theatres;
    @Autowired private ScreenRepository screens;
    @Autowired private MovieRepository movies;
    @Autowired private ShowRepository shows;
    @Autowired private SeatRepository seats;

    @Test
    void cancellationMakesSeatAvailableForAnotherBooking() {
        Fixture fixture = fixture();
        var booking = bookingService.create(fixture.userId(),
                new BookingRequest(fixture.showId(), List.of(fixture.seatId())));
        assertThat(bookingService.availableSeats(fixture.showId(), Pageable.unpaged()).getContent()).isEmpty();
        var payment = paymentService.create(fixture.userId(),
                new PaymentRequest(booking.id(), PaymentMethod.CASH));

        var cancelled = bookingService.cancel(fixture.userId(), booking.id());

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.seats()).isEmpty();
        assertThat(paymentService.getById(fixture.userId(), payment.id()).status())
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat(bookingService.availableSeats(fixture.showId(), Pageable.unpaged()).getContent())
                .extracting("id").containsExactly(fixture.seatId());
        assertThat(bookingService.create(fixture.userId(),
                new BookingRequest(fixture.showId(), List.of(fixture.seatId()))).id()).isNotNull();
    }

    @Test
    void completedCashBookingCannotBeCancelled() {
        Fixture fixture = fixture();
        var booking = bookingService.create(fixture.userId(),
                new BookingRequest(fixture.showId(), List.of(fixture.seatId())));
        var payment = paymentService.create(fixture.userId(),
                new PaymentRequest(booking.id(), PaymentMethod.CASH));
        paymentService.confirmCash(payment.id());

        assertThatThrownBy(() -> bookingService.cancel(fixture.userId(), booking.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("refund");
    }

    private Fixture fixture() {
        User user = new User();
        user.setName("Booking test");
        user.setEmail("booking-" + UUID.randomUUID() + "@example.com");
        user.setPassword("test-only-hash");
        users.save(user);

        Theatre theatre = new Theatre();
        theatre.setName("Test theatre");
        theatre.setAddress("123 Test Street");
        theatre.setCity("Chicago");
        theatres.save(theatre);

        Screen screen = new Screen();
        screen.setName("Screen 1");
        screen.setCapacity(2);
        screen.setTheatre(theatre);
        screens.save(screen);

        Movie movie = new Movie();
        movie.setTitle("Test movie");
        movie.setGenre("Drama");
        movie.setLanguage("English");
        movie.setDuration(100);
        movie.setReleaseDate(LocalDate.now());
        movies.save(movie);

        Show show = new Show();
        show.setShowDate(LocalDate.now().plusDays(1));
        show.setStartTime(LocalTime.NOON);
        show.setEndTime(LocalTime.of(14, 0));
        show.setMovie(movie);
        show.setScreen(screen);
        shows.save(show);

        Seat seat = new Seat();
        seat.setSeatNumber("1");
        seat.setRowNumber("A");
        seat.setSeatType(SeatType.STANDARD);
        seat.setPrice(new BigDecimal("10.00"));
        seat.setScreen(screen);
        seats.save(seat);
        return new Fixture(user.getId(), show.getId(), seat.getId());
    }

    private record Fixture(Long userId, Long showId, Long seatId) { }
}
