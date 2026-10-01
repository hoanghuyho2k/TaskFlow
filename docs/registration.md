# Registration

Restart the application, then use Swagger UI's Authentication group or:

```sh
curl -i -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"learner@example.com","displayName":"Learner","password":"Example-password-123!"}'
```

Expect 201 with `id`, normalized `email`, trimmed `displayName`, and `createdAt`.
No password, hash, or token is returned. The example is a learning credential.
Sending the same normalized email again returns 409. Invalid request fields return
400 Problem Details with field messages. Use a different email for another User.

Email is required, validated after trimming/lowercasing, and limited to 254
characters. Display name is required and limited to 100 characters. Password is
required, at least 12 Java characters, and at most 72 UTF-8 bytes. Passwords are
never trimmed. Multibyte characters use more than one byte, so character length
alone cannot enforce bcrypt's limit.

The controller validates the DTO before calling the transactional registration
service. The service hashes the password and saves the User. An initial email
lookup provides a quick conflict response; PostgreSQL's unique constraint prevents
duplicates when concurrent registrations race. `saveAndFlush` detects that conflict
inside the service, which translates only `users_email_unique` violations to 409.
Other database failures remain server failures. Runtime exceptions roll back.

Request `toString` redacts credentials, password JSON serialization is write-only,
responses contain no credential fields, and verbose MVC request logging is disabled.
Do not enable request-body or Hibernate parameter logging for credential flows.

Registration creates an account; it does not authenticate a session. Login and JWT
are next. Project access control and membership will follow afterward.
