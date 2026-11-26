import com.napier.sem.AutoDisplay;
import com.napier.sem.report.capital.AllCapitalCitiesQuery.CapitalRow;
import com.napier.sem.report.capital.TopNCapitalCitiesInRegionQuery;
import com.napier.sem.report.capital.TopNCapitalCitiesInRegionReport;
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

/** Independent H2 fixtures for Issue #25; a live MySQL server is not required. */
public class TopNCapitalCitiesInRegionReportTest {

    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:capital_top_region_" + UUID.randomUUID() + ";MODE=MySQL");

        try (Statement statement = connection.createStatement()) {
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

            statement.execute("""
                    CREATE TABLE city (
                        ID INT PRIMARY KEY,
                        Name VARCHAR(100),
                        CountryCode VARCHAR(3),
                        District VARCHAR(100),
                        Population BIGINT NOT NULL
                    )
                    """);

            statement.executeUpdate("""
                    INSERT INTO country VALUES
                    ('AAA', 'Country A', 'Asia', 'Eastern Asia', 9000000, 1),
                    ('BBB', 'Country B', 'Europe', 'Western Europe', 7000000, 3),
                    ('CCC', 'Country C', 'Asia', 'Eastern Asia', 8000000, 5),
                    ('DDD', 'Country D', 'Asia', 'Eastern Asia', 6000000, NULL),
                    ('EEE', 'Country E', 'Asia', 'Eastern Asia', 5000000, 999),
                    ('FFF', 'Country F', 'Asia', 'Eastern Asia', 1000000, 8),
                    ('GGG', 'Country G', 'Asia', 'Southern Asia', 11000000, 9)
                    """);

            statement.executeUpdate("""
                    INSERT INTO city VALUES
                    (1, 'Alpha', 'AAA', 'Central', 100000),
                    (2, 'Noncapital A', 'AAA', 'Central', 5000000),
                    (3, 'Bravo', 'BBB', 'Central', 900000),
                    (4, 'Noncapital B', 'BBB', 'North', 1200000),
                    (5, 'Charlie', 'CCC', 'South', 400000),
                    (6, 'No capital city', 'DDD', 'South', 800000),
                    (7, 'Unlinked city', 'EEE', 'North', 600000),
                    (8, 'Foxtrot', 'FFF', 'Central', 0),
                    (9, 'Golf', 'GGG', 'Central', 1500000)
                    """);

            return connection;
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
    }

    private List<CapitalRow> capitals(
            Connection connection, String region, int n) throws SQLException {
        return new TopNCapitalCitiesInRegionQuery()
                .findTopNCapitals(connection, region, n);
    }

    private List<String> names(
            Connection connection, String region, int n) throws SQLException {
        return capitals(connection, region, n)
                .stream()
                .map(CapitalRow::name)
                .toList();
    }

    private String capture(
            Connection connection, String region, int n) throws SQLException {

        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            new TopNCapitalCitiesInRegionReport().display(connection, region, n);
        } finally {
            System.setOut(original);
        }

