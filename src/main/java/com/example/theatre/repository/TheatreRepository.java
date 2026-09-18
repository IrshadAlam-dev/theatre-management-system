package com.example.theatre.repository;

import com.example.theatre.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    Page<Theatre> findByCityContainingIgnoreCase(String city, Pageable pageable);
}
