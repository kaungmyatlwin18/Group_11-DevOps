import com.napier.sem.report.country.TopNCountriesByContinentReport;
import com.napier.sem.report.country.TopNCountriesByRegionReport;
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

/** JUnit 5 tests for Group 11 Issues #8 and #9. No Docker/MySQL server is needed. */
public class TopNCountryReportsTest {

    private Connection createTestDatabase() throws SQLException {
        String databaseName = "topn_" + UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + databaseName + ";MODE=MySQL");
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
                    (1, 'Peking'), (2, 'New Delhi'), (3, 'Washington'),
                    (4, 'Tokyo'), (5, 'Seoul'), (6, 'Jakarta'), (7, 'Ulan Bator')
                    """);
            statement.executeUpdate("""
                    INSERT INTO country VALUES
                    ('CHN', 'China', 'Asia', 'Eastern Asia', 1000, 1),
                    ('IND', 'India', 'Asia', 'Southern and Central Asia', 950, 2),
                    ('USA', 'United States', 'North America', 'North America', 940, 3),
                    ('JPN', 'Japan', 'Asia', 'Eastern Asia', 900, 4),
                    ('KOR', 'South Korea', 'Asia', 'Eastern Asia', 800, 5),
                    ('IDN', 'Indonesia', 'Asia', 'Southeast Asia', 850, 6),
                    ('MNG', 'Mongolia', 'Asia', 'Eastern Asia', 650, 7)
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
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private List<String> countryCodes(String output) {
        return output.lines()
                .filter(line -> line.matches("^(CHN|IND|USA|JPN|KOR|IDN|MNG)\\s+.*"))
                .map(line -> line.trim().split("\\s+")[0])
                .toList();
    }

    @Test
    void topThreeCountriesInAsiaAreSortedAndLimited() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new TopNCountriesByContinentReport().display(connection, "Asia", 3));
            assertEquals(List.of("CHN", "IND", "JPN"), countryCodes(output));
            assertFalse(output.contains("United States"));
            for (String column : List.of("Code", "Name", "Continent", "Region", "Population", "Capital")) {
                assertTrue(output.contains(column), "Missing column: " + column);
            }
            assertTrue(output.contains("Peking"));
            assertTrue(output.contains("New Delhi"));
            assertTrue(output.contains("Tokyo"));
        }
    }

    @Test
    void topTwoCountriesInEasternAsiaAreSortedAndLimited() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new TopNCountriesByRegionReport().display(connection, "Eastern Asia", 2));
            assertEquals(List.of("CHN", "JPN"), countryCodes(output));
            assertFalse(output.contains("India"));
            assertFalse(output.contains("South Korea"));
        }
    }

    @Test
    void continentAndRegionFiltersAreTrimmed() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String continentOutput = captureOutput(() ->
                    new TopNCountriesByContinentReport().display(connection, "  Asia  ", 2));
            String regionOutput = captureOutput(() ->
                    new TopNCountriesByRegionReport().display(connection, "  Eastern Asia  ", 2));
            assertEquals(List.of("CHN", "IND"), countryCodes(continentOutput));
            assertEquals(List.of("CHN", "JPN"), countryCodes(regionOutput));
        }
    }

    @Test
    void zeroOrNegativeTopNIsRejected() throws Exception {
        try (Connection connection = createTestDatabase()) {
            TopNCountriesByContinentReport continentReport = new TopNCountriesByContinentReport();
            TopNCountriesByRegionReport regionReport = new TopNCountriesByRegionReport();
            assertThrows(IllegalArgumentException.class, () -> continentReport.display(connection, "Asia", 0));
            assertThrows(IllegalArgumentException.class, () -> regionReport.display(connection, "Eastern Asia", -2));
        }
    }

    @Test
    void blankOrNullFiltersAreRejected() throws Exception {
        try (Connection connection = createTestDatabase()) {
            TopNCountriesByContinentReport continentReport = new TopNCountriesByContinentReport();
            TopNCountriesByRegionReport regionReport = new TopNCountriesByRegionReport();
            assertThrows(IllegalArgumentException.class, () -> continentReport.display(connection, "  ", 3));
            assertThrows(IllegalArgumentException.class, () -> continentReport.display(connection, null, 3));
            assertThrows(IllegalArgumentException.class, () -> regionReport.display(connection, "  ", 3));
            assertThrows(IllegalArgumentException.class, () -> regionReport.display(connection, null, 3));
        }
    }

    @Test
    void unknownContinentOrRegionDisplaysNoResults() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String continentOutput = captureOutput(() ->
                    new TopNCountriesByContinentReport().display(connection, "Atlantis", 3));
            String regionOutput = captureOutput(() ->
                    new TopNCountriesByRegionReport().display(connection, "Atlantis", 3));
            assertTrue(countryCodes(continentOutput).isEmpty());
            assertTrue(countryCodes(regionOutput).isEmpty());
            assertTrue(continentOutput.contains("No results found."));
            assertTrue(regionOutput.contains("No results found."));
        }
    }
}
