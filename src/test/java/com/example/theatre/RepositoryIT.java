package com.example.theatre;

import com.example.theatre.entity.Movie;
import com.example.theatre.repository.MovieRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "app.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Transactional
class RepositoryIT {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createsReadsUpdatesAndDeletesAMovie() {
        Movie movie = new Movie();
        movie.setTitle("Repository test " + UUID.randomUUID());
        movie.setGenre("Drama");
        movie.setLanguage("English");
        movie.setDuration(120);
        movie.setReleaseDate(LocalDate.now());

        Movie savedMovie = movieRepository.saveAndFlush(movie);
        Long movieId = savedMovie.getId();
        entityManager.clear();

        assertThat(movieId).isNotNull();
        assertThat(movieRepository.findById(movieId)).isPresent();
        assertThat(movieRepository.findByTitleContainingIgnoreCase(
                savedMovie.getTitle().toUpperCase(), PageRequest.of(0, 10)).getContent())
                .extracting(Movie::getId).contains(movieId);
        assertThat(movieRepository.findByGenreIgnoreCase("drama", PageRequest.of(0, 10)).getContent())
                .extracting(Movie::getId).contains(movieId);

        savedMovie.setDuration(130);
        movieRepository.saveAndFlush(savedMovie);
        entityManager.clear();
        assertThat(movieRepository.findById(movieId).orElseThrow().getDuration()).isEqualTo(130);

        movieRepository.deleteById(movieId);
        movieRepository.flush();
        assertThat(movieRepository.findById(movieId)).isEmpty();
    }
}
