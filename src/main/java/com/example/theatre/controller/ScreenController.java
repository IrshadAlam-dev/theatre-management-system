package com.example.theatre.controller;

import com.example.theatre.dto.ScreenRequest;
import com.example.theatre.dto.ScreenResponse;
import com.example.theatre.dto.PageResponse;
import com.example.theatre.service.ScreenService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/screens")
public class ScreenController {

    private final ScreenService service;

    public ScreenController(ScreenService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ScreenResponse> create(@Valid @RequestBody ScreenRequest request) {
        ScreenResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/screens/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<ScreenResponse> list(@RequestParam(required = false) Long theatreId,
                                     @PageableDefault(size = 10) Pageable pageable) {
        return PageResponse.from(theatreId == null ? service.getAll(pageable) : service.getByTheatre(theatreId, pageable));
    }

    @GetMapping("/{id}")
    public ScreenResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public ScreenResponse update(@PathVariable Long id, @Valid @RequestBody ScreenRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
