import com.napier.sem.report.city.CitiesByCountryReport;
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
 * Verifies Issue #13 with an isolated H2 World database.
 * The tests do not require a running MySQL server.
 */
public class CitiesByCountryReportTest {

    /** Builds a small country and city dataset in deliberately unsorted order. */
    private Connection createTestDatabase() throws SQLException {
        String databaseName = "city_country_" + UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + databaseName + ";MODE=MySQL");

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
                    ('THA', 'Thailand')
                    """);
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (1, 'Mandalay', 'MMR', 'Mandalay', 885300),
                    (2, 'Tokyo', 'JPN', 'Tokyo-to', 7980230),
                    (3, 'Moulmein', 'MMR', 'Mon', 307900),
                    (4, 'Osaka', 'JPN', 'Osaka', 2595674),
                    (5, 'Rangoon', 'MMR', 'Rangoon', 3361700),
                    (6, 'Bangkok', 'THA', 'Bangkok', 6320174)
                    """);
        }
        return connection;
    }

    @Test
    void returnsOnlyCitiesFromSelectedCountry() throws Exception {
        try (Connection connection = createTestDatabase()) {
            CityReport report = new CityReport();
            List<CityReport.CityRow> myanmar =
                    report.findCitiesInCountry(connection, "Myanmar");
            List<CityReport.CityRow> japan =
                    report.findCitiesInCountry(connection, "Japan");

            assertEquals(3, myanmar.size());
            assertTrue(myanmar.stream().allMatch(city -> city.country().equals("Myanmar")));
            assertFalse(myanmar.stream().anyMatch(city -> city.name().equals("Tokyo")));
            assertEquals(List.of("Tokyo", "Osaka"),
                    japan.stream().map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void ordersCountryCitiesByPopulationDescending() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new CityReport().findCitiesInCountry(connection, "Myanmar");

            assertEquals(List.of("Rangoon", "Mandalay", "Moulmein"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals(List.of(3361700L, 885300L, 307900L),
                    cities.stream().map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void displaysRequiredColumnsAndSelectedCountry() throws Exception {
        try (Connection connection = createTestDatabase()) {
            PrintStream originalOutput = System.out;
            ByteArrayOutputStream capturedBytes = new ByteArrayOutputStream();

            try (PrintStream capture =
                         new PrintStream(capturedBytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                new CitiesByCountryReport().display(connection, "Myanmar");
            } finally {
                System.setOut(originalOutput);
            }

            String output = capturedBytes.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Cities in country: Myanmar"));
            for (String column : List.of("Name", "Country", "District", "Population")) {
                assertTrue(output.contains(column), "Missing column: " + column);
            }
            assertTrue(output.contains("Rangoon"));
            assertTrue(output.contains("Myanmar"));
            assertTrue(output.contains("Mandalay"));
            assertTrue(output.contains("3361700"));
            assertFalse(output.contains("Tokyo"));
            assertFalse(output.contains("Bangkok"));
        }
    }
}
