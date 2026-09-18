# Phase 8: validation and exception responses

`GlobalExceptionHandler` is annotated with `@RestControllerAdvice`, so it handles
exceptions from every REST controller in one place. Controllers stay small and do
not need repeated `try/catch` blocks.

The response shape is the `ApiError` record:

```json
{
  "timestamp": "2026-09-16T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/movies",
  "validationErrors": {
    "title": "must not be blank",
    "duration": "must be greater than 0"
  }
}
```

`validationErrors` is an empty object for errors that are not field-validation
errors. The timestamp comes from the server's local clock; clients should not use
it as a globally ordered event ID.

| Situation | Status |
| --- | --- |
| Missing entity | 404 Not Found |
| DTO validation, malformed JSON, invalid path/query type | 400 Bad Request |
| Business rule or database constraint conflict | 409 Conflict |
| Unexpected server failure | 500 Internal Server Error |

The handler does not return raw malformed JSON, submitted values, SQL statements,
stack traces, or exception causes to clients. Unexpected failures are logged on
the server and receive a generic client message.

An invalid request triggers DTO validation only where a controller uses `@Valid`.
Some multi-record rules (show overlap, seat capacity, booking availability) still
belong in services and are not expressible as simple field annotations. Database
constraints remain a final safeguard.

Authentication failures (401) and authorization failures (403) are handled by
Spring Security's filter chain, not by this MVC advice. Phase 9 configured those
responses to use the same JSON shape.

Run the fast tests with `mvn test`. The controller tests cover validation, missing
records, conflicts, malformed JSON, invalid IDs, and safe 500 responses. Database
integration tests can be run separately with the commands in the earlier guides.
