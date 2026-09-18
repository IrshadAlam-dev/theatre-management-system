package com.example.theatre.service;

import com.example.theatre.dto.TheatreRequest;
import com.example.theatre.dto.TheatreResponse;
import com.example.theatre.entity.Theatre;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.mapper.TheatreMapper;
import com.example.theatre.repository.TheatreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TheatreService {
    private final TheatreRepository repository;
    public TheatreService(TheatreRepository repository) { this.repository = repository; }
    @Transactional public TheatreResponse create(TheatreRequest request) { return TheatreMapper.toResponse(repository.save(TheatreMapper.create(request))); }
    public Page<TheatreResponse> getAll(Pageable pageable) { return repository.findAll(pageable).map(TheatreMapper::toResponse); }
    public Page<TheatreResponse> searchByCity(String city, Pageable pageable) { return repository.findByCityContainingIgnoreCase(city, pageable).map(TheatreMapper::toResponse); }
    public TheatreResponse getById(Long id) { return TheatreMapper.toResponse(findTheatre(id)); }
    @Transactional public TheatreResponse update(Long id, TheatreRequest request) { Theatre value = findTheatre(id); TheatreMapper.update(value, request); return TheatreMapper.toResponse(repository.save(value)); }
    @Transactional public void delete(Long id) { repository.delete(findTheatre(id)); }
    private Theatre findTheatre(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Theatre", id)); }
}
