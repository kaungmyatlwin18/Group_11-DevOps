import com.napier.sem.report.country.AllCountriesReport;
import com.napier.sem.report.country.CountriesByContinentReport;
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

/** Tests for Developer 2's assigned Issues #2 and #4 only. */
public class CountryReportTest {

    private Connection createTestDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:test_" + UUID.randomUUID().toString().replace("-", "")
                        + ";MODE=MySQL");

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100))");
            statement.execute("""
                    CREATE TABLE country (
                      Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100),
                      Continent VARCHAR(50), Region VARCHAR(100),
                      Population BIGINT, Capital INT)
                    """);
            statement.executeUpdate("""
                    INSERT INTO city VALUES
                    (1, 'Peking'), (2, 'New Delhi'), (3, 'Washington'),
                    (4, 'Jakarta'), (5, 'Tokyo')
                    """);
            statement.executeUpdate("""
                    INSERT INTO country VALUES
                    ('CHN', 'China', 'Asia', 'Eastern Asia', 1000, 1),
                    ('IND', 'India', 'Asia', 'Southern and Central Asia', 900, 2),
                    ('USA', 'United States', 'North America', 'North America', 800, 3),
                    ('IDN', 'Indonesia', 'Asia', 'Southeast Asia', 750, 4),
                    ('JPN', 'Japan', 'Asia', 'Eastern Asia', 700, 5)
                    """);
        }
        return connection;
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private String captureOutput(SqlAction action) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private List<String> codes(String output) {
        return output.lines()
                .filter(line -> line.matches("^(CHN|IND|USA|IDN|JPN)\\s+.*"))
                .map(line -> line.stripLeading().split("\\s+")[0])
                .toList();
    }

    @Test
    void allCountriesAreOrderedByPopulation() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() -> new AllCountriesReport().display(connection));
            assertEquals(List.of("CHN", "IND", "USA", "IDN", "JPN"), codes(output));
            for (String header : List.of("Code", "Name", "Continent", "Region",
                    "Population", "Capital")) {
                assertTrue(output.contains(header), "Missing column: " + header);
            }
            assertTrue(output.contains("Peking"));
        }
    }

    @Test
    void onlyCountriesInAsiaAreShownInPopulationOrder() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new CountriesByContinentReport().display(connection, "Asia"));
            assertEquals(List.of("CHN", "IND", "IDN", "JPN"), codes(output));
            assertFalse(output.contains("United States"));
        }
    }

    @Test
    void blankContinentIsRejected() throws Exception {
        try (Connection connection = createTestDatabase()) {
            assertThrows(IllegalArgumentException.class, () ->
                    new CountriesByContinentReport().display(connection, "  "));
        }
    }
}
