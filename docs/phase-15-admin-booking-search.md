# Phase 15: admin booking search

Admins can search bookings by customer name, customer email, booking reference,
or exact numeric booking ID, and can filter by `CONFIRMED` or `CANCELLED` status.
Results are paginated, newest bookings first, and include customer identity,
show, seats, amount, booking status, and payment ID when one exists.

The endpoint is `GET /api/bookings/admin/search`. Optional query parameters are
`query`, `status`, `page`, `size`, and `sort`. It is restricted to the ADMIN
role; the existing customer booking endpoints still return only the signed-in
user's records. The Admin desk loads the latest bookings and offers the same
search and status filters.
