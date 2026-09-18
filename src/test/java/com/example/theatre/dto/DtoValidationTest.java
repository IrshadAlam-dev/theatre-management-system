package com.example.theatre.dto;

import com.example.theatre.entity.Role;
import com.example.theatre.entity.SeatType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsAValidMovieRequest() {
        MovieRequest request = new MovieRequest("A New Movie", "Description", "Drama", "English",
                120, LocalDate.of(2026, 1, 1), new BigDecimal("8.5"));

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsBlankTitlesInvalidDurationAndOutOfRangeRatings() {
        MovieRequest request = new MovieRequest(" ", null, "Drama", "English",
                0, LocalDate.of(2026, 1, 1), new BigDecimal("10.1"));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("title", "duration", "rating");
    }

    @Test
    void rejectsInvalidRegistrationDetails() {
        RegisterRequest request = new RegisterRequest(" ", "not-an-email", "short", null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("name", "email", "password");
    }

    @Test
    void rejectsMissingAndInvalidBookingSeatIds() {
        assertThat(validator.validate(new BookingRequest(1L, null))).isNotEmpty();
        assertThat(validator.validate(new BookingRequest(1L, java.util.List.of()))).isNotEmpty();
        assertThat(validator.validate(new BookingRequest(1L, Arrays.asList(0L, null)))).hasSize(2);
    }

    @Test
    void rejectsNegativeSeatPricesAndMoreThanTwoDecimalPlaces() {
        SeatRequest negativePrice = new SeatRequest("1", "A", SeatType.STANDARD,
                new BigDecimal("-1.00"), 1L);
        SeatRequest excessivePrecision = new SeatRequest("1", "A", SeatType.STANDARD,
                new BigDecimal("15.001"), 1L);

        assertThat(validator.validate(negativePrice)).isNotEmpty();
        assertThat(validator.validate(excessivePrecision)).isNotEmpty();
    }

    @Test
    void doesNotPrintCredentialsOrTokensFromDtos() {
        assertThat(new RegisterRequest("Alex", "alex@example.com", "sensitive-password", null).toString())
                .doesNotContain("sensitive-password");
        assertThat(new LoginRequest("alex@example.com", "sensitive-password").toString())
                .doesNotContain("sensitive-password");
        assertThat(new AuthResponse("sensitive-token", "Bearer", 3600, null).toString())
                .doesNotContain("sensitive-token");
    }

    @Test
    void serializesUserResponsesWithoutAPassword() {
        UserResponse response = new UserResponse(1L, "Alex", "alex@example.com", null,
                Role.CUSTOMER, LocalDateTime.of(2026, 1, 1, 10, 0));

        String json = JsonMapper.builder().build().writeValueAsString(response);

        assertThat(json).contains("\"email\":\"alex@example.com\"");
        assertThat(json).doesNotContain("password", "bookings");
    }
}
