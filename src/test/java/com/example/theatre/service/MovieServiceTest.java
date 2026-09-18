package com.example.theatre.service;

import com.example.theatre.dto.MovieRequest;
import com.example.theatre.dto.MovieResponse;
import com.example.theatre.entity.Movie;
import com.example.theatre.exception.ResourceNotFoundException;
import com.example.theatre.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository repository;

    private MovieService service;

    @BeforeEach
    void setUp() {
        service = new MovieService(repository);
    }

    @Test
    void createsMovieFromRequest() {
        when(repository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieResponse response = service.create(movieRequest("Original title", 120));

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Original title");
        assertThat(response.duration()).isEqualTo(120);
    }

    @Test
    void findsExistingMovie() {
        Movie movie = movieEntity("Found movie", 120);
        when(repository.findById(10L)).thenReturn(Optional.of(movie));

        assertThat(service.getById(10L).title()).isEqualTo("Found movie");
    }

    @Test
    void reportsMissingMovie() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Movie with ID 99 was not found");
    }

    @Test
    void updatesExistingMovie() {
        Movie movie = movieEntity("Old title", 100);
        when(repository.findById(10L)).thenReturn(Optional.of(movie));
        when(repository.save(movie)).thenReturn(movie);

        MovieResponse response = service.update(10L, movieRequest("New title", 130));

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.duration()).isEqualTo(130);
    }

    @Test
    void deletesExistingMovie() {
        Movie movie = movieEntity("Delete me", 90);
        when(repository.findById(10L)).thenReturn(Optional.of(movie));

        service.delete(10L);

        verify(repository).delete(movie);
    }

    private MovieRequest movieRequest(String title, int duration) {
        return new MovieRequest(title, "Description", "Drama", "English", duration,
                LocalDate.of(2026, 1, 1), new BigDecimal("8.0"));
    }

    private Movie movieEntity(String title, int duration) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription("Description");
        movie.setGenre("Drama");
        movie.setLanguage("English");
        movie.setDuration(duration);
        movie.setReleaseDate(LocalDate.of(2026, 1, 1));
        movie.setRating(new BigDecimal("8.0"));
        return movie;
    }
}
