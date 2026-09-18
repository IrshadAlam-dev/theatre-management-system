package com.example.theatre.dto;

import jakarta.validation.constraints.*;

public record UserUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 30) String phone
) {
}
