# redis
Testcontainers tests for an order cache using the Jedis client against a real Redis 7.4.1 container.

## Goal
Show that a service without a dedicated Testcontainers module can still be tested with `GenericContainer` and a mapped port. The tests cover cache behaviour a mock would not reproduce.

## Run it
```
cd redis
mvn -q test
```
Expected: `OrderCacheRedisTest` runs 3 tests with 0 failures (about 7 s here). Docker must be running.

## What it proves
- `cacheHitAndMiss` stores `order:1` and reads it back, and an unknown key returns `null`.
- `entriesExpire` sets a 200 ms TTL with `psetex`; the value is present at once and gone after a 400 ms sleep.
- `counterIsAtomic` calls `incr` three times on `orders:count` and gets 3.
- `OrderCacheRedisTest.java` finds the server with `REDIS.getHost()` and `getMappedPort(6379)`, never a fixed port.

## Trade-offs
- The expiry test uses `Thread.sleep(400)`, so it depends on timing and could be flaky on a very slow machine.
- Only single-node Redis is covered; there is no cluster, persistence or pub/sub test.
- Keys are not flushed between tests (only the counter key is deleted), so tests use distinct keys.

## When not to use it
- If the cache is behind an interface you can fake, a map-backed fake is enough for unit tests.
- For testing Redis Cluster or Sentinel failover.
