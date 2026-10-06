# service-connection
A small Spring Boot 3.3.5 app with an Orders repository, tested with a Postgres container wired in by `@ServiceConnection`.

## Goal
Show how Spring Boot configures its datasource from a running Testcontainers container without any properties file. This replaces the older `@DynamicPropertySource` pattern.

## Run it
```
cd service-connection
mvn -q test
```
Expected: `ServiceConnectionTest` runs 2 tests with 0 failures (about 21 s here, including Spring startup and the container). Docker must be running.

## What it proves
- `ServiceConnectionTest.java` has no `application.properties` and no `@DynamicPropertySource`; `@ServiceConnection` on the `PostgreSQLContainer` field supplies `spring.datasource.*`.
- `OrdersApp.OrderRepository` creates the `orders` table and inserts two SKUs through `JdbcTemplate`; `count()` returns 2.
- The datasource JDBC URL contains the container's mapped port, so the app really talks to the container.

## Trade-offs
- It pulls in the Spring Boot test stack, so it starts slower than the plain `postgres` folder.
- The table is created in the repository constructor for brevity; a real app would use Flyway or Liquibase.
- The database is shared by all tests in the class and is not reset between them.

## When not to use it
- For code that is not a Spring Boot app, use plain Testcontainers as in `postgres`.
- For pure repository logic with no SQL, a unit test is faster.
