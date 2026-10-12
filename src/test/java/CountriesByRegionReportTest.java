import com.napier.sem.report.country.CountriesByRegionReport;
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

/** Automated tests for GitHub Issue #6; does not change Developer 2's tests. */
public class CountriesByRegionReportTest {

    private Connection createTestDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:region_" + UUID.randomUUID().toString().replace("-", "")
                        + ";MODE=MySQL");

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100))");
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100),
                        Continent VARCHAR(50),
                        Region VARCHAR(100),
                        Population BIGINT,
                        Capital INT
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO city VALUES
                    (1, 'Peking'), (2, 'Tokyo'), (3, 'Seoul'),
                    (4, 'New Delhi'), (5, 'Washington')
                    """);
            statement.executeUpdate("""
                    INSERT INTO country VALUES
                    ('CHN', 'China', 'Asia', 'Eastern Asia', 1000, 1),
                    ('JPN', 'Japan', 'Asia', 'Eastern Asia', 800, 2),
                    ('KOR', 'South Korea', 'Asia', 'Eastern Asia', 700, 3),
                    ('IND', 'India', 'Asia', 'Southern and Central Asia', 950, 4),
                    ('USA', 'United States', 'North America', 'North America', 900, 5)
                    """);
        }
        return connection;
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private String captureOutput(SqlAction action) throws SQLException {
        PrintStream previous = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(previous);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private List<String> countryCodes(String output) {
        return output.lines()
                .filter(line -> line.matches("^(CHN|JPN|KOR|IND|USA)\\s+.*"))
                .map(line -> line.trim().split("\\s+")[0])
                .toList();
    }

    @Test
    void selectedRegionShowsOnlyCountriesSortedByPopulation() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new CountriesByRegionReport().display(connection, "Eastern Asia"));
            assertEquals(List.of("CHN", "JPN", "KOR"), countryCodes(output));
            assertFalse(output.contains("India"));
            assertFalse(output.contains("United States"));
            for (String heading : List.of("Code", "Name", "Continent", "Region",
                    "Population", "Capital")) {
                assertTrue(output.contains(heading), "Missing column: " + heading);
            }
            assertTrue(output.contains("Peking"));
            assertTrue(output.contains("Tokyo"));
            assertTrue(output.contains("Seoul"));
        }
    }

    @Test
    void regionWithExtraSpacesIsTrimmed() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new CountriesByRegionReport().display(connection, "  Eastern Asia  "));
            assertEquals(List.of("CHN", "JPN", "KOR"), countryCodes(output));
        }
    }

    @Test
    void blankOrNullRegionIsRejected() throws Exception {
        try (Connection connection = createTestDatabase()) {
            CountriesByRegionReport report = new CountriesByRegionReport();
            assertThrows(IllegalArgumentException.class, () -> report.display(connection, "  "));
            assertThrows(IllegalArgumentException.class, () -> report.display(connection, null));
        }
    }

    @Test
    void unknownRegionDisplaysAnEmptyTable() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new CountriesByRegionReport().display(connection, "Atlantis"));
            assertTrue(countryCodes(output).isEmpty());
            assertTrue(output.contains("No results found."));
        }
    }
}
