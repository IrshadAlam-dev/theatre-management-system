package com.example.theatre.controller;

import com.example.theatre.dto.SeatRequest;
import com.example.theatre.dto.SeatResponse;
import com.example.theatre.dto.PageResponse;
import com.example.theatre.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final SeatService service;

    public SeatController(SeatService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SeatResponse> create(@Valid @RequestBody SeatRequest request) {
        SeatResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/seats/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<SeatResponse> list(@RequestParam(required = false) Long screenId,
                                   @PageableDefault(size = 10) Pageable pageable) {
        return PageResponse.from(screenId == null ? service.getAll(pageable) : service.getByScreen(screenId, pageable));
    }

    @GetMapping("/{id}")
    public SeatResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public SeatResponse update(@PathVariable Long id, @Valid @RequestBody SeatRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
