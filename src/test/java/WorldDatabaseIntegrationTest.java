
import com.napier.sem.DatabaseConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(
        named = "DB_PASSWORD",
        matches = ".+"
)
public class WorldDatabaseIntegrationTest {

    // Test the application's real database connection
    private Connection connect() throws Exception {
        DatabaseConnection database = new DatabaseConnection();
        return database.connect();
    }

    // Test 1: Verify MySQL connection
    @Test
    void testDatabaseConnection() throws Exception {
        try (Connection conn = connect()) {
            assertTrue(
                    conn.isValid(5),
                    "Database connection should be valid"
            );
        }
    }

    // Test 2: Verify World database records
    @Test
    void testWorldDataCounts() throws Exception {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM country")) {

                assertTrue(rs.next());
                assertEquals(239, rs.getInt(1));
            }

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM city")) {

                assertTrue(rs.next());
                assertEquals(4079, rs.getInt(1));
            }
        }
    }

    // Test 3: Verify Asian countries by population
    @Test
    void testAsianCountriesPopulationOrder() throws Exception {

        String sql = "SELECT Name, Population " +
                "FROM country " +
                "WHERE Continent = 'Asia' " +
                "ORDER BY Population DESC";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            long previousPopulation = Long.MAX_VALUE;
            int count = 0;

            while (rs.next()) {
                long population = rs.getLong("Population");

                assertTrue(
                        population <= previousPopulation,
                        "Countries must be ordered by descending population"
                );

                previousPopulation = population;
                count++;
            }

            assertTrue(
                    count > 0,
                    "Asian countries should exist"
            );
        }
    }
}
