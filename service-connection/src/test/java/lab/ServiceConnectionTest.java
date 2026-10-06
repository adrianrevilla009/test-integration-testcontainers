package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Note: no application.properties and no @DynamicPropertySource.
// @ServiceConnection wires spring.datasource.* from the running container.
@SpringBootTest
@Testcontainers
class ServiceConnectionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.4-alpine");

    @Autowired
    OrdersApp.OrderRepository orders;

    @Autowired
    DataSource dataSource;

    @Test
    void repositoryTalksToTheContainer() {
        orders.add("A-1");
        orders.add("B-2");
        assertEquals(2, orders.count());
    }

    @Test
    void datasourceUrlPointsAtTheMappedPort() throws Exception {
        try (var c = dataSource.getConnection()) {
            assertTrue(c.getMetaData().getURL().contains(String.valueOf(PG.getMappedPort(5432))));
        }
    }
}
