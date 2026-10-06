package lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@SpringBootApplication
public class OrdersApp {

    public static void main(String[] args) {
        SpringApplication.run(OrdersApp.class, args);
    }

    @Repository
    public static class OrderRepository {
        private final JdbcTemplate jdbc;

        public OrderRepository(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
            jdbc.execute("CREATE TABLE IF NOT EXISTS orders (id serial PRIMARY KEY, sku text NOT NULL)");
        }

        public void add(String sku) {
            jdbc.update("INSERT INTO orders (sku) VALUES (?)", sku);
        }

        public int count() {
            return jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class);
        }
    }
}
