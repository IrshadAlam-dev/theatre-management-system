package com.example.theatre;

import com.example.theatre.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Run explicitly against the local MySQL database. All fixture rows are rolled back.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "app.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Transactional
class EntityMappingIT {

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndLoadsTheEntityRelationships() {
        User user = new User();
        user.setName("Mapping test customer");
        user.setEmail(UUID.randomUUID() + "@example.com");
        user.setPassword("test-only-not-an-authentication-account");
        entityManager.persist(user);

        Theatre theatre = new Theatre();
        theatre.setName("Mapping test theatre");
        theatre.setAddress("123 Test Street");
        theatre.setCity("Chicago");
        entityManager.persist(theatre);

        Screen screen = new Screen();
        screen.setName("Screen 1");
        screen.setCapacity(100);
        screen.setTheatre(theatre);
        entityManager.persist(screen);

        Movie movie = new Movie();
        movie.setTitle("Mapping test movie");
        movie.setGenre("Drama");
        movie.setLanguage("English");
        movie.setDuration(120);
        movie.setReleaseDate(LocalDate.now());
        entityManager.persist(movie);

        Show show = new Show();
        show.setShowDate(LocalDate.now().plusDays(1));
        show.setStartTime(LocalTime.of(18, 0));
        show.setEndTime(LocalTime.of(20, 0));
        show.setMovie(movie);
        show.setScreen(screen);
        entityManager.persist(show);

        Seat seat = new Seat();
        seat.setSeatNumber("1");
        seat.setRowNumber("A");
        seat.setSeatType(SeatType.STANDARD);
        seat.setPrice(new BigDecimal("15.00"));
        seat.setScreen(screen);
        entityManager.persist(seat);

        Booking booking = new Booking();
        booking.setBookingReference("TEST-" + UUID.randomUUID());
        booking.setTotalAmount(seat.getPrice());
        booking.setUser(user);
        booking.setShow(show);
        entityManager.persist(booking);

        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setBooking(booking);
        bookingSeat.setShow(show);
        bookingSeat.setSeat(seat);
        bookingSeat.setPrice(seat.getPrice());
        entityManager.persist(bookingSeat);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setMethod(PaymentMethod.CARD);
        entityManager.persist(payment);

        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Theatre.class, theatre.getId()).getTotalScreens()).isEqualTo(1);
        Screen savedScreen = entityManager.find(Screen.class, screen.getId());
        assertThat(savedScreen.getSeats()).hasSize(1);
        assertThat(savedScreen.getShows()).hasSize(1);
        assertThat(entityManager.find(Movie.class, movie.getId()).getShows()).hasSize(1);
        assertThat(entityManager.find(User.class, user.getId()).getBookings()).hasSize(1);
        assertThat(entityManager.find(Show.class, show.getId()).getBookings()).hasSize(1);

        Booking savedBooking = entityManager.find(Booking.class, booking.getId());
        assertThat(savedBooking.getBookingSeats()).hasSize(1);
        assertThat(savedBooking.getBookingSeats().getFirst().getSeat().getId()).isEqualTo(seat.getId());
        assertThat(savedBooking.getPayment().getId()).isEqualTo(payment.getId());
        assertThat(savedBooking.getPayment().getAmount()).isEqualByComparingTo("15.00");
    }
}
