package com.example.theatre.controller;

import com.example.theatre.dto.MovieRequest;
import com.example.theatre.dto.MovieResponse;
import com.example.theatre.dto.PageResponse;
import com.example.theatre.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService service;

    public MovieController(MovieService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
        MovieResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/movies/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<MovieResponse> list(@RequestParam(required = false) String title,
                                    @RequestParam(required = false) String genre,
                                    @PageableDefault(size = 10) Pageable pageable) {
        if (title != null && !title.isBlank()) return PageResponse.from(service.searchByTitle(title, pageable));
        if (genre != null && !genre.isBlank()) return PageResponse.from(service.searchByGenre(genre, pageable));
        return PageResponse.from(service.getAll(pageable));
    }

    @GetMapping("/{id}")
    public MovieResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public MovieResponse update(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
