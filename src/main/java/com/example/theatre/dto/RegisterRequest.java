package com.example.theatre.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 30) String phone
) {

    @Override
    public String toString() {
        return "RegisterRequest[credentials redacted]";
    }
}
