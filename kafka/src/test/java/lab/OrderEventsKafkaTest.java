package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class OrderEventsKafkaTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Test
    void ordersKeepPerKeyOrdering() {
        var producerProps = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (var producer = new KafkaProducer<String, String>(producerProps)) {
            for (var status : List.of("CREATED", "PAID", "SHIPPED")) {
                // same key => same partition => consumers see these in order
                producer.send(new ProducerRecord<>("orders", "order-42", status));
            }
            producer.flush();
        }

        var consumerProps = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "lab-group",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var seen = new ArrayList<String>();
        try (var consumer = new KafkaConsumer<String, String>(consumerProps)) {
            consumer.subscribe(List.of("orders"));
            var deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            while (seen.size() < 3 && System.nanoTime() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(r -> seen.add(r.value()));
            }
        }
        assertEquals(List.of("CREATED", "PAID", "SHIPPED"), seen);
    }
}
