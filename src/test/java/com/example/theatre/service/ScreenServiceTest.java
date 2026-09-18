package com.example.theatre.service;

import com.example.theatre.dto.ScreenRequest;
import com.example.theatre.entity.Screen;
import com.example.theatre.entity.Theatre;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.repository.ScreenRepository;
import com.example.theatre.repository.SeatRepository;
import com.example.theatre.repository.TheatreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScreenServiceTest {

    @Mock private ScreenRepository screenRepository;
    @Mock private TheatreRepository theatreRepository;
    @Mock private SeatRepository seatRepository;
    private ScreenService service;

    @BeforeEach
    void setUp() {
        service = new ScreenService(screenRepository, theatreRepository, seatRepository);
    }

    @Test
    void rejectsCapacityLowerThanExistingSeatCount() {
        Theatre theatre = new Theatre();
        Screen screen = new Screen();
        screen.setTheatre(theatre);
        screen.setCapacity(100);
        when(screenRepository.findById(5L)).thenReturn(Optional.of(screen));
        when(theatreRepository.findById(2L)).thenReturn(Optional.of(theatre));
        when(seatRepository.countByScreenId(5L)).thenReturn(50L);

        ScreenRequest request = new ScreenRequest("Screen 1", 40, 2L);

        assertThatThrownBy(() -> service.update(5L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Screen capacity cannot be lower than its current seat count");
    }
}
