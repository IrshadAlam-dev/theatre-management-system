package com.example.theatre.service;

import com.example.theatre.dto.BookingRequest;
import com.example.theatre.dto.BookingResponse;
import com.example.theatre.dto.AdminBookingResponse;
import com.example.theatre.dto.BookingSeatResponse;
import com.example.theatre.dto.SeatResponse;
import com.example.theatre.dto.SeatAvailabilityResponse;
import com.example.theatre.entity.Booking;
import com.example.theatre.entity.BookingSeat;
import com.example.theatre.entity.BookingStatus;
import com.example.theatre.entity.Payment;
import com.example.theatre.entity.PaymentStatus;
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
import com.example.theatre.mapper.SeatMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookingService {
    @Value("${app.booking.cancellation-cutoff-minutes:0}")
    private long cancellationCutoffMinutes;

    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final ShowRepository shows;
    private final SeatRepository seats;
    private final UserRepository users;
    private final PaymentRepository payments;

    public BookingService(BookingRepository bookings, BookingSeatRepository bookingSeats,
                          ShowRepository shows, SeatRepository seats, UserRepository users,
                          PaymentRepository payments) {
        this.bookings = bookings;
        this.bookingSeats = bookingSeats;
        this.shows = shows;
        this.seats = seats;
        this.users = users;
        this.payments = payments;
    }

    @Transactional
    public BookingResponse create(Long userId, BookingRequest request) {
        if (new HashSet<>(request.seatIds()).size() != request.seatIds().size()) {
            throw new InvalidRequestException("Seat IDs must not be repeated");
        }
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Show show = shows.findByIdForBooking(request.showId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", request.showId()));
        if (!LocalDateTime.of(show.getShowDate(), show.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot book a show that has already started");
        }

        List<Seat> selectedSeats = request.seatIds().stream().map(seatId -> {
            Seat seat = seats.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat", seatId));
            if (!seat.getScreen().getId().equals(show.getScreen().getId())) {
                throw new InvalidRequestException("Seat " + seatId + " does not belong to this show's screen");
            }
            if (bookingSeats.existsByShowIdAndSeatId(show.getId(), seatId)) {
                throw new BusinessRuleException("Seat " + seatId + " is already booked for this show");
            }
            return seat;
        }).toList();

        Booking booking = new Booking();
        booking.setBookingReference(UUID.randomUUID().toString());
        booking.setUser(user);
        booking.setShow(show);
        booking.setTotalAmount(selectedSeats.stream().map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        bookings.save(booking);

        for (Seat seat : selectedSeats) {
            BookingSeat reserved = new BookingSeat();
            reserved.setBooking(booking);
            reserved.setShow(show);
            reserved.setSeat(seat);
            reserved.setPrice(seat.getPrice());
            bookingSeats.save(reserved);
        }
        return toResponse(booking);
    }

    public BookingResponse getById(Long userId, Long bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        return toResponse(booking);
    }

    public Page<BookingResponse> getMine(Long userId, Pageable pageable) {
        return bookings.findByUserId(userId, pageable).map(this::toResponse);
    }

    public Page<AdminBookingResponse> searchForAdmin(String search, BookingStatus status, Pageable pageable) {
        String query = search == null ? "" : search.trim();
        Long bookingId = null;
        if (!query.isEmpty() && query.chars().allMatch(Character::isDigit)) {
            try { bookingId = Long.valueOf(query); } catch (NumberFormatException ignored) { }
        }
        Long exactId = bookingId;
        return bookings.searchForAdmin(query, exactId, status, pageable)
                .map(booking -> new AdminBookingResponse(toResponse(booking),
                        booking.getUser().getName(), booking.getUser().getEmail()));
    }

    public Page<SeatResponse> availableSeats(Long showId, Pageable pageable) {
        Show show = shows.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        return seats.findAvailableForShow(show.getScreen().getId(), showId, pageable)
                .map(SeatMapper::toResponse);
    }

    public Page<SeatAvailabilityResponse> seatMap(Long showId, Pageable pageable) {
        Show show = shows.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        Set<Long> reservedSeatIds = bookingSeats.findByShowId(showId).stream()
                .map(reservation -> reservation.getSeat().getId())
                .collect(Collectors.toSet());
        return seats.findByScreenId(show.getScreen().getId(), pageable).map(seat ->
                new SeatAvailabilityResponse(seat.getId(), seat.getSeatNumber(), seat.getRowNumber(),
                        seat.getSeatType(), seat.getPrice(), seat.getScreen().getId(),
                        !reservedSeatIds.contains(seat.getId())));
    }

    @Transactional
    public BookingResponse cancel(Long userId, Long bookingId) {
        Booking booking = bookings.findByIdForPayment(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking", bookingId);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Booking is already cancelled");
        }
        LocalDateTime cancellationCutoff = LocalDateTime
                .of(booking.getShow().getShowDate(), booking.getShow().getStartTime())
                .minusMinutes(Math.max(0, cancellationCutoffMinutes));
        if (!LocalDateTime.now().isBefore(cancellationCutoff)) {
            throw new BusinessRuleException("The cancellation cutoff for this show has passed");
        }
        Payment payment = payments.findByBookingId(bookingId).orElse(null);
        if (payment != null && payment.getStatus() != PaymentStatus.PENDING
                && payment.getStatus() != PaymentStatus.FAILED) {
            throw new BusinessRuleException("A completed payment requires a separate refund process");
        }
        if (payment != null && payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.CANCELLED);
        }
        bookingSeats.deleteAll(bookingSeats.findByBookingId(bookingId));
        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        List<BookingSeatResponse> seatResponses = bookingSeats.findByBookingId(booking.getId()).stream()
                .map(reserved -> new BookingSeatResponse(reserved.getSeat().getId(),
                        reserved.getSeat().getSeatNumber(), reserved.getSeat().getRowNumber(),
                        reserved.getSeat().getSeatType(), reserved.getPrice()))
                .toList();
        return new BookingResponse(booking.getId(), booking.getBookingReference(), booking.getBookingDate(),
                booking.getTotalAmount(), booking.getStatus(), booking.getUser().getId(),
                booking.getShow().getId(), seatResponses,
                booking.getPayment() == null ? null : booking.getPayment().getId());
    }
}
