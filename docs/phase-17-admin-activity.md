# Phase 17: admin activity log

The Admin desk shows recent successful catalogue changes, user updates, cash
confirmations, and cash refunds. Each entry records the admin user ID, action,
resource, resource ID when available, and timestamp. Request bodies and customer
credentials are not copied into the log.

Admins can also read the paginated log with `GET /api/admin/activity`. The
endpoint is protected by the ADMIN role. Flyway migration
`V5__create_admin_activity.sql` creates the audit table. Log writes occur after
the API operation succeeds and are committed separately, so they do not change
the result of the original action if logging itself fails.
