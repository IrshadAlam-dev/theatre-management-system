# Phase 12: available seats and cancellation

## Available seats

`GET /api/shows/{showId}/available-seats` is public and paginated. It returns
seats belonging to the show's screen that have no current reservation for that
show. It is a snapshot: another customer can reserve a seat immediately after
the response. The booking transaction still performs the final availability
check.

```bash
curl -s 'http://localhost:8080/api/shows/9/available-seats' | jq
```

## Cancellation

`POST /api/bookings/{bookingId}/cancel` requires the booking owner's bearer
token. It is allowed only before the show starts and while any cash payment is
not completed. The `BOOKING_CANCELLATION_CUTOFF_MINUTES` setting can close
cancellation earlier; its default is `0`, meaning until showtime. The booking remains in history with status `CANCELLED`, but its
active reservation rows are deleted so the seats can be booked again. A pending
cash payment changes to `CANCELLED`. A completed or refunded payment requires
a separate refund process and cannot be cancelled through this endpoint.

```bash
curl -s -X POST 'http://localhost:8080/api/bookings/BOOKING_ID/cancel' \
  -H 'Authorization: Bearer CUSTOMER_TOKEN' | jq
```

Cancelling twice returns a conflict. The cancellation response has an empty
`seats` list because the schema stores current reservations rather than a
historical seat snapshot; the booking's reference and total remain available.

Flyway migration V2 adds the `CANCELLED` payment status. Restart the app after
updating the code so Flyway applies it automatically. The MySQL integration
test is `BookingFlowIT`.
