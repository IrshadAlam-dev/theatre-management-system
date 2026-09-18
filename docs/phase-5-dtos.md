# Phase 5: request and response DTOs

DTO stands for Data Transfer Object. It is simply the shape of the data crossing
the API boundary. An entity describes database storage; a DTO describes an API
request or response.

## Reading a Java record

The DTOs are small Java records. For example:

```java
public record ScreenResponse(Long id, String name, Integer capacity, Long theatreId) {
}
```

Java generates the constructor and read-only accessors automatically:

```java
ScreenResponse response = new ScreenResponse(1L, "Screen 1", 100, 2L);
Long theatreId = response.theatreId();
```

An accessor is `name()`, not `getName()`. Records avoid repetitive getters and
setters without introducing Lombok or other code-generation dependencies. They
have no inheritance hierarchy or business logic. A record containing a list does
not automatically make that list immutable.

## Request versus response

- `MovieRequest` contains client input for creating/updating a movie.
- `MovieResponse` adds the server-generated ID to the returned data.
- `ScreenRequest` uses `theatreId`, not a nested Theatre entity.
- `UserResponse` intentionally has no password or booking collection.
- `RegisterRequest` does not accept a role. Registration will create a customer.
- `UserUpdateRequest` cannot change a role or password through a generic update.
- `BookingRequest` accepts only `showId` and `seatIds`. The authenticated user and
  calculated price will come from the service, not from client-provided fields.
- `BookingResponse` includes seat snapshots and an optional payment ID.
- `PaymentRequest` accepts only a booking ID and payment method. It does not
  accept client-chosen amounts, successful payment statuses, or transaction IDs.
- `AuthResponse.expiresIn` is the token lifetime in seconds.

Credential and token DTOs override `toString()` so accidental object logging
does not print passwords or access tokens. This does not make logging raw HTTP
bodies safe: never log registration/login bodies or authentication headers.

## Validation annotations

- `@NotBlank`: required, non-whitespace text.
- `@NotNull`: a value must be supplied.
- `@Email`: email format.
- `@Size`: maximum text length or required list length.
- `@Positive`: numeric value must be greater than zero.
- `@Digits`: supported monetary precision.
- `@DecimalMin` and `@DecimalMax`: rating range.
- `@FutureOrPresent`: shows cannot use a past date.

For a booking, `List<@NotNull @Positive Long>` also validates every seat ID.
Duplicate seat IDs, same-day show times, screen capacity, show overlap, booking
ownership, availability, and payment lifecycle rules need service logic; these
annotations cannot check the database.

The password character limit is only a first validation step. BCrypt has a
72-byte input limit, so the authentication service must also check the UTF-8 byte
length before encoding. Character count alone is insufficient for Unicode text.

Annotations are checked in isolated tests now. Controllers will later use `@Valid`
to enforce them on incoming HTTP requests, with centralized error responses.
Adding annotations alone does not yet create or secure any API endpoints.

## Example movie input

```json
{
  "title": "A New Movie",
  "description": "A short description",
  "genre": "Drama",
  "language": "English",
  "duration": 120,
  "releaseDate": "2026-01-01",
  "rating": 8.5
}
```

`duration` is measured in minutes. Movie release dates may be historical or in the
future. Ratings are optional, range from 0 to 10, and have at most one decimal.

## Test command

```bash
mvn test
```

The DTO tests do not need MySQL or `.env`. To also run the existing local database
integration tests, export `.env` first, then use:

```bash
mvn -Dtest=DtoValidationTest,EntityMappingIT,RepositoryIT test
```

Manual entity-to-DTO mapping will be added when the services consume these DTOs.
No mapping framework or generic response wrapper is needed.
