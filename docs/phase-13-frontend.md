# Phase 13: simple browser frontend

The frontend is served by Spring Boot at `http://localhost:8080/`. It uses
plain HTML, CSS, and JavaScript in `src/main/resources/static`; there is no
Node build step or separate frontend server.

## Run

Start Docker/MySQL, then run from the project folder:

```bash
cd theatre-management-system
docker compose up -d --wait
set -a
source .env
set +a
mvn spring-boot:run
```

Open `http://localhost:8080/` in a browser. You can register or sign in as a
customer, browse upcoming shows, select available seats, view bookings, record
a pending cash payment, or cancel an unpaid booking before the show starts.
An admin can sign in with the configured admin account, create theatres,
screens, movies, shows, and seats, and manually confirm a cash payment after
receiving the money.

The UI keeps the JWT in memory only; refreshing or closing the tab signs you
out. This avoids persisting a token in browser storage and keeps the code
simple. It is a demo UI, not a production payment terminal. Cash confirmation
does not transfer money, and there is no refund workflow.

The catalogue page currently loads up to 100 records per resource, suitable
for this demo. The REST APIs remain available for full paginated access.

## Usability details

The browser shows loading states while fetching shows, seats, and bookings;
form errors appear next to the relevant form. Seat buttons retain keyboard
focus when selected, and the selected-seat summary announces changes. Expired
sessions sign out and ask the user to log in again. The layout adapts to small
screens and respects reduced-motion preferences.