        return bytes.toString(StandardCharsets.UTF_8);
    }


    private String captureAllReports(Connection connection) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            AutoDisplay.displayAllReports(connection);
        } finally {
            System.setOut(original);
        }

        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsRequestedTopThreeOnlyFromSelectedRegion() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(
                    List.of("Charlie", "Alpha", "Foxtrot"),
                    names(connection, "Eastern Asia", 3));
        }
    }

    @Test
    void returnsExactlyTwoCapitalsWhenTwoAreRequested() throws SQLException {
        try (Connection connection = database()) {
            List<CapitalRow> result =
                    capitals(connection, "Eastern Asia", 2);

            assertEquals(2, result.size());

            assertEquals(
                    List.of("Charlie", "Alpha"),
                    result.stream()
                            .map(CapitalRow::name)
                            .toList()
            );
        }
    }

    @Test
    void supportsTopOne() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(List.of("Charlie"), names(connection, "Eastern Asia", 1));
        }
    }

    @Test
    void returnsAllAvailableMatchingCapitalsWhenNExceedsCount() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(
                    List.of("Charlie", "Alpha", "Foxtrot"),
                    names(connection, "Eastern Asia", 100));
        }
    }

    @Test
    void filtersWesternEuropeCorrectly() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(List.of("Bravo"), names(connection, "Western Europe", 5));
        }
    }

    @Test
    void filtersSouthernAsiaSeparatelyFromEasternAsia() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(List.of("Golf"), names(connection, "Southern Asia", 5));
            assertFalse(names(connection, "Eastern Asia", 10).contains("Golf"));
        }
    }

    @Test
    void sortsByCapitalCityPopulationNotCountryPopulation() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(
                    List.of(400000L, 100000L, 0L),
                    capitals(connection, "Eastern Asia", 3).stream()
                            .map(CapitalRow::population)
                            .toList());

            assertEquals(
                    List.of("Country C", "Country A", "Country F"),
                    capitals(connection, "Eastern Asia", 3).stream()
                            .map(CapitalRow::country)
                            .toList());
        }
    }

    @Test
    void excludesNonCapitalCitiesAndMissingOrInvalidCapitalReferences() throws SQLException {
        try (Connection connection = database()) {
            List<CapitalRow> rows = capitals(connection, "Eastern Asia", 100);

            assertFalse(rows.stream().anyMatch(row ->
                    row.name().contains("Noncapital")
                            || row.name().equals("No capital city")
                            || row.name().equals("Unlinked city")));
        }
    }

    @Test
    void trimsRegionAndMatchesIgnoringCase() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(
                    names(connection, "Eastern Asia", 10),
                    names(connection, "  eAsTeRn aSiA  ", 10));
        }
    }

    @Test
    void resolvesPopulationTiesByCapitalName() throws SQLException {
        try (Connection connection = database();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                    "UPDATE city SET Population = 400000 WHERE ID = 1");

            assertEquals(
                    List.of("Alpha", "Charlie", "Foxtrot"),
                    names(connection, "Eastern Asia", 10));
        }
    }

    @Test
    void handlesUnknownRegionAndEmptyReport() throws SQLException {
        try (Connection connection = database()) {
            assertTrue(capitals(connection, "Unknown Region", 5).isEmpty());
            assertTrue(capture(connection, "Unknown Region", 5)
                    .contains("No results found."));
        }
    }

    @Test
    void treatsSqlLikeRegionInputAsAValue() throws SQLException {
        try (Connection connection = database()) {
            assertTrue(capitals(connection, "Eastern Asia' OR '1'='1", 10).isEmpty());
        }
    }

    @Test
    void rejectsBlankOrNullRegion() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(IllegalArgumentException.class,
                    () -> capitals(connection, "  ", 5));
            assertThrows(IllegalArgumentException.class,
                    () -> capitals(connection, null, 5));
        }
    }

    @Test
    void rejectsNonPositiveN() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(IllegalArgumentException.class,
                    () -> capitals(connection, "Eastern Asia", 0));
            assertThrows(IllegalArgumentException.class,
                    () -> capitals(connection, "Eastern Asia", -1));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class,
                () -> capitals(null, "Eastern Asia", 5));
    }

    @Test
    void propagatesClosedConnectionError() throws SQLException {
        Connection connection = database();
        connection.close();

        assertThrows(SQLException.class,
                () -> capitals(connection, "Eastern Asia", 5));
    }

    @Test
    void displaysRequiredColumnsAndOnlyRequestedRows() throws SQLException {
        try (Connection connection = database()) {
            String output = capture(connection, "Eastern Asia", 2);

            assertTrue(output.contains("Top 2 capital cities in region: Eastern Asia"));
            assertTrue(output.lines().anyMatch(line ->
                    line.trim().matches("Name\\s+Country\\s+Population")));
            assertTrue(output.lines().anyMatch(line ->
                    line.trim().matches("Charlie\\s+Country C\\s+400000")));
            assertTrue(output.lines().anyMatch(line ->
                    line.trim().matches("Alpha\\s+Country A\\s+100000")));
            assertTrue(output.indexOf("Charlie") < output.indexOf("Alpha"));
            assertFalse(output.contains("Foxtrot"));
            assertFalse(output.contains("District"));
        }
    }

    @Test
    void readsAndTrimsConfiguredTopN() {
        String previous = System.getProperty("capital.topN.region");

        try {
            System.setProperty("capital.topN.region", " 3 ");
            assertEquals(3, TopNCapitalCitiesInRegionReport.configuredTopN());
        } finally {
            if (previous == null) {
                System.clearProperty("capital.topN.region");
            } else {
                System.setProperty("capital.topN.region", previous);
            }
        }
    }

    @Test
    void rejectsInvalidConfiguredTopN() {
        String previous = System.getProperty("capital.topN.region");

        try {
            for (String invalid : List.of("0", "-2", "abc", "2.5", "2147483648")) {
                System.setProperty("capital.topN.region", invalid);
                assertThrows(
                        IllegalArgumentException.class,
                        TopNCapitalCitiesInRegionReport::configuredTopN);
            }
        } finally {
            if (previous == null) {
                System.clearProperty("capital.topN.region");
            } else {
                System.setProperty("capital.topN.region", previous);
            }
        }
    }

    @Test
    void autoDisplayIncludesIssue25AndUsesConfiguredRegionAndN() throws SQLException {
        String previousRegion = System.getProperty("capital.region");
        String previousTopN = System.getProperty("capital.topN.region");

        try (Connection connection = database()) {
            System.setProperty("capital.region", "Western Europe");
            System.setProperty("capital.topN.region", "1");

            String output = captureAllReports(connection);

            assertTrue(output.contains("===== ISSUE #25 ====="));

            int start = output.indexOf("===== ISSUE #25 =====");
            int end = output.indexOf("\n===== ISSUE #", start + 1);
            String section = output.substring(
                    start, end < 0 ? output.length() : end);

            assertTrue(section.contains("Top 1 capital cities in region: Western Europe"));
            assertTrue(section.contains("Bravo"));
            assertFalse(section.contains("Golf"));
            assertFalse(section.contains("Charlie"));
        } finally {
            if (previousRegion == null) {
                System.clearProperty("capital.region");
            } else {
                System.setProperty("capital.region", previousRegion);
            }

            if (previousTopN == null) {
                System.clearProperty("capital.topN.region");
            } else {
                System.setProperty("capital.topN.region", previousTopN);
            }
        }
    }

}
