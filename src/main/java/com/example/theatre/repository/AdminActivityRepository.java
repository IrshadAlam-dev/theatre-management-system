package com.example.theatre.repository;

import com.example.theatre.entity.AdminActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActivityRepository extends JpaRepository<AdminActivity, Long> {
    Page<AdminActivity> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
