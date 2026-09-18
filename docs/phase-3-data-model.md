# Phase 3: entities and database schema

## How to read the code

Start with `Movie.java`, then read `Theatre.java`, `Screen.java`, and `Seat.java`.
Finally read `Show.java`, `User.java`, `Booking.java`, `BookingSeat.java`, and `Payment.java`.

Each entity is a normal Java class with fields and getters/setters:

- `@Entity` tells Hibernate to store the class in the database.
- `@Table` specifies the table name.
- `@Id` marks the primary key.
- `@GeneratedValue` lets MySQL generate the numeric ID.
- `@Column` describes a database column.
- `@ManyToOne` and `@JoinColumn` identify the foreign-key relationship.
- `@OneToMany(mappedBy = "...")` reads the other side of that relationship.
- `@OneToOne` connects a booking to its payment.
- `@Enumerated(EnumType.STRING)` stores readable enum names, not numbers.

For example, `Screen.theatre` owns the `theatre_id` foreign key. `Theatre.screens`
is the inverse view of that relationship. Always set `screen.setTheatre(theatre)`
when creating a screen. After reloading a theatre, Hibernate loads its screens
from the foreign key. Merely adding a screen to the list does not save it.

## Relationships

- One theatre has many screens.
- One screen has many seats and shows.
- One movie has many shows.
- One user has many bookings.
- One show has many bookings.
- One booking has many booking-seat reservations.
- One booking has at most one payment. A payment always belongs to one booking.

`BookingSeat` contains a booking, show, seat, and the price at the time of booking.
The unique `(show_id, seat_id)` constraint is the database's last line of defence
against double booking. A service will also validate that the reservation's show
matches the booking and that the seat belongs to the show's screen.

## Simple design decisions

- Money uses `BigDecimal` with two decimal places, never `double`.
- A movie's `duration` is measured in minutes; its optional rating is from 0 to 10.
- `totalScreens` is derived from `Theatre.screens`, not duplicated in the database.
- Screen capacity is the configured maximum; later services will check seat counts.
- Shows currently start and end on the same calendar date. Overnight scheduling
  is deliberately outside this first version.
- Booking-seat rows represent current reservations. Cancellation will remove
  them and retain the booking and payment; detailed cancelled-seat history is
  outside this first version.
- Payment methods and statuses record payment information. They do not process
  real money or connect to a payment gateway.
- Relationships use lazy fetching where practical. JPA may still load an inverse
  optional one-to-one relationship when checking whether a payment exists.
- No cascading deletes are used. Services will perform explicit lifecycle changes
  rather than accidentally deleting bookings when a theatre is removed.
- Entity classes are not API responses. DTOs will prevent password exposure and
  bidirectional JSON serialization problems in later phases.

## Schema changes

`V1__create_theatre_schema.sql` creates the nine tables, foreign keys, indexes,
and basic check constraints. Flyway applies it once and records its checksum.
Hibernate then validates the entity mappings against the actual schema.

Do not edit V1 after it has been applied. Future schema changes belong in a new
versioned migration such as `V2__add_booking_currency.sql`.

## Run the mapping check

From the project folder, with MySQL running and your `.env` ready:

The project uses host port `3307` so it does not conflict with the existing MySQL
server on port `3306`. The container still uses `3306` internally. In `.env`, use
`DB_URL=jdbc:mysql://localhost:3307/theatre_management`.

```bash
set -a
source .env
set +a
mvn -Dtest=EntityMappingIT test
```

Use shell-compatible values in `.env`; quote values containing spaces or shell
special characters. Never commit `.env` or paste its contents into a chat.

The integration test intentionally ends in `IT`, so a normal `mvn test` does not
connect to your local database. Run it explicitly with the command above.
The migration is persistent; all fixture records created by the test are rolled
back when the test finishes. MySQL auto-increment counters may still advance.
