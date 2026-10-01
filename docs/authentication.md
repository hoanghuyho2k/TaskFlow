# Login and bearer authentication

Before starting Java, generate a local signing key in the same terminal:

```sh
export JWT_SECRET="$(openssl rand -base64 32)"
./mvnw spring-boot:run
```

Startup requires a Base64-encoded key of at least 32 random bytes. Keep the key
private and stable for tokens to remain valid across restarts. Regenerating it
invalidates previously issued tokens. Do not commit it. Tests use a separate,
explicit test profile and a test-only key.

Log in with the User you already registered:

```sh
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"learner@example.com","password":"Example-password-123!"}'
```

The response contains `accessToken`, `tokenType: "Bearer"`, and `expiresIn: 900`
(seconds). Copy the accessToken into Swagger UI's **Authorize** dialog. Enter
only the token; Swagger supplies the Bearer prefix. For curl:

```sh
curl -i http://localhost:8080/api/projects \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN'
```

Project and Task endpoints now require authentication. Registration, login,
Actuator health, and API documentation are public. Missing or invalid bearer
tokens return 401 Problem Details and a `WWW-Authenticate: Bearer` header.
Wrong passwords and unknown emails return the same `Invalid email or password`
response. Unknown emails still perform a dummy password-hash comparison to reduce
the obvious timing difference; this does not guarantee identical timing.

JWTs are signed, not encrypted. Claims contain only User ID as `sub`, issuer,
audience, issue time, and expiry. Spring validates HS256 signatures, issuer,
audience, subject format, and timestamps (with its standard clock tolerance).
Passwords and hashes are never token claims. The token expires after 15 minutes;
log in again to obtain another. Refresh tokens and immediate revocation are not
implemented. Deleted Users are not yet checked on each request.

Security is stateless: no server session or authentication cookie is created.
CSRF protection is disabled because authentication uses an explicitly supplied
Authorization header. Do not switch to cookie-based authentication without
revisiting CSRF. HTTPS and login rate limiting are deployment concerns still pending.

Authentication establishes the caller's identity. Authorization is the next
milestone step: for now, every authenticated User can access all Projects and Tasks.
Project ownership and membership checks will restrict that access next.

Your old unauthenticated curl examples now need an Authorization header. User
registration remains public, but it does not itself issue a token.
