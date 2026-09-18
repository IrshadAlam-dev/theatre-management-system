# Phase 6: services and manual mapping

The service layer sits between future controllers and repositories:

```text
Controller -> Service -> Repository -> MySQL
```

Controllers will be responsible for HTTP. Repositories are responsible for data
access. Services contain application decisions and transaction boundaries.

## Services in this phase

- `UserService`: list, read, update, and delete users. Registration and passwords
  belong to the security phase.
- `TheatreService`: CRUD plus city search.
- `ScreenService`: CRUD, filtering by theatre, unique names within a theatre, and
  protection against reducing capacity below the number of existing seats.
- `MovieService`: CRUD plus title and genre searches.
- `ShowService`: CRUD, same-day time validation, and prevention of overlapping
  shows on the same screen.
- `SeatService`: CRUD, filtering by screen, screen-capacity enforcement, and
  prevention of duplicate row/seat positions within a screen.

Booking and payment services are deliberately postponed until their business-logic
phase. They require authentication, transactions, concurrency protection, seat
availability, cancellation, totals, and payment-state rules. Keeping that workflow
together will make it easier to learn and explain.

## Transactions

Each service class defaults to `@Transactional(readOnly = true)` for reads.
Create, update, and delete methods use `@Transactional`. If a write method fails,
Spring rolls back its database changes.

`@Transactional` works because Spring calls the public method through a generated
service proxy. Avoid calling a transactional method from another method in the
same class and assuming a new transaction starts.

## Mappers

There is one small mapper per resource. `create` converts a request into a new
entity, `update` copies permitted fields to an existing entity, and `toResponse`
converts an entity into a response DTO. Mapping is explicit Java code so it is
easy to debug and explain. No reflection or mapping framework is used.

## Rules intentionally deferred

- Foreign-key conflicts during deletion will receive structured `409 Conflict`
  responses in the exception-handling phase.
- DTO validation annotations will be activated by controller `@Valid` parameters.
- Authentication and role checks will be implemented in the security phase.
- Deleting records with historical bookings will remain prohibited by foreign
  keys; no cascading delete hides that behavior.
- Overnight shows are not supported; end time must be after start time on the
  same date.

## Tests

Run the fast unit and DTO tests without MySQL:

```bash
mvn test
```

The service tests mock repositories. They verify movie create/read/update/delete,
missing-resource behavior, overlapping-show rejection, invalid show times, and
screen capacity protection. Existing `IT` tests continue to cover real MySQL.

Maven starts Mockito as a test JVM agent, as Mockito recommends for current JDKs.
This avoids slow and increasingly restricted runtime self-attachment on JDK 26.
The setting is test-only and has no effect on the application at runtime.
