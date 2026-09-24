# Ledger API

A REST service for tracking expenses. Spring Boot, PostgreSQL, JPA, 13 JUnit
tests.

Built to learn the Java stack that most Canadian banks and insurers hire for.
The scope is deliberately small so the engineering choices are visible rather
than buried.

## Stack

Java 21, Spring Boot 3.3, Spring Data JPA, Hibernate, PostgreSQL, Bean
Validation, JUnit 5, MockMvc, H2 for tests, Maven, GitHub Actions.

## Endpoints

| Method | Path | Does |
|---|---|---|
| POST | `/api/expenses` | Create an expense |
| GET | `/api/expenses` | List, paginated, filterable by category |
| GET | `/api/expenses/{id}` | Fetch one |
| PUT | `/api/expenses/{id}` | Update one |
| DELETE | `/api/expenses/{id}` | Delete one |
| GET | `/api/expenses/summary?from=&to=` | Totals per category over a date range |
| GET | `/health` | Liveness probe |

## Running it

```bash
docker compose up -d db          # Postgres on localhost:5433 (the app's default)
mvn spring-boot:run              # http://localhost:8080
```

Tests need no database. They run on in-memory H2 in PostgreSQL compatibility
mode:

```bash
mvn test
```

```
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Design decisions

**Money is `BigDecimal`, never `double`.** Binary floating point cannot
represent 0.10 exactly, so `0.10 + 0.20` is not `0.30`. There is a test that
asserts exactly this, because a ledger whose totals are off by a cent is
worthless. The column is `precision = 12, scale = 2`.

**Request and response records are separate from the entity.** `ExpenseRequest`
has no `id` and no `createdAt`, so a client cannot set either by including them
in the JSON. Exposing the entity directly is how mass-assignment bugs happen.

**Validation is declarative and returns field-level errors.** Bean Validation
annotations on the request record, and `@RestControllerAdvice` turns a failure
into `{"status": 400, "fields": {"amount": "amount must be greater than zero"}}`
rather than Spring's default error page. A missing row returns 404, not 500.

**The summary is a `GROUP BY` in the database.** Totals per category are
computed with a JPQL aggregate query returning a projection interface, not by
loading every row and summing in Java.

**Constructor injection, not `@Autowired` on a field.** The controller's
repository is required, so it belongs in the constructor where it cannot be null and the class
can be built in a test without a Spring context.

**Composite index on `(category, spent_on)`.** The list endpoint filters by
category and sorts by date, so one index serves both instead of a filter
followed by a sort.

**Enum stored as `STRING`, not ordinal.** Ordinals are positional, so inserting
a new category in the middle of the enum would silently rewrite the meaning of
every existing row.

## Layout

```
src/main/java/com/jatinbagga/ledger/
  LedgerApplication.java          entry point
  model/Expense.java              JPA entity
  model/Category.java             enum
  repository/ExpenseRepository.java  Spring Data repo + the aggregate query
  dto/ExpenseRequest.java         validated input contract
  dto/ExpenseResponse.java        output contract
  dto/CategorySummary.java        summary row
  controller/ExpenseController.java  CRUD + summary
  controller/HealthController.java
  exception/ApiExceptionHandler.java consistent JSON errors
  exception/NotFoundException.java
src/test/java/...ExpenseControllerTest.java  13 MockMvc tests
```

## Not done yet

- Flyway migrations. `ddl-auto=update` is fine for development and wrong for
  production, where the schema should be owned by versioned migrations.
- Authentication. Every endpoint is currently open.
- Multi-user support. There is no user column yet, so this is a single ledger.
