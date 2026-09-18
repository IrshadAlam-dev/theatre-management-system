package com.example.theatre.controller;

import com.example.theatre.dto.MovieRequest;
import com.example.theatre.dto.MovieResponse;
import com.example.theatre.service.MovieService;
import com.example.theatre.exception.BusinessRuleException;
import com.example.theatre.exception.GlobalExceptionHandler;
import com.example.theatre.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MovieControllerTest {

    @Mock private MovieService service;
    private MockMvc mvc;
    private JsonMapper json;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new MovieController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        json = JsonMapper.builder().findAndAddModules().build();
    }

    @Test
    void createsMovieWithLocationAndResponse() throws Exception {
        MovieRequest request = request();
        when(service.create(request)).thenReturn(response());

        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/movies/7"))
                .andExpect(jsonPath("$.title").value("The Sample"));
    }

    @Test
    void rejectsInvalidInputBeforeCallingService() throws Exception {
        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"genre\":\"Drama\",\"language\":\"English\",\"duration\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.duration").exists())
                .andExpect(jsonPath("$.path").value("/api/movies"));

        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    void listsMoviesWithDefaultPageSize() throws Exception {
        when(service.getAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response())));

        mvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7));

        org.mockito.ArgumentCaptor<Pageable> page = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(service).getAll(page.capture());
        org.assertj.core.api.Assertions.assertThat(page.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void routesGenreSearchToService() throws Exception {
        when(service.searchByGenre(eq("Drama"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response())));

        mvc.perform(get("/api/movies").param("genre", "Drama"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].genre").value("Drama"));
    }

    @Test
    void deletesMovieWithNoContent() throws Exception {
        mvc.perform(delete("/api/movies/7"))
                .andExpect(status().isNoContent());

        verify(service).delete(7L);
    }

    @Test
    void returnsNotFoundForMissingMovie() throws Exception {
        when(service.getById(99L)).thenThrow(new ResourceNotFoundException("Movie", 99L));

        mvc.perform(get("/api/movies/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Movie with ID 99 was not found"))
                .andExpect(jsonPath("$.path").value("/api/movies/99"));
    }

    @Test
    void returnsConflictForBusinessRule() throws Exception {
        when(service.create(any(MovieRequest.class))).thenThrow(new BusinessRuleException("Movie already exists"));

        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Movie already exists"));
    }

    @Test
    void returnsConflictWithoutLeakingDatabaseDetails() throws Exception {
        when(service.create(any(MovieRequest.class)))
                .thenThrow(new DataIntegrityViolationException("secret database constraint details"));

        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("The request conflicts with existing data or related records"));
    }

    @Test
    void returnsBadRequestForMalformedJson() throws Exception {
        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Request body is missing or contains invalid JSON"));
    }

    @Test
    void returnsBadRequestForInvalidPathId() throws Exception {
        mvc.perform(get("/api/movies/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void hidesUnexpectedErrorDetails() throws Exception {
        when(service.getById(7L)).thenThrow(new IllegalStateException("internal secret detail"));

        mvc.perform(get("/api/movies/7"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    private MovieRequest request() {
        return new MovieRequest("The Sample", "Description", "Drama", "English", 120,
                LocalDate.of(2026, 1, 1), new BigDecimal("8.5"));
    }

    private MovieResponse response() {
        MovieRequest request = request();
        return new MovieResponse(7L, request.title(), request.description(), request.genre(),
                request.language(), request.duration(), request.releaseDate(), request.rating());
    }
}
