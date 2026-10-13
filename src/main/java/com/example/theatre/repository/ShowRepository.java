package com.example.theatre.repository;

import com.example.theatre.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalTime;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("select count(s) from Show s where s.showDate > :date or (s.showDate = :date and s.startTime > :time)")
    long countUpcoming(@Param("date") LocalDate date, @Param("time") LocalTime time);

    // Lock one show while reserving seats so two requests cannot claim the same seat.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Show s where s.id = :id")
    Optional<Show> findByIdForBooking(@Param("id") Long id);

    Page<Show> findByMovieIdAndShowDate(Long movieId, LocalDate showDate, Pageable pageable);

    Page<Show> findByScreenIdAndShowDate(Long screenId, LocalDate showDate, Pageable pageable);

    @Query("""
            select (count(s) > 0) from Show s
            where s.screen.id = :screenId
              and s.showDate = :showDate
              and s.startTime < :endTime
              and s.endTime > :startTime
              and (:excludedShowId is null or s.id <> :excludedShowId)
            """)
    boolean existsOverlappingShow(@Param("screenId") Long screenId,
                                  @Param("showDate") LocalDate showDate,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime,
                                  @Param("excludedShowId") Long excludedShowId);
}
