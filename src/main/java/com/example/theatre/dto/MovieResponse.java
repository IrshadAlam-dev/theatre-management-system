package com.example.theatre.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovieResponse(
        Long id,
        String title,
        String description,
        String genre,
        String language,
        Integer duration,
        LocalDate releaseDate,
        BigDecimal rating
) {
}
