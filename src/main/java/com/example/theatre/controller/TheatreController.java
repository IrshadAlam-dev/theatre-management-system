package com.example.theatre.controller;

import com.example.theatre.dto.TheatreRequest;
import com.example.theatre.dto.TheatreResponse;
import com.example.theatre.dto.PageResponse;
import com.example.theatre.service.TheatreService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/theatres")
public class TheatreController {

    private final TheatreService service;

    public TheatreController(TheatreService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TheatreResponse> create(@Valid @RequestBody TheatreRequest request) {
        TheatreResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/theatres/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<TheatreResponse> list(@RequestParam(required = false) String city,
                                      @PageableDefault(size = 10) Pageable pageable) {
        if (city != null && !city.isBlank()) return PageResponse.from(service.searchByCity(city, pageable));
        return PageResponse.from(service.getAll(pageable));
    }

    @GetMapping("/{id}")
    public TheatreResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public TheatreResponse update(@PathVariable Long id, @Valid @RequestBody TheatreRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
