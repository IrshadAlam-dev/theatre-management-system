package com.example.theatre.repository;

import com.example.theatre.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBookingId(Long bookingId);

    List<BookingSeat> findByShowId(Long showId);

    boolean existsByShowIdAndSeatId(Long showId, Long seatId);
}
