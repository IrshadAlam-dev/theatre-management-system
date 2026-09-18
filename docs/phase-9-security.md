# Phase 9: registration, login, and authorization

## Flow

1. `POST /api/auth/register` validates name, email, and password, then saves a
   CUSTOMER user with a BCrypt password hash. The response never includes a hash.
2. `POST /api/auth/login` compares the supplied password to the stored hash.
   Incorrect email and password both return the same `401` message.
3. Successful login returns a signed bearer token and its lifetime in seconds.
4. Send `Authorization: Bearer <accessToken>` on protected requests. Spring
   Security checks its signature, issuer, and expiration before calling a controller.

Tokens expire after one hour. The server keeps no login session. A user or role
change does not instantly revoke an existing token; a future production system
would need a revocation strategy or shorter token lifetimes. Keep the signing
key private. Changing it invalidates previously issued tokens.

## Access rules

| Endpoint | Access |
| --- | --- |
| `/actuator/health`, `/api/auth/register`, `/api/auth/login` | Public |
| GET movies, theatres, shows, screens, seats | Public |
| Create/update/delete movies, theatres, shows, screens, seats | ADMIN |
| List/read/update/delete users | ADMIN |
| Other routes | Denied until explicitly added |

Missing or invalid credentials return `401`; an authenticated customer attempting
an admin action receives `403`. Both use the same JSON error shape as Phase 8.
CSRF protection is disabled because this is a stateless bearer-token API, not a
browser session using cookies. HTTP Basic and form login are also disabled.

## Required local setup

Generate a random signing key without sending it to anyone:

```bash
openssl rand -base64 32
```

Add the output to your ignored `.env` file as `JWT_SECRET_BASE64=<output>`.
The value must decode to at least 32 bytes. Do not commit `.env`, paste the key
into chat, or reuse a key across environments.

To create the first admin account, optionally add both of these to `.env` before
starting the application:

```text
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=<a unique password of at least 12 characters and at most 72 UTF-8 bytes>
```

Use your own email and password values. On startup, the bootstrap creates an
ADMIN only if that email does not already exist. It never upgrades an existing
customer or resets an admin password. Remove `ADMIN_PASSWORD` from `.env` after
the first successful startup if you prefer; the hashed account remains in MySQL.
Protect `.env` even though Git ignores it.

Spring Boot does not read `.env` automatically. From the project folder:

```bash
set -a
source .env
set +a
mvn spring-boot:run
```

If a password contains shell-special characters or spaces, quote the value in
`.env` so `source` reads it correctly. Do not paste passwords into documentation.

## Example requests

```http
POST /api/auth/register
Content-Type: application/json

{"name":"Sam","email":"sam@example.com","password":"long-unique-password"}
```

```http
POST /api/auth/login
Content-Type: application/json

{"email":"sam@example.com","password":"long-unique-password"}
```

Use the returned `accessToken` only in a bearer header. Do not put it in URLs,
logs, or committed Postman environment files. The admin account logs in through
the same `/api/auth/login` route.

## Tests

`mvn test` runs the fast tests without Docker. `SecurityIT` runs against local
MySQL and checks registration, BCrypt, login, role restrictions, and 401/403
responses. Its database fixture is rolled back after each test. It uses a fixed
test-only signing key, not your local or production key.
