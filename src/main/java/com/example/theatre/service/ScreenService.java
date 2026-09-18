package com.example.theatre.service;

import com.example.theatre.dto.ScreenRequest;
import com.example.theatre.dto.ScreenResponse;
import com.example.theatre.entity.Screen;
import com.example.theatre.entity.Theatre;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.mapper.ScreenMapper;
import com.example.theatre.repository.ScreenRepository;
import com.example.theatre.repository.SeatRepository;
import com.example.theatre.repository.TheatreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScreenService {
    private final ScreenRepository repository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;
    public ScreenService(ScreenRepository repository, TheatreRepository theatreRepository, SeatRepository seatRepository) { this.repository = repository; this.theatreRepository = theatreRepository; this.seatRepository = seatRepository; }
    @Transactional public ScreenResponse create(ScreenRequest request) {
        Theatre theatre = findTheatre(request.theatreId());
        if (repository.existsByTheatreIdAndNameIgnoreCase(theatre.getId(), request.name())) throw new BusinessRuleException("A screen with this name already exists in the theatre");
        return ScreenMapper.toResponse(repository.save(ScreenMapper.create(request, theatre)));
    }
    public Page<ScreenResponse> getAll(Pageable pageable) { return repository.findAll(pageable).map(ScreenMapper::toResponse); }
    public Page<ScreenResponse> getByTheatre(Long theatreId, Pageable pageable) { findTheatre(theatreId); return repository.findByTheatreId(theatreId, pageable).map(ScreenMapper::toResponse); }
    public ScreenResponse getById(Long id) { return ScreenMapper.toResponse(findScreen(id)); }
    @Transactional public ScreenResponse update(Long id, ScreenRequest request) {
        Screen screen = findScreen(id); Theatre theatre = findTheatre(request.theatreId());
        if (repository.existsByTheatreIdAndNameIgnoreCaseAndIdNot(theatre.getId(), request.name(), id)) throw new BusinessRuleException("A screen with this name already exists in the theatre");
        if (request.capacity() < seatRepository.countByScreenId(id)) throw new BusinessRuleException("Screen capacity cannot be lower than its current seat count");
        ScreenMapper.update(screen, request, theatre); return ScreenMapper.toResponse(repository.save(screen));
    }
    @Transactional public void delete(Long id) { repository.delete(findScreen(id)); }
    private Screen findScreen(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Screen", id)); }
    private Theatre findTheatre(Long id) { return theatreRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Theatre", id)); }
}
