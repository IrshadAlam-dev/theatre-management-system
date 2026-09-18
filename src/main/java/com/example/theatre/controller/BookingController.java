package com.example.theatre.controller;

import com.example.theatre.dto.BookingRequest;
import com.example.theatre.dto.BookingResponse;
import com.example.theatre.dto.PageResponse;
import com.example.theatre.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody BookingRequest request) {
        BookingResponse created = service.create(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity.created(URI.create("/api/bookings/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<BookingResponse> mine(@AuthenticationPrincipal Jwt jwt,
                                               @PageableDefault(size = 10) Pageable pageable) {
        return PageResponse.from(service.getMine(Long.valueOf(jwt.getSubject()), pageable));
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getById(Long.valueOf(jwt.getSubject()), id);
    }

    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancel(Long.valueOf(jwt.getSubject()), id);
    }
}
