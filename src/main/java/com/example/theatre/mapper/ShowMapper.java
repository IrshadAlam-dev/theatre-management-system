package com.example.theatre.mapper;

import com.example.theatre.dto.ShowRequest;
import com.example.theatre.dto.ShowResponse;
import com.example.theatre.entity.Movie;
import com.example.theatre.entity.Screen;
import com.example.theatre.entity.Show;

public final class ShowMapper {
    private ShowMapper() {}
    public static Show create(ShowRequest request, Movie movie, Screen screen) { Show value = new Show(); update(value, request, movie, screen); return value; }
    public static void update(Show value, ShowRequest request, Movie movie, Screen screen) { value.setShowDate(request.showDate()); value.setStartTime(request.startTime()); value.setEndTime(request.endTime()); value.setMovie(movie); value.setScreen(screen); }
    public static ShowResponse toResponse(Show value) { return new ShowResponse(value.getId(), value.getShowDate(), value.getStartTime(), value.getEndTime(), value.getMovie().getId(), value.getScreen().getId()); }
}
