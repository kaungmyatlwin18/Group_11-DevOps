import com.napier.sem.report.city.CityReport;
import com.napier.sem.report.city.TopNCitiesInWorldQuery;
import com.napier.sem.report.city.TopNCitiesInWorldReport;
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

/** Issue #15: in-memory H2 tests; Docker and MySQL are not required. */
public class TopNCitiesInWorldReportTest {

    private Connection createDatabase() throws SQLException {
        String id = UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:top_world_cities_" + id + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE city (
                        ID INT PRIMARY KEY,
                        Name VARCHAR(100) NOT NULL,
                        CountryCode VARCHAR(3) NOT NULL,
                        District VARCHAR(100),
                        Population BIGINT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name) VALUES
                    ('MMR', 'Myanmar'), ('JPN', 'Japan'), ('CHN', 'China'),
                    ('KOR', 'South Korea'), ('GBR', 'United Kingdom'), ('EGY', 'Egypt')
                    """);
            // Deliberately unsorted to detect missing ORDER BY.
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (1, 'Mandalay', 'MMR', 'Mandalay', 885300),
                    (2, 'Tokyo', 'JPN', 'Tokyo-to', 7980230),
                    (3, 'Rangoon', 'MMR', 'Rangoon', 3361700),
                    (4, 'London', 'GBR', 'England', 7285000),
                    (5, 'Shanghai', 'CHN', 'Shanghai', 9696300),
                    (6, 'Osaka', 'JPN', 'Osaka', 2595674),
                    (7, 'Cairo', 'EGY', 'Cairo', 6789479),
                    (8, 'Seoul', 'KOR', 'Seoul', 9981619)
                    """);
        }
        return connection;
    }

    private List<CityReport.CityRow> fetch(Connection connection, int n)
            throws SQLException {
        return new TopNCitiesInWorldQuery().findTopNCities(connection, n);
    }

    private String captureDisplay(Connection connection, int n) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            new TopNCitiesInWorldReport().display(connection, n);
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsExactlyRequestedTopThreeCities() throws Exception {
        try (Connection connection = createDatabase()) {
            List<CityReport.CityRow> result = fetch(connection, 3);
            assertEquals(3, result.size());
            assertEquals(List.of("Seoul", "Shanghai", "Tokyo"),
                    result.stream().map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void allowsUserToSelectOneCity() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals("Seoul", fetch(connection, 1).get(0).name());
            assertEquals(1, fetch(connection, 1).size());
        }
    }

    @Test
    void sortsCitiesFromLargestToSmallestPopulation() throws Exception {
        try (Connection connection = createDatabase()) {
            List<Long> populations = fetch(connection, 8).stream()
                    .map(CityReport.CityRow::population).toList();
            assertEquals(List.of(9981619L, 9696300L, 7980230L, 7285000L,
                    6789479L, 3361700L, 2595674L, 885300L), populations);
        }
    }

    @Test
    void returnsNameCountryDistrictAndPopulation() throws Exception {
        try (Connection connection = createDatabase()) {
            CityReport.CityRow largest = fetch(connection, 1).get(0);
            assertEquals("Seoul", largest.name());
            assertEquals("South Korea", largest.country());
            assertEquals("Seoul", largest.district());
            assertEquals(9981619L, largest.population());
        }
    }

    @Test
    void returnsAllAvailableCitiesWhenNMatchesOrExceedsTotal() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(8, fetch(connection, 8).size());
            assertEquals(8, fetch(connection, 100).size());
        }
    }

    @Test
    void breaksPopulationTiesByCityNameThenId() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (9, 'Alpha', 'JPN', 'First', 7285000),
                    (10, 'London', 'GBR', 'Second', 7285000)
                    """);
            List<CityReport.CityRow> result = fetch(connection, 6);
            assertEquals(List.of("Seoul", "Shanghai", "Tokyo", "Alpha", "London", "London"),
                    result.stream().map(CityReport.CityRow::name).toList());
            assertEquals("England", result.get(4).district());
            assertEquals("Second", result.get(5).district());
        }
    }

    @Test
    void rejectsZeroAndNegativeN() throws Exception {
        try (Connection connection = createDatabase()) {
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, 0));
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, -3));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> fetch(null, 3));
    }

    @Test
    void closedConnectionRaisesSQLException() throws Exception {
        Connection connection = createDatabase();
        connection.close();
        assertThrows(SQLException.class, () -> fetch(connection, 3));
    }

    @Test
    void emptyCityTableReturnsEmptyList() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM city");
            assertTrue(fetch(connection, 3).isEmpty());
        }
    }

    @Test
    void displayIncludesHeadingsAndOnlyTopNRows() throws Exception {
        try (Connection connection = createDatabase()) {
            String output = captureDisplay(connection, 3);
            assertTrue(output.contains("Top 3 cities in the world by population"));
            for (String header : List.of("Name", "Country", "District", "Population")) {
                assertTrue(output.contains(header), "Missing heading: " + header);
            }
            assertTrue(output.contains("Seoul"));
            assertTrue(output.contains("South Korea"));
            assertTrue(output.contains("Tokyo-to"));
            assertTrue(output.contains("9981619"));
            assertTrue(output.indexOf("Seoul") < output.indexOf("Shanghai"));
            assertFalse(output.contains("London"), "N=3 must not show the fourth city");
            assertFalse(output.contains("Mandalay"), "N=3 must not show the last city");
        }
    }

    @Test
    void emptyReportDisplaysNoResultsMessage() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM city");
            assertTrue(captureDisplay(connection, 3).contains("No results found."));
        }
    }

    @Test
    void configuredTopNReadsUserSuppliedProperty() {
        String old = System.getProperty("city.topN.world");
        try {
            System.setProperty("city.topN.world", " 7 ");
            assertEquals(7, TopNCitiesInWorldReport.configuredTopN());
        } finally {
            restoreProperty(old);
        }
    }

    @Test
    void configuredTopNRejectsTextAndDecimalValues() {
        String old = System.getProperty("city.topN.world");
        try {
            System.setProperty("city.topN.world", "five");
            assertThrows(IllegalArgumentException.class,
                    TopNCitiesInWorldReport::configuredTopN);
            System.setProperty("city.topN.world", "3.5");
            assertThrows(IllegalArgumentException.class,
                    TopNCitiesInWorldReport::configuredTopN);
        } finally {
            restoreProperty(old);
        }
    }

    @Test
    void configuredTopNRejectsNonPositiveValues() {
        String old = System.getProperty("city.topN.world");
        try {
            System.setProperty("city.topN.world", "0");
            assertThrows(IllegalArgumentException.class,
                    TopNCitiesInWorldReport::configuredTopN);
            System.setProperty("city.topN.world", "-2");
            assertThrows(IllegalArgumentException.class,
                    TopNCitiesInWorldReport::configuredTopN);
        } finally {
            restoreProperty(old);
        }
    }

    private static void restoreProperty(String originalValue) {
        if (originalValue == null) {
            System.clearProperty("city.topN.world");
        } else {
            System.setProperty("city.topN.world", originalValue);
        }
    }
}
