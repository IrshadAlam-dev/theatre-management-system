package com.example.theatre.service;

import com.example.theatre.dto.ShowRequest;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.repository.MovieRepository;
import com.example.theatre.repository.ScreenRepository;
import com.example.theatre.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowServiceTest {

    @Mock private ShowRepository showRepository;
    @Mock private MovieRepository movieRepository;
    @Mock private ScreenRepository screenRepository;
    private ShowService service;

    @BeforeEach
    void setUp() {
        service = new ShowService(showRepository, movieRepository, screenRepository);
    }

    @Test
    void rejectsEndTimeBeforeStartTime() {
        ShowRequest request = request(LocalTime.of(20, 0), LocalTime.of(18, 0));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Show end time must be after start time");
    }

    @Test
    void rejectsOverlappingShowOnSameScreen() {
        ShowRequest request = request(LocalTime.of(18, 0), LocalTime.of(20, 0));
        when(showRepository.existsOverlappingShow(2L, request.showDate(), request.startTime(),
                request.endTime(), null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("The screen already has an overlapping show");
    }

    private ShowRequest request(LocalTime start, LocalTime end) {
        return new ShowRequest(LocalDate.now().plusDays(1), start, end, 1L, 2L);
    }
}
