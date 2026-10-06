# localstack
Testcontainers tests that run S3 and SQS calls with the AWS SDK v2 against a LocalStack 3.8.1 container.

## Goal
Show how to test AWS-facing code without a real account or any cost. The example stores an order invoice in S3 and sends an order event through SQS.

## Run it
```
cd localstack
mvn -q test
```
Expected: `OrdersAwsTest` runs 2 tests with 0 failures (about 23 s here, mostly container startup). Docker must be running. No AWS account or credentials are involved.

## What it proves
- `invoiceRoundTripThroughS3` creates bucket `invoices`, writes `order-1.txt` with `total=42` and reads the same text back.
- `orderEventThroughSqs` creates queue `order-events`, sends `order-1 CREATED` and receives it.
- `OrdersAwsTest.java` points both clients at `getEndpointOverride(...)` and uses the container's dummy keys; S3 uses path-style access.

## Trade-offs
- LocalStack only emulates AWS; IAM policies, quotas and latency differ from the real service.
- Only S3 and SQS are started, so each extra service means another `withServices` entry and a slower start.
- Tests share one container, so buckets and queues persist across tests in the class.

## When not to use it
- To verify real IAM permissions or cross-account behaviour; that needs a real environment.
- For code that already hides AWS behind an interface you can fake.
