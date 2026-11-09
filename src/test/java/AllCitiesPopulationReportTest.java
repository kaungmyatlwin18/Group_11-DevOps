import com.napier.sem.report.city.AllCitiesPopulationQuery;
import com.napier.sem.report.city.AllCitiesPopulationReport;
import com.napier.sem.report.city.CityReport;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #10: all cities worldwide ordered by population.
 * Uses an independent H2 database; no Docker/MySQL connection is needed.
 */
public class AllCitiesPopulationReportTest {

    /** Inserts cities in intentionally mixed order across several countries. */
    private Connection createTestDatabase() throws SQLException {
        String name = "all_cities_" + UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + name + ";MODE=MySQL");

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE city (
                        ID INT PRIMARY KEY,
                        Name VARCHAR(100),
                        CountryCode VARCHAR(3),
                        District VARCHAR(100),
                        Population BIGINT
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name) VALUES
                    ('MMR', 'Myanmar'),
                    ('JPN', 'Japan'),
                    ('CHN', 'China'),
                    ('KOR', 'South Korea')
                    """);
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (1, 'Mandalay', 'MMR', 'Mandalay', 885300),
                    (2, 'Tokyo', 'JPN', 'Tokyo-to', 7980230),
                    (3, 'Rangoon', 'MMR', 'Rangoon', 3361700),
                    (4, 'Shanghai', 'CHN', 'Shanghai', 9696300),
                    (5, 'Osaka', 'JPN', 'Osaka', 2595674),
                    (6, 'Seoul', 'KOR', 'Seoul', 9981619)
                    """);
        }
        return connection;
    }

    @Test
    void returnsAllCitiesAcrossEveryCountry() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new AllCitiesPopulationQuery().findAllCities(connection);

            assertEquals(6, cities.size(), "The report must include all six cities");
            assertEquals(List.of("South Korea", "China", "Japan", "Myanmar", "Japan", "Myanmar"),
                    cities.stream().map(CityReport.CityRow::country).toList());
            assertTrue(cities.stream().anyMatch(city -> city.name().equals("Mandalay")));
            assertTrue(cities.stream().anyMatch(city -> city.name().equals("Osaka")));
        }
    }

    @Test
    void ordersCitiesWorldwideByPopulationDescending() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new AllCitiesPopulationQuery().findAllCities(connection);

            assertEquals(List.of("Seoul", "Shanghai", "Tokyo", "Rangoon", "Osaka", "Mandalay"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals(List.of(9981619L, 9696300L, 7980230L, 3361700L, 2595674L, 885300L),
                    cities.stream().map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void displaysRequiredColumnsAndWorldwideResults() throws Exception {
        try (Connection connection = createTestDatabase()) {
            PrintStream originalOutput = System.out;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                new AllCitiesPopulationReport().display(connection);
            } finally {
                System.setOut(originalOutput);
            }

            String output = bytes.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("All cities in the world by population"));
            for (String header : List.of("Name", "Country", "District", "Population")) {
                assertTrue(output.contains(header), "Missing column: " + header);
            }
            assertTrue(output.contains("Seoul"));
            assertTrue(output.contains("South Korea"));
            assertTrue(output.contains("Tokyo-to"));
            assertTrue(output.contains("Myanmar"));
            assertTrue(output.contains("9981619"));
            assertTrue(output.contains("885300"));
            assertTrue(output.indexOf("Seoul") < output.indexOf("Shanghai"),
                    "Largest city should print before the second largest");
        }
    }
}
