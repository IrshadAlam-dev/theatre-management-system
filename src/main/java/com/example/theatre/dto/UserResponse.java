package com.example.theatre.dto;

import com.example.theatre.entity.Role;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        LocalDateTime createdAt
) {
}
