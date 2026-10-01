# TaskFlow

A Java 21 / Spring Boot backend learning project for project and task management.
The current milestone supports creating, listing, retrieving, updating, and deleting projects through a
layered REST API backed by PostgreSQL and Flyway migrations.
Additional domain APIs and event processing will be introduced incrementally.
Task persistence is now available; Task HTTP endpoints are the next step.

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
datasource check. `/` returns 404; project endpoints live under `/api/projects`.

Stop the application with Ctrl+C before building:

```sh
./mvnw verify
```

The generated `@SpringBootTest` loads the application context and connects to
the Compose database. Keep PostgreSQL running for this test. It is a foundation
smoke test. Project repository tests also exercise real PostgreSQL writes and
reads, rolling back their transactions. Identity sequences may still advance.
MockMvc API tests exercise validation and the controller, service, and repository
together, also rolling back test transactions. They do not independently prove
commit behavior because they wrap each request in a test transaction.
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
fields, column lengths, and rejects empty or space-only names. Request DTO
validation rejects blank names and oversized fields before persistence.

`ProjectRepository extends JpaRepository<Project, Long>` gives Spring Data the
entity and ID types. Spring creates the repository bean and CRUD implementation.
The service adds transaction boundaries, and the controller exposes HTTP endpoints.

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

## Create and retrieve projects

Restart the application after building changes. Create a project:

```sh
curl -i -X POST http://localhost:8080/api/projects \
  -H 'Content-Type: application/json' \
  -d '{"name":"TaskFlow","description":"Backend portfolio"}'
```

Expect `201 Created`, a `Location: /api/projects/<id>` header, and JSON with
`id`, `name`, `description`, and `createdAt`. Description is optional; name is
required and limited to 150 characters, description to 2000. Validation applies
to the submitted name before surrounding whitespace is stripped for storage.
Names need not be unique. Replace `1` below with the ID returned from creation:

```sh
curl -i http://localhost:8080/api/projects/1
curl -i http://localhost:8080/api/projects/9223372036854775807
curl -i -X POST http://localhost:8080/api/projects \
  -H 'Content-Type: application/json' -d '{"name":"   "}'
```

Expect `200` for an existing project, `404` for a missing project, and `400` for
the blank name. Error responses use `application/problem+json`; request field
validation includes an `errors` array with field names and messages. Malformed
JSON and invalid IDs also return `400`. Manual POST requests persist real rows.

Request flow: Spring MVC deserializes JSON into `CreateProjectRequest` and runs
Bean Validation through `@Valid`. The controller calls `ProjectService`, which
constructs the entity and calls the repository inside `@Transactional`. Spring's
transaction proxy commits before the service returns to the controller; runtime
exceptions roll back. The service maps the entity to `ProjectResponse` inside
the transaction, and the controller sends HTTP status, headers, and JSON.
Retrieval uses `@Transactional(readOnly = true)` as a persistence optimization
hint, not an authorization rule or a guarantee that writes are impossible.

`@Service` and `@RestController` register Spring beans. Constructor injection
connects the controller to the service and the service to the generated repository
bean. The records define immutable API data; JPA entities represent persisted
state. `ApiExceptionHandler` translates a missing project to `404` and delegates
standard MVC failures to Spring's `ResponseEntityExceptionHandler`.

## List, update, and delete projects

```sh
curl -i 'http://localhost:8080/api/projects?page=0&size=20'
```

Listing returns `content`, `page`, `size`, `totalElements`, and `totalPages`.
Pages start at zero, size defaults to 20 and must be between 1 and 100, and results
are sorted by ascending ID. An empty or out-of-range page returns `200` with an
empty `content` array. Invalid pagination parameters return `400`.

Use the ID of a project you created in the following commands (replace `1`):

```sh
curl -i -X PUT http://localhost:8080/api/projects/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"TaskFlow updated","description":"Updated description"}'
curl -i http://localhost:8080/api/projects/1
curl -i -X DELETE http://localhost:8080/api/projects/1
curl -i http://localhost:8080/api/projects/1
```

PUT returns `200` and replaces the editable fields. Name is required; omitted or
null description clears the previous description. The same length and blank-name
rules apply as creation. ID and `createdAt` stay unchanged. Missing projects return
`404`; PUT does not create a project at a supplied ID. DELETE permanently removes
the project, returns `204` with no body, and subsequent GET or DELETE returns `404`.

Update loads a managed entity inside a service transaction and calls
`updateDetails`. Hibernate's dirty checking detects changed fields and writes an
UPDATE when the transaction flushes; an extra repository `save` call is unnecessary.
Repository tests flush and clear the persistence context before re-reading to
verify database state. These tests roll back, so manual projects remain intact.
Concurrent updates currently use last-write-wins; optimistic locking is a future
design decision. Offset pagination gives stable ordering but not a snapshot
across requests when other clients insert or delete projects.

Milestone 2's basic Project CRUD is complete. Next is the Task domain and its
relationship to Project. Ownership and access control will follow with authentication.

## Task persistence (Milestone 3, first step)

`Task` belongs to one Project through a required `project_id` foreign key.
The Java mapping uses a unidirectional `@ManyToOne(fetch = LAZY)` relationship:
the Task references its Project, and Project does not hold a task collection.
Lazy loading defers loading project details until needed; access those details
inside a service transaction when mapping future response DTOs.

Tasks have a required title (200 characters), optional description (5000
characters), creation timestamp, status (`TODO`, `IN_PROGRESS`, `DONE`), and
priority (`LOW`, `MEDIUM`, `HIGH`). New tasks default to `TODO` and `MEDIUM`.
Enums use `EnumType.STRING`; SQL CHECK constraints enforce the allowed names.
Reordering enum constants is safe, but renaming an enum value requires a migration
and a plan for existing rows. Project membership is fixed for a task in this model.
Assignment is deferred until the User domain exists.

Flyway V2 creates the table and an index on `(project_id, id)` for ordered task
queries within a project. PostgreSQL does not automatically index the referencing
side of a foreign key. The repository exposes a paginated `findByProjectId` query.

Deleting a Project permanently deletes its Tasks using database `ON DELETE CASCADE`.
Deleting a Task does not delete its Project. There is no JPA cascade configured
on the Task-to-Project relationship. Database cascades do not update cached JPA
entities; the delete flow must not keep using previously loaded tasks afterward.

After running `./mvnw verify`, inspect the new schema:

```sh
docker compose exec postgres psql -U taskflow -d taskflow
```

```sql
\d tasks
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
\q
```

Expect successful migrations V1 and V2. Task repository tests verify persistence,
defaults, project-scoped pagination, foreign-key and enum constraints, blank-title
rejection, and project deletion cascades. Test writes roll back.

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
