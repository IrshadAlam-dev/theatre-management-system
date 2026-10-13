package com.example.theatre.repository;

import com.example.theatre.entity.Booking;
import com.example.theatre.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForPayment(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b join b.payment p where p.id = :paymentId")
    Optional<Booking> findByPaymentIdForConfirmation(@Param("paymentId") Long paymentId);

    Optional<Booking> findByBookingReference(String bookingReference);

    Page<Booking> findByUserId(Long userId, Pageable pageable);
    long countByStatus(BookingStatus status);

    @Query("""
            select b from Booking b
            where (:query = '' or lower(b.bookingReference) like lower(concat('%', :query, '%'))
                   or lower(b.user.name) like lower(concat('%', :query, '%'))
                   or lower(b.user.email) like lower(concat('%', :query, '%'))
                   or (:bookingId is not null and b.id = :bookingId))
              and (:status is null or b.status = :status)
            """)
    Page<Booking> searchForAdmin(@Param("query") String query,
                                 @Param("bookingId") Long bookingId,
                                 @Param("status") BookingStatus status,
                                 Pageable pageable);
}
