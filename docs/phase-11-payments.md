# Phase 11: payment records

This phase lets a signed-in user create **one pending cash payment record** for
their own booking and read it later. An ADMIN can mark it completed only after
receiving the cash. It does **not** connect to a payment gateway, collect
card/UPI credentials, or transfer money. CARD and UPI requests are rejected.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/payments` | Create a pending record for your booking |
| GET | `/api/payments/{id}` | Read your own payment record |
| POST | `/api/payments/{id}/confirm-cash` | ADMIN confirms cash was received |

Both require `Authorization: Bearer <accessToken>`. Example body:

```json
{"bookingId": 1, "method": "CASH"}
```

The service takes the amount from the booking, not the request. It checks
ownership, refuses cancelled bookings, and refuses a second payment for the
same booking. A database lock on the booking plus its unique payment constraint
protect against concurrent duplicate requests. Reading another user's payment
returns 404.

The admin confirmation changes PENDING to COMPLETED and generates a cash receipt
reference. It does not collect or verify money automatically. Do not call the
confirmation endpoint until the cash has actually been received. A second
confirmation is rejected. Phase 14 adds the confirming admin ID and time for
new confirmations; see `phase-14-cash-audit.md`.

After code changes, restart the app from the project folder:

```bash
cd "/Users/irshdalam/Documents/ChatGPT/Theatre Management System - Java"
set -a
source .env
set +a
mvn spring-boot:run
```
