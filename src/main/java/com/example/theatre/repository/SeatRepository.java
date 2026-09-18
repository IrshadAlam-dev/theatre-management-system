package com.example.theatre.repository;

import com.example.theatre.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Query("""
            select s from Seat s where s.screen.id = :screenId
            and not exists (select bs.id from BookingSeat bs
                            where bs.show.id = :showId and bs.seat = s)
            """)
    Page<Seat> findAvailableForShow(@Param("screenId") Long screenId,
                                    @Param("showId") Long showId, Pageable pageable);

    Page<Seat> findByScreenId(Long screenId, Pageable pageable);

    long countByScreenId(Long screenId);

    boolean existsByScreenIdAndRowNumberAndSeatNumber(Long screenId, String rowNumber, String seatNumber);

    boolean existsByScreenIdAndRowNumberAndSeatNumberAndIdNot(
            Long screenId, String rowNumber, String seatNumber, Long id);
}
