package lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class OrdersPostgresTest {

    // static: one container shared by every test in the class (startup is the slow part)
    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16.4-alpine");

    Connection conn() throws SQLException {
        return DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
    }

    // Fresh schema per test keeps tests independent while the container is shared.
    @BeforeEach
    void schema() throws SQLException {
        try (var c = conn(); var s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS orders");
            s.execute("CREATE TABLE orders (id serial PRIMARY KEY, sku text NOT NULL, qty int CHECK (qty > 0))");
        }
    }

    @Test
    void insertAndQuery() throws SQLException {
        try (var c = conn(); var s = c.createStatement()) {
            s.execute("INSERT INTO orders (sku, qty) VALUES ('A-1', 3), ('B-2', 5)");
            var rs = s.executeQuery("SELECT sum(qty) FROM orders");
            rs.next();
            assertEquals(8, rs.getInt(1));
        }
    }

    // Real Postgres semantics (constraint error codes) that H2 only imitates.
    @Test
    void checkConstraintIsEnforced() throws SQLException {
        try (var c = conn(); var s = c.createStatement()) {
            var e = assertThrows(SQLException.class, () -> s.execute("INSERT INTO orders (sku, qty) VALUES ('A-1', 0)"));
            assertEquals("23514", e.getSQLState()); // check_violation
        }
    }

    @Test
    void serverIsRealPostgres() throws SQLException {
        try (var c = conn(); var s = c.createStatement()) {
            var rs = s.executeQuery("SHOW server_version");
            rs.next();
            assertTrue(rs.getString(1).startsWith("16.4"));
        }
    }
}
