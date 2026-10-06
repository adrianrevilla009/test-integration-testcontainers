# kafka
One Testcontainers test that starts a Kafka broker, produces three order events and reads them back.

## Goal
Show how to test a Kafka producer and consumer against a real broker. The example checks that events sent with the same key arrive in order.

## Run it
```
cd kafka
mvn -q test
```
Expected: `OrderEventsKafkaTest` runs 1 test with 0 failures (about 23 s here, mostly broker startup). Docker must be running.

## What it proves
- `OrderEventsKafkaTest.java` starts `confluentinc/cp-kafka:7.6.1` through `KafkaContainer` and uses `getBootstrapServers()` for both clients.
- Three records with key `order-42` (`CREATED`, `PAID`, `SHIPPED`) go to topic `orders`; the plain `KafkaConsumer` reads them back in that order.
- The consumer polls for up to 30 s and uses `auto.offset.reset=earliest`, so it sees records written before it subscribed.

## Trade-offs
- Broker startup dominates the runtime; the container is shared only within this one test class.
- The topic is auto-created on first send; partition count and replication are not configured.
- The poll loop is hand-written with a deadline; there is no Spring Kafka listener or Awaitility here.

## When not to use it
- For checking serializer or business logic alone, a mock producer is much faster.
- To test cluster behaviour such as multi-broker failover; this is a single broker.
