# Phase 14: cash-confirmation audit

When an admin confirms a pending cash payment, the application now stores that
admin's user ID and the confirmation time on the payment. The payment response
includes `confirmedByUserId` and `confirmedAt`. Both are `null` until confirmation.

In the browser, sign in as an admin, enter a payment ID, and select **Look up**
to check its status and amount. After actually receiving the cash, select
**Confirm cash**. You can look up the same ID again to see who confirmed it and
when. Customers can see those details on their own completed payment, too.

Admins can also call `GET /api/payments/admin/{id}` with their bearer token.
Customers cannot use this endpoint; their existing `GET /api/payments/{id}`
continues to return only their own payment.

Flyway migration `V3__add_cash_confirmation_audit.sql` runs automatically when
the application next starts. Payments completed before this migration keep
`null` audit fields: the old confirming admin cannot be reconstructed reliably.
This audit identifies the admin account used, but does not verify that physical
cash changed hands or replace a financial accounting system.
