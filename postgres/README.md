# postgres
JUnit 5 tests that run plain JDBC against a real PostgreSQL 16.4 container started by Testcontainers.

## Goal
Show the smallest useful Testcontainers setup: one throwaway Postgres per test class, with the schema reset before each test. The point is to test against real Postgres behaviour instead of an in-memory imitation.

## Run it
```
cd postgres
mvn -q test
```
Expected: `OrdersPostgresTest` runs 3 tests with 0 failures (about 11 s here, most of it container startup). Docker must be running.

## What it proves
- `insertAndQuery` inserts two rows into `orders` and reads back `sum(qty)` = 8.
- `checkConstraintIsEnforced` inserts `qty = 0` and gets SQLState `23514` (check_violation), an error code H2 does not reproduce the same way.
- `serverIsRealPostgres` runs `SHOW server_version` and sees a `16.4` server.
- The container field in `OrdersPostgresTest.java` is `static`, so all tests share one container; `@BeforeEach` drops and recreates the table for isolation.

## Trade-offs
- Needs a Docker daemon and pulls `postgres:16.4-alpine` on first run, so it is slower than an in-memory database.
- Dropping and recreating the table per test is simple but would be slow for large schemas.
- Connections are opened by hand with `DriverManager`; there is no pool or migration tool here.

## When not to use it
- For pure unit tests of logic that never touches SQL, a container is wasted time.
- On CI runners without Docker access.
