package com.example.theatre.controller;

import com.example.theatre.dto.PaymentRequest;
import com.example.theatre.dto.PaymentResponse;
import com.example.theatre.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody PaymentRequest request) {
        PaymentResponse created = service.create(Long.valueOf(jwt.getSubject()), request);
        return ResponseEntity.created(URI.create("/api/payments/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getById(Long.valueOf(jwt.getSubject()), id);
    }

    @PostMapping("/{id}/confirm-cash")
    public PaymentResponse confirmCash(@PathVariable Long id) {
        return service.confirmCash(id);
    }
}
