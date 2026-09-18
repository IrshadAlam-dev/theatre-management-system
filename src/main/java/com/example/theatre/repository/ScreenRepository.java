package com.example.theatre.repository;

import com.example.theatre.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScreenRepository extends JpaRepository<Screen, Long> {

    Page<Screen> findByTheatreId(Long theatreId, Pageable pageable);

    boolean existsByTheatreIdAndNameIgnoreCase(Long theatreId, String name);

    boolean existsByTheatreIdAndNameIgnoreCaseAndIdNot(Long theatreId, String name, Long id);
}
