package com.example.theatre.service;

import com.example.theatre.dto.MovieRequest;
import com.example.theatre.dto.MovieResponse;
import com.example.theatre.entity.Movie;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.mapper.MovieMapper;
import com.example.theatre.repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MovieService {
    private final MovieRepository repository;
    public MovieService(MovieRepository repository) { this.repository = repository; }
    @Transactional public MovieResponse create(MovieRequest request) { return MovieMapper.toResponse(repository.save(MovieMapper.create(request))); }
    public Page<MovieResponse> getAll(Pageable pageable) { return repository.findAll(pageable).map(MovieMapper::toResponse); }
    public Page<MovieResponse> searchByTitle(String title, Pageable pageable) { return repository.findByTitleContainingIgnoreCase(title, pageable).map(MovieMapper::toResponse); }
    public Page<MovieResponse> searchByGenre(String genre, Pageable pageable) { return repository.findByGenreIgnoreCase(genre, pageable).map(MovieMapper::toResponse); }
    public MovieResponse getById(Long id) { return MovieMapper.toResponse(findMovie(id)); }
    @Transactional public MovieResponse update(Long id, MovieRequest request) { Movie movie = findMovie(id); MovieMapper.update(movie, request); return MovieMapper.toResponse(repository.save(movie)); }
    @Transactional public void delete(Long id) { repository.delete(findMovie(id)); }
    private Movie findMovie(Long id) { return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movie", id)); }
}
