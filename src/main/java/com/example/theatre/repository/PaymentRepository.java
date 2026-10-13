package com.example.theatre.repository;

import com.example.theatre.entity.Payment;
import com.example.theatre.entity.PaymentMethod;
import com.example.theatre.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    long countByStatusAndMethod(PaymentStatus status, PaymentMethod method);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForConfirmation(@Param("id") Long id);

    Optional<Payment> findByBookingId(Long bookingId);
}
