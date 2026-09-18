# Phase 10: bookings

An authenticated customer (or admin) can reserve seats for a future show.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/bookings` | Reserve one or more seats |
| GET | `/api/bookings` | List your own bookings |
| GET | `/api/bookings/{id}` | Read one of your own bookings |

All three require `Authorization: Bearer <accessToken>` from `/api/auth/login`.
The user ID comes from the signed token, never the request body. Reading another
user's booking returns 404.

Example request:

```http
POST /api/bookings
Authorization: Bearer <accessToken>
Content-Type: application/json

{"showId": 1, "seatIds": [1, 2]}
```

The service checks that the show has not started, that all selected seats belong
to its screen, and that none is already reserved. It takes a database write lock
on the show while checking and inserting reservations; the unique `(show_id,
seat_id)` constraint is an additional safety net. The booking stores a snapshot
of each seat's price and the total amount. A booking reference is generated on
the server.

Bookings are currently **confirmed reservations, not paid orders**. No payment
is charged by this endpoint. Cancellation and payment handling are not exposed
yet. The schema already supports them for a later phase.

After code changes, stop the current app with Ctrl+C and restart it:

```bash
cd "/Users/irshdalam/Documents/ChatGPT/Theatre Management System - Java"
set -a
source .env
set +a
mvn spring-boot:run
```

Fast tests: `mvn test`. Database-backed integration tests need the local MySQL
container and can be run separately.
