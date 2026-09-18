package com.example.theatre.service;

import com.example.theatre.dto.ShowRequest;
import com.example.theatre.dto.ShowResponse;
import com.example.theatre.entity.Movie;
import com.example.theatre.entity.Screen;
import com.example.theatre.entity.Show;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.mapper.ShowMapper;
import com.example.theatre.repository.MovieRepository;
import com.example.theatre.repository.ScreenRepository;
import com.example.theatre.repository.ShowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShowService {
    private final ShowRepository repository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    public ShowService(ShowRepository repository, MovieRepository movieRepository, ScreenRepository screenRepository) { this.repository = repository; this.movieRepository = movieRepository; this.screenRepository = screenRepository; }
    @Transactional public ShowResponse create(ShowRequest request) { validate(request, null); return ShowMapper.toResponse(repository.save(ShowMapper.create(request, findMovie(request.movieId()), findScreen(request.screenId())))); }
    public Page<ShowResponse> getAll(Pageable pageable) { return repository.findAll(pageable).map(ShowMapper::toResponse); }
    public ShowResponse getById(Long id) { return ShowMapper.toResponse(findShow(id)); }
    @Transactional public ShowResponse update(Long id, ShowRequest request) { Show show = findShow(id); validate(request, id); ShowMapper.update(show, request, findMovie(request.movieId()), findScreen(request.screenId())); return ShowMapper.toResponse(repository.save(show)); }
    @Transactional public void delete(Long id) { repository.delete(findShow(id)); }
    private void validate(ShowRequest request, Long excludedId) {
        if (!request.endTime().isAfter(request.startTime())) throw new BusinessRuleException("Show end time must be after start time");
        if (repository.existsOverlappingShow(request.screenId(), request.showDate(), request.startTime(), request.endTime(), excludedId)) throw new BusinessRuleException("The screen already has an overlapping show");
    }
    private Show findShow(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show", id)); }
    private Movie findMovie(Long id) { return movieRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movie", id)); }
    private Screen findScreen(Long id) { return screenRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Screen", id)); }
}
