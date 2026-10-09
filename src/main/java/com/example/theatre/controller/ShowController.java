package com.example.theatre.controller;

import com.example.theatre.dto.ShowRequest;
import com.example.theatre.dto.ShowResponse;
import com.example.theatre.dto.PageResponse;
import com.example.theatre.service.ShowService;
import com.example.theatre.service.BookingService;
import com.example.theatre.dto.SeatResponse;
import com.example.theatre.dto.SeatAvailabilityResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService service;
    private final BookingService bookingService;

    public ShowController(ShowService service, BookingService bookingService) {
        this.service = service;
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<ShowResponse> create(@Valid @RequestBody ShowRequest request) {
        ShowResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/shows/" + created.id())).body(created);
    }

    @GetMapping
    public PageResponse<ShowResponse> list(@PageableDefault(size = 10) Pageable pageable) {
        return PageResponse.from(service.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ShowResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/{id}/available-seats")
    public PageResponse<SeatResponse> availableSeats(@PathVariable Long id,
                                                      @PageableDefault(size = 10) Pageable pageable) {
        return PageResponse.from(bookingService.availableSeats(id, pageable));
    }

    @GetMapping("/{id}/seat-map")
    public PageResponse<SeatAvailabilityResponse> seatMap(@PathVariable Long id,
            @PageableDefault(size = 200) Pageable pageable) {
        return PageResponse.from(bookingService.seatMap(id, pageable));
    }

    @PutMapping("/{id}")
    public ShowResponse update(@PathVariable Long id, @Valid @RequestBody ShowRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
