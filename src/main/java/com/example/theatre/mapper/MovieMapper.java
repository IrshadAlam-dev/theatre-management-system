package com.example.theatre.mapper;

import com.example.theatre.dto.MovieRequest;
import com.example.theatre.dto.MovieResponse;
import com.example.theatre.entity.Movie;

public final class MovieMapper {
    private MovieMapper() {}
    public static Movie create(MovieRequest request) { Movie value = new Movie(); update(value, request); return value; }
    public static void update(Movie value, MovieRequest request) { value.setTitle(request.title()); value.setDescription(request.description()); value.setGenre(request.genre()); value.setLanguage(request.language()); value.setDuration(request.duration()); value.setReleaseDate(request.releaseDate()); value.setRating(request.rating()); }
    public static MovieResponse toResponse(Movie value) { return new MovieResponse(value.getId(), value.getTitle(), value.getDescription(), value.getGenre(), value.getLanguage(), value.getDuration(), value.getReleaseDate(), value.getRating()); }
}
