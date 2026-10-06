# awaitility-async
Awaitility tests against a simulated slow order processor, with no container involved.

## Goal
Show how to assert on eventually-consistent behaviour, such as a queue consumer, without fixed sleeps. A single-thread executor stands in for the consumer.

## Run it
```
cd awaitility-async
mvn -q test
```
Expected: `AsyncOrderProcessingTest` runs 3 tests with 0 failures (about 1.3 s here). Docker is not needed.

## What it proves
- `waitsOnlyAsLongAsNeeded`: a status that flips from `CREATED` to `PAID` after 300 ms is detected by polling every 50 ms, well before the 5 s limit.
- `pollsUntilCounterReachesTarget` waits until three background tasks have incremented a counter to 3.
- `timesOutWithAReadableMessage`: with a 500 ms limit and a 10 s task, Awaitility throws `ConditionTimeoutException` whose message contains `expected: <PAID> but was: <CREATED>`.

## Trade-offs
- The processor is simulated in `AsyncOrderProcessingTest.java`; it does not use Kafka or a database, so it shows the pattern only.
- Polling adds up to one poll interval of delay and uses CPU at tight intervals.
- Limits are fixed numbers; on a slow CI machine they may need tuning.

## When not to use it
- When you can call the code synchronously or inject a latch or callback, that is more deterministic.
- For asserting that something never happens; a timeout alone does not prove that.
