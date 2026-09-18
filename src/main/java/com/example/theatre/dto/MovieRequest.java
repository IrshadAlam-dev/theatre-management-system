package com.example.theatre.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovieRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 50) String genre,
        @NotBlank @Size(max = 50) String language,
        @NotNull @Positive Integer duration,
        @NotNull LocalDate releaseDate,
        @DecimalMin("0.0") @DecimalMax("10.0") @Digits(integer = 2, fraction = 1) BigDecimal rating
) {
}
