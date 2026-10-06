# test-integration-testcontainers

Integration tests for an Orders service that run against real Postgres, Kafka, Redis and emulated AWS in throwaway Docker containers, plus HTTP stubs and async assertions. It is useful as a set of small, working patterns you can copy into a Java project.

## What is inside

| Folder | What it shows | Run |
| --- | --- | --- |
| [`postgres`](./postgres) | JDBC tests against a PostgreSQL container, shared per class | `cd postgres && mvn -q test` |
| [`kafka`](./kafka) | Produce and consume order events on a Kafka container, per-key ordering | `cd kafka && mvn -q test` |
| [`redis`](./redis) | Cache hit, miss, expiry and counter tests on a generic Redis container | `cd redis && mvn -q test` |
| [`service-connection`](./service-connection) | Spring Boot datasource wired to a container with `@ServiceConnection` | `cd service-connection && mvn -q test` |
| [`wiremock-mockserver-stubs`](./wiremock-mockserver-stubs) | WireMock and MockServer stubs with delay and connection faults | `cd wiremock-mockserver-stubs && mvn -q test` |
| [`awaitility-async`](./awaitility-async) | Awaitility polling and timeout messages for async code | `cd awaitility-async && mvn -q test` |
| [`localstack`](./localstack) | S3 and SQS tests against LocalStack | `cd localstack && mvn -q test` |

## Prerequisites

- Java 21 and Maven 3.8 or newer
- A running Docker daemon (every folder except `awaitility-async`, and the WireMock half of `wiremock-mockserver-stubs`)

## How to read it

Start with `postgres` for the basic container lifecycle, then `service-connection` for the Spring Boot version. Each folder is a standalone Maven project with its own `pom.xml`, and all of them were run locally with their tests passing.
