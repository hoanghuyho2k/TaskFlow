# TaskFlow

A Java 21 / Spring Boot backend learning project for project and task management.
The current milestone establishes PostgreSQL connectivity and application health.
Domain APIs and event processing will be introduced incrementally.

## Local development

Prerequisites: Java 21 and Docker Desktop with Docker Compose. The Maven wrapper
downloads the pinned Maven version, so a separate Maven installation is unnecessary.

Start Docker Desktop, then run from the repository root:

```sh
docker compose up -d --wait postgres
./mvnw spring-boot:run
```

In a second terminal:

```sh
curl -i http://localhost:8080/actuator/health
```

Expect HTTP 200 with `"status":"UP"` (the response may also list health groups).
The aggregate health includes the
datasource check. There are no domain endpoints yet; `/` returns 404.

Stop the application with Ctrl+C before building:

```sh
./mvnw verify
```

The generated `@SpringBootTest` loads the application context and connects to
the Compose database. Keep PostgreSQL running for this test. It is a foundation
smoke test, not yet an isolated database integration test suite.

## How the foundation works

`@SpringBootApplication` enables configuration, automatic configuration and
component scanning under `com.huyho.taskflow`. Dependencies tell Spring Boot
which infrastructure to configure: Spring MVC for HTTP, JPA/Hibernate for
persistence, the PostgreSQL JDBC driver for database access, and Actuator for health.
The Boot parent manages compatible dependency and plugin versions; the project's
`0.0.1-SNAPSHOT` is its own development version, independent of the stable Boot parent.

Compose runs only PostgreSQL. The Java process runs on the host and connects to
`localhost:5432`. The port binds to loopback and the named volume preserves data
across container recreation. The included credentials are local development
defaults. Override `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` for other environments;
do not use the local credentials for deployment. Spring does not automatically
load a `.env` file; export overrides in the shell that launches Java.

`ddl-auto=validate` prevents Hibernate from creating or changing tables. There
are no entities to validate yet. Flyway migrations will accompany the first domain
model. `open-in-view=false` keeps persistence access within application boundaries
instead of extending the Hibernate session through HTTP response rendering.

## Troubleshooting

- Docker connection errors: start Docker Desktop and check `docker info`.
- Port 5432 already allocated: check for another local PostgreSQL or container
  before starting Compose. If you change the host port, change `DB_URL` too.
- Database connection refused: run `docker compose ps` and
  `docker compose logs postgres`; wait for PostgreSQL to be healthy.
- Authentication failed: verify credentials. PostgreSQL initialization variables
  apply only when the data volume is empty; changing them does not change an
  existing database user's password.
- Port 8080 already in use: stop the other application or export `SERVER_PORT=8081`
  and use that port in curl.
- Maven download failure: check network access and Java installation; do not
  remove dependencies or skip tests to hide a failure.

Stop the database while keeping its data:

```sh
docker compose down
```

Adding `-v` deletes the database volume and its data; reserve that for an intentional reset.
