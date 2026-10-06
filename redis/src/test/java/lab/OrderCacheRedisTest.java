package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.JedisPooled;

@Testcontainers
class OrderCacheRedisTest {

    // No dedicated Redis module needed: GenericContainer + a mapped port is enough.
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7.4.1-alpine")).withExposedPorts(6379);

    JedisPooled jedis() {
        return new JedisPooled(REDIS.getHost(), REDIS.getMappedPort(6379));
    }

    @Test
    void cacheHitAndMiss() {
        try (var j = jedis()) {
            j.set("order:1", "CREATED");
            assertEquals("CREATED", j.get("order:1"));
            assertNull(j.get("order:missing"));
        }
    }

    // Expiry is server behaviour a mock would never reproduce.
    @Test
    void entriesExpire() throws InterruptedException {
        try (var j = jedis()) {
            j.psetex("order:ttl", 200, "PAID");
            assertEquals("PAID", j.get("order:ttl"));
            Thread.sleep(400);
            assertNull(j.get("order:ttl"));
        }
    }

    @Test
    void counterIsAtomic() {
        try (var j = jedis()) {
            j.del("orders:count");
            j.incr("orders:count");
            j.incr("orders:count");
            assertTrue(j.incr("orders:count") == 3L);
        }
    }
}
