package lab;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Stand-in for any eventually-consistent consumer (Kafka listener, queue worker):
 * the order status changes some time after submit() returns.
 */
class AsyncOrderProcessingTest {

    final ExecutorService pool = Executors.newSingleThreadExecutor();
    final ConcurrentHashMap<String, String> status = new ConcurrentHashMap<>();

    @AfterEach
    void shutdown() {
        pool.shutdownNow();
    }

    void submit(String id, long processingMillis) {
        status.put(id, "CREATED");
        pool.submit(() -> {
            Thread.sleep(processingMillis);
            status.put(id, "PAID");
            return null;
        });
    }

    @Test
    void waitsOnlyAsLongAsNeeded() {
        submit("o-1", 300);
        var start = System.nanoTime();
        await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> assertEquals("PAID", status.get("o-1")));
        var elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertTrue(elapsedMs < 2000, "returned shortly after the status flipped, not after the full timeout");
    }

    @Test
    void pollsUntilCounterReachesTarget() {
        var processed = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            pool.submit(() -> {
                Thread.sleep(100);
                processed.incrementAndGet();
                return null;
            });
        }
        await().atMost(Duration.ofSeconds(5)).until(processed::get, n -> n == 3);
    }

    // The failure mode is the point: a precise timeout instead of a hang or a flaky fixed sleep.
    @Test
    void timesOutWithAReadableMessage() {
        submit("o-2", 10_000);
        var e = assertThrows(ConditionTimeoutException.class, () -> await().atMost(Duration.ofMillis(500))
                .untilAsserted(() -> assertEquals("PAID", status.get("o-2"))));
        assertTrue(e.getMessage().contains("expected: <PAID> but was: <CREATED>"));
    }
}
