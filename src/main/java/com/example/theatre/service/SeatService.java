package com.example.theatre.service;

import com.example.theatre.dto.SeatRequest;
import com.example.theatre.dto.SeatResponse;
import com.example.theatre.entity.Screen;
import com.example.theatre.entity.Seat;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.mapper.SeatMapper;
import com.example.theatre.repository.ScreenRepository;
import com.example.theatre.repository.SeatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SeatService {
    private final SeatRepository repository;
    private final ScreenRepository screenRepository;
    public SeatService(SeatRepository repository, ScreenRepository screenRepository) { this.repository = repository; this.screenRepository = screenRepository; }
    @Transactional public SeatResponse create(SeatRequest request) {
        Screen screen = findScreen(request.screenId());
        validatePosition(request, null);
        if (repository.countByScreenId(screen.getId()) >= screen.getCapacity()) throw new BusinessRuleException("Screen capacity has been reached");
        return SeatMapper.toResponse(repository.save(SeatMapper.create(request, screen)));
    }
    public Page<SeatResponse> getAll(Pageable pageable) { return repository.findAll(pageable).map(SeatMapper::toResponse); }
    public Page<SeatResponse> getByScreen(Long screenId, Pageable pageable) { findScreen(screenId); return repository.findByScreenId(screenId, pageable).map(SeatMapper::toResponse); }
    public SeatResponse getById(Long id) { return SeatMapper.toResponse(findSeat(id)); }
    @Transactional public SeatResponse update(Long id, SeatRequest request) {
        Seat seat = findSeat(id); Screen target = findScreen(request.screenId()); validatePosition(request, id);
        long targetCount = repository.countByScreenId(target.getId());
        boolean staysOnSameScreen = seat.getScreen().getId().equals(target.getId());
        if (!staysOnSameScreen && targetCount >= target.getCapacity()) throw new BusinessRuleException("Screen capacity has been reached");
        SeatMapper.update(seat, request, target); return SeatMapper.toResponse(repository.save(seat));
    }
    @Transactional public void delete(Long id) { repository.delete(findSeat(id)); }
    private void validatePosition(SeatRequest request, Long excludedId) {
        boolean duplicate = excludedId == null
                ? repository.existsByScreenIdAndRowNumberAndSeatNumber(request.screenId(), request.rowNumber(), request.seatNumber())
                : repository.existsByScreenIdAndRowNumberAndSeatNumberAndIdNot(request.screenId(), request.rowNumber(), request.seatNumber(), excludedId);
        if (duplicate) throw new BusinessRuleException("This seat position already exists in the screen");
    }
    private Seat findSeat(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Seat", id)); }
    private Screen findScreen(Long id) { return screenRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Screen", id)); }
}
