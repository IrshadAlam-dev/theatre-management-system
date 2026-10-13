package com.example.theatre.dto;

import java.time.LocalDateTime;

public record AdminActivityResponse(
        Long id,
        Long adminUserId,
        String adminName,
        String action,
        String resource,
        Long resourceId,
        LocalDateTime occurredAt
) {
}
