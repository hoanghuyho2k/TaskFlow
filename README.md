# TaskFlow

A Java 21 / Spring Boot backend learning project for project and task management.
The current milestone adds the Project persistence model and a Flyway migration
on top of PostgreSQL connectivity and application health.
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
smoke test. Project repository tests also exercise real PostgreSQL writes and
reads, rolling back their transactions. Identity sequences may still advance.
These tests currently use the local database; isolated Testcontainers tests will
follow in the testing milestone.

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

`ddl-auto=validate` prevents Hibernate from creating or changing tables.
Flyway applies `src/main/resources/db/migration/V1__create_projects.sql` before
Hibernate validates the Project mapping. Applied migrations are recorded with
checksums in `flyway_schema_history`: do not edit an applied migration; add a new
version for later schema changes. `open-in-view=false` keeps persistence access within application boundaries
instead of extending the Hibernate session through HTTP response rendering.

## Project persistence (Milestone 2, first step)

The `project` package groups the entity and repository by domain. `Project` maps
to `projects`, with a generated numeric ID, a required name (150 characters), an
optional description (2000 characters), and an immutable creation timestamp.
Names are not unique. `@PrePersist` supplies the timestamp before the initial JPA
insert; direct SQL writers must supply it themselves. PostgreSQL enforces non-null
fields, column lengths, and rejects empty or space-only names. Request validation
will provide clearer errors when we add DTOs and the controller.

`ProjectRepository extends JpaRepository<Project, Long>` gives Spring Data the
entity and ID types. Spring creates the repository bean and CRUD implementation.
The next step adds the service boundary, DTOs, and HTTP endpoints.

After running `./mvnw verify`, inspect the schema:

```sh
docker compose exec postgres psql -U taskflow -d taskflow
```

Inside psql:

```sql
\d projects
SELECT version, description, success FROM flyway_schema_history;
SELECT count(*) FROM projects;
\q
```

Expect migration version `1` with `success = true`. On a fresh database, the
project count remains zero after tests because test transactions roll back.

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
- Flyway checksum mismatch: an applied migration was changed. Restore the
  original migration and add a new version for the intended change.
- Hibernate schema validation failure: compare the entity mapping and applied
  SQL migration; do not switch to `ddl-auto=update` to hide the mismatch.

Stop the database while keeping its data:

```sh
docker compose down
```

Adding `-v` deletes the database volume and its data; reserve that for an intentional reset.
