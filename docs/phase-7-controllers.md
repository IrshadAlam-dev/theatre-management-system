# Phase 7: REST controllers

The six controllers in `controller/` turn HTTP requests into service calls. They
do not contain database queries or booking logic.

| Resource | Base path | Operations |
| --- | --- | --- |
| Theatres | `/api/theatres` | POST, GET collection/item, PUT item, DELETE item |
| Screens | `/api/screens` | POST, GET collection/item, PUT item, DELETE item |
| Movies | `/api/movies` | POST, GET collection/item, PUT item, DELETE item |
| Shows | `/api/shows` | POST, GET collection/item, PUT item, DELETE item |
| Seats | `/api/seats` | POST, GET collection/item, PUT item, DELETE item |
| Users | `/api/users` | GET collection/item, PUT item, DELETE item |

`POST /api/users` is deliberately absent: customer registration belongs to the
authentication phase, which must encode passwords and assign the CUSTOMER role.
Bookings and payments also need their own business and security rules.

Create returns `201 Created` with a `Location` header; successful delete returns
`204 No Content`. Read/update return `200 OK`. `@Valid` runs the request DTO's
validation annotations before calling its service. Error response formatting is
described in `phase-8-errors.md`.

Collections accept `page`, `size`, and `sort` query parameters. Page numbers start
at zero; the default page size is 10. Their JSON uses `content`, `page`, `size`,
`totalElements`, and `totalPages`, rather than exposing Spring Data internals.
Optional filters are `city` for theatres, `title` or `genre` for movies,
`theatreId` for screens, and `screenId` for seats. For movies, `title` takes
precedence when both `title` and `genre` are supplied.

Spring Security now uses JWT authentication and ADMIN/CUSTOMER rules. See
`phase-9-security.md` for the current access policy and setup instructions.

Run `mvn test` to exercise the controller's create, validation, listing, search,
and delete behavior without connecting to MySQL. The full application still needs
Docker MySQL and environment variables to start.
