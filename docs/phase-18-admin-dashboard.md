# Phase 18: admin overview

The Admin desk shows current counts for total, confirmed, and cancelled
bookings, pending CASH payments, and upcoming shows. The browser requests fresh
counts when the Admin desk opens and after catalogue or cash payment changes.

The summary endpoint is `GET /api/admin/dashboard` and requires the ADMIN role.
Upcoming shows are counted using the current server date and time.
