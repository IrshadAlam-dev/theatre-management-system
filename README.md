# Theatre Management System

A full-stack theatre booking project built with Java 26 and Spring Boot. Customers can browse shows, reserve seats, record a pending cash payment, and cancel an unpaid booking. Admins can add catalogue data and confirm cash after receiving it. The browser interface is served by the same Spring Boot application. There is no separate frontend server.

## What you need

- JDK 26
- Maven 3.9+
- Docker Desktop (running)

No Node.js or frontend build tool is required.

## Run it locally

Clone the repository, then open its project folder:

```bash
git clone https://github.com/IrshadAlam-dev/theatre-management-system.git
cd theatre-management-system
```

1. Create your private configuration file:

   ```bash
   cp .env.example .env
   ```

2. Open `.env` and replace the example passwords. Set `DB_PASSWORD` to the same value as `MYSQL_PASSWORD`. Generate a JWT signing key with `openssl rand -base64 32` and put its output in `JWT_SECRET_BASE64`. To create an admin on first startup, uncomment `ADMIN_EMAIL` and `ADMIN_PASSWORD` and use your own values; the admin password must be 12–72 UTF-8 bytes. Do not share or commit `.env`.

3. Start MySQL:

   ```bash
   docker compose up -d --wait
   ```

4. Load the configuration and start Spring Boot:

   ```bash
   set -a
   source .env
   set +a
   mvn spring-boot:run
   ```

5. Open [http://localhost:8080/](http://localhost:8080/) for the UI. The health check is [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

Leave the terminal running while using the app. Press `Ctrl+C` to stop Spring Boot. Later, start it again using step 4; your MySQL data stays in the Docker volume.

## Try the main workflow

1. Sign in as an admin. Add a theatre, then a screen, movie, future show, and seat. The admin page displays IDs to use in the related forms.
2. Sign out and register a customer. Open a show, choose an available seat, and reserve it.
3. Open **My bookings** and record a CASH payment. It starts as **PENDING**—no money moves through this application.
4. After receiving the cash (or when explicitly simulating this in a local demo), sign in as admin. Enter the payment ID, use **Look up** to check it, then confirm. Its status becomes **COMPLETED**, with the confirming admin ID and time recorded.
5. To test cancellation, make a *different unpaid booking*. Cancel it before the show begins; its seat becomes available again. For a completed cash payment, an admin can record a refund after returning the money; future-show seats are released and the booking is cancelled.

The show lineup can be searched by movie, genre, theatre, and date. It refreshes every 30 seconds while the browse page is open. The browser keeps the login token in memory only. Refreshing the page signs you out.

## How the code is organized

```text
src/main/java/com/example/theatre/
  controller/   HTTP endpoints and request validation
  service/      Booking, payment, and other business rules
  repository/   Spring Data JPA database access
  entity/       Database objects and relationships
  dto/          Request and response shapes
  security/     JWT authentication and role rules
  exception/    Consistent API errors
src/main/resources/
  db/migration/ Flyway database changes
  static/       Plain HTML, CSS, and JavaScript frontend
```

The usual request path is **browser → controller → service → repository → MySQL**. Controllers stay thin; the service layer checks rules such as seat availability and booking ownership. Flyway manages the schema, and Hibernate validates the entity mappings.

## Main API endpoints

| Area | Endpoints | Access |
| --- | --- | --- |
| Accounts | `POST /api/auth/register`, `POST /api/auth/login` | Public |
| Catalogue | `GET /api/movies`, `/api/theatres`, `/api/screens`, `/api/shows`, `/api/seats` | Public |
| Available seats | `GET /api/shows/{id}/available-seats` | Public |
| Catalogue changes | `POST`, `PUT`, `DELETE` on catalogue routes | Admin |
| Bookings | `POST /api/bookings`, `GET /api/bookings`, `GET /api/bookings/{id}`, `POST /api/bookings/{id}/cancel` | Signed-in owner |
| Booking search | `GET /api/bookings/admin/search?query=...&status=...` | Admin |
| Cash payments | `POST /api/payments`, `GET /api/payments/{id}` | Signed-in owner |
| Cash confirmation | `POST /api/payments/{id}/confirm-cash` | Admin |
| Cash refund | `POST /api/payments/{id}/refund-cash` | Admin |
| Payment lookup | `GET /api/payments/admin/{id}` | Admin |

Protected requests use `Authorization: Bearer <accessToken>` from login. The browser UI adds this header for you. API errors use one JSON shape; common results are `400` for invalid input, `401` for missing/invalid login, `403` for insufficient role, `404` for missing or other users' records, and `409` for business conflicts.

## Tests

Run the fast unit/controller tests:

```bash
mvn test
```

For the MySQL-backed integration tests, first start Docker and load `.env` as in the setup steps, then run:

```bash
mvn -Dtest='*IT' test
```

Integration tests use rollback for their test data. They check entity mappings, repositories, security, and the booking/cancellation/cash flow. The tests do not use your JWT signing key.

## Scope and safety

This is a learning and portfolio application, not a payment gateway. CASH confirmation and refund are manual admin records, not proof that cash changed hands. The confirming and refunding admin IDs and times are stored for new actions; older completed payments have no confirmation audit details. There is no password-reset flow yet. Use only test data and test credentials locally. Keep `.env` private; `.env.example` contains placeholders and is safe to commit.

For more detail about each implementation phase, see the [docs folder](docs/).
