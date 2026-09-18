# Phase 4: repositories

A repository is the layer that reads and writes database records. Services will
use these interfaces instead of writing JDBC or SQL for ordinary CRUD operations.

There is one small repository interface for each of the nine entities. All extend
`JpaRepository<Entity, Long>`: the first type is the entity and the second is its ID.

## Methods we get automatically

- `save(entity)` creates or updates a record.
- `findById(id)` finds one record and returns an `Optional`.
- `findAll(pageable)` reads a page of records, with optional sorting.
- `existsById(id)` checks whether a record exists.
- `deleteById(id)` deletes a record, subject to the database's foreign keys.

## Why there are no implementation classes

Spring Data JPA creates the implementation at startup. It also builds queries
from readable method names. For example:

```java
Page<Screen> findByTheatreId(Long theatreId, Pageable pageable);
```

This means: find screens whose theatre has the supplied ID, returning a page.
`Pageable` includes the page number, page size, and sort order. Page numbers start
at zero.

Other methods support finding a user by email, filtering movies by title or genre,
listing shows for a movie/screen and date, reading a customer's bookings, checking
seat reservations, and finding the payment belonging to a booking.

The case-insensitive searches use `IgnoreCase`; partial text searches use
`ContainingIgnoreCase`.

## What repositories do not do

Repositories do not validate booking rules, authorize users, or build API
responses. Those belong in services and controllers. A successful `save` call
does not necessarily mean SQL has executed immediately; Hibernate may wait until
flush or transaction commit. `saveAndFlush` is useful in tests for checking actual
database writes.

## Run and test

From the project folder, with Docker MySQL running on host port 3307:

```bash
set -a
source .env
set +a
mvn -Dtest=EntityMappingIT,RepositoryIT test
```

These explicit integration tests use your local MySQL database. Fixture records
are rolled back. A normal `mvn test` does not run the `IT` classes.

To start the application:

```bash
set -a
source .env
set +a
mvn spring-boot:run
```

Spring Boot does not automatically load `.env`; the shell commands above export
its values for the application. Use shell-compatible values and quote passwords
containing spaces or special characters. Do not share the `.env` contents.

The current scaffold can serve `GET /actuator/health`. CRUD APIs, registration,
login, and custom role-based authorization have not been implemented yet. Spring
Security's default rules may reject other requests; that is expected at this stage.
