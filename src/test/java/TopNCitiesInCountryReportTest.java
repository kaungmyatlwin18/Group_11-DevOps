import com.napier.sem.report.city.CityReport;
import com.napier.sem.report.city.TopNCitiesInCountryQuery;
import com.napier.sem.report.city.TopNCitiesInCountryReport;
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

/** Issue #18: H2 tests of country selection, row limits, order and display. */
public class TopNCitiesInCountryReportTest {
    private Connection createDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:top_country_cities_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100) NOT NULL, Region VARCHAR(100) NOT NULL)
                    """);
            statement.execute("""
                    CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100) NOT NULL,
                        CountryCode VARCHAR(3) NOT NULL, District VARCHAR(100),
                        Population BIGINT NOT NULL)
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name, Region) VALUES
                    ('MMR', 'Myanmar', 'Southeast Asia'),
                    ('JPN', 'Japan', 'Eastern Asia'),
                    ('CHN', 'China', 'Eastern Asia'),
                    ('KOR', 'South Korea', 'Eastern Asia'),
                    ('GBR', 'United Kingdom', 'British Islands'),
                    ('THA', 'Thailand', 'Southeast Asia')
                    """);
            // Synthetic fixture deliberately inserted out of population order.
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (1, 'Mandalay', 'MMR', 'Mandalay', 885300),
                    (2, 'Tokyo', 'JPN', 'Tokyo-to', 7980230),
                    (3, 'London', 'GBR', 'England', 7285000),
                    (4, 'Rangoon', 'MMR', 'Rangoon', 3361700),
                    (5, 'Shanghai', 'CHN', 'Shanghai', 9696300),
                    (6, 'Bago', 'MMR', 'Bago', 190000),
                    (7, 'Seoul', 'KOR', 'Seoul', 9981619),
                    (8, 'Bangkok', 'THA', 'Bangkok', 6320174),
                    (9, 'Yokohama', 'JPN', 'Kanagawa', 3339594),
                    (10, 'Osaka', 'JPN', 'Osaka', 2595674),
                    (11, 'Nagoya', 'JPN', 'Aichi', 2154376)
                    """);
            return connection;
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }

    private List<CityReport.CityRow> fetch(Connection connection, String country, int n)
            throws SQLException {
        return new TopNCitiesInCountryQuery().findTopNCities(connection, country, n);
    }

    private List<String> names(Connection connection, String country, int n) throws SQLException {
        return fetch(connection, country, n).stream().map(CityReport.CityRow::name).toList();
    }

    private String captureReport(Connection connection, String country, int n) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(stream);
            new TopNCitiesInCountryReport().display(connection, country, n);
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsTopThreeCitiesInJapan() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of("Tokyo", "Yokohama", "Osaka"), names(c, "Japan", 3));
        }
    }

    @Test
    void changingCountryChangesResults() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of("Rangoon", "Mandalay"), names(c, "Myanmar", 2));
        }
    }

    @Test
    void excludesOtherCountriesEvenWithinSameRegion() throws Exception {
        try (Connection c = createDatabase()) {
            var cities = fetch(c, "Japan", 10);
            assertEquals(4, cities.size());
            assertTrue(cities.stream().allMatch(row -> row.country().equals("Japan")));
            assertFalse(names(c, "Japan", 10).contains("Shanghai"));
            assertFalse(names(c, "Japan", 10).contains("Seoul"));
        }
    }

    @Test
    void sortsByPopulationDescending() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of(7980230L, 3339594L, 2595674L, 2154376L),
                    fetch(c, "Japan", 10).stream().map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void mapsAllFourRequiredFields() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(new CityReport.CityRow("Tokyo", "Japan", "Tokyo-to", 7980230L),
                    fetch(c, "Japan", 1).get(0));
        }
    }

    @Test
    void supportsNEqualToOne() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of("Tokyo"), names(c, "Japan", 1));
        }
    }

    @Test
    void returnsExactlyNWhenEnoughCitiesExist() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(2, fetch(c, "Japan", 2).size());
        }
    }

    @Test
    void returnsAllMatchesWhenNExceedsAvailable() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(4, fetch(c, "Japan", 1000).size());
        }
    }

    @Test
    void trimsCountryWhitespace() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of("Tokyo", "Yokohama"), names(c, "  Japan  ", 2));
        }
    }

    @Test
    void rejectsNullOrBlankCountry() throws Exception {
        try (Connection c = createDatabase()) {
            for (String input : new String[]{null, "", "   "}) {
                assertThrows(IllegalArgumentException.class, () -> fetch(c, input, 3));
            }
        }
    }

    @Test
    void rejectsZeroAndNegativeN() throws Exception {
        try (Connection c = createDatabase()) {
            assertThrows(IllegalArgumentException.class, () -> fetch(c, "Japan", 0));
            assertThrows(IllegalArgumentException.class, () -> fetch(c, "Japan", -1));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> fetch(null, "Japan", 3));
    }

    @Test
    void closedConnectionThrowsSQLException() throws Exception {
        Connection c = createDatabase();
        c.close();
        assertThrows(SQLException.class, () -> fetch(c, "Japan", 3));
    }

    @Test
    void unknownCountryReturnsNoRows() throws Exception {
        try (Connection c = createDatabase()) {
            assertTrue(fetch(c, "Atlantis", 3).isEmpty());
        }
    }

    @Test
    void emptyCityTableReturnsNoRows() throws Exception {
        try (Connection c = createDatabase(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM city");
            assertTrue(fetch(c, "Japan", 3).isEmpty());
        }
    }

    @Test
    void tiesSortByNameThenId() throws Exception {
        try (Connection c = createDatabase(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                    INSERT INTO city VALUES
                    (20, 'Zulu', 'JPN', 'Tie', 2595674),
                    (21, 'Alpha', 'JPN', 'Tie', 2595674),
                    (22, 'Osaka', 'JPN', 'Second', 2595674)
                    """);
            var cities = fetch(c, "Japan", 7);
            assertEquals(List.of("Tokyo", "Yokohama", "Alpha", "Osaka", "Osaka", "Zulu", "Nagoya"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals("Osaka", cities.get(3).district());
            assertEquals("Second", cities.get(4).district());
        }
    }

    @Test
    void displaysCountryAndRequiredColumns() throws Exception {
        try (Connection c = createDatabase()) {
            String output = captureReport(c, "Japan", 3);
            assertTrue(output.contains("Top 3 cities in country: Japan"));
            for (String header : List.of("Name", "Country", "District", "Population")) {
                assertTrue(output.contains(header), "Missing header: " + header);
            }
            assertTrue(output.contains("Tokyo-to"));
            assertTrue(output.contains("7980230"));
            assertTrue(output.indexOf("Tokyo") < output.indexOf("Yokohama"));
            assertFalse(output.contains("Shanghai"));
        }
    }

    @Test
    void reportShowsOnlyNRequestedRows() throws Exception {
        try (Connection c = createDatabase()) {
            String output = captureReport(c, "Japan", 1);
            assertTrue(output.contains("Tokyo"));
            assertFalse(output.contains("Yokohama"));
            assertFalse(output.contains("Osaka"));
        }
    }

    @Test
    void reportShowsNoResultsForUnknownCountry() throws Exception {
        try (Connection c = createDatabase()) {
            assertTrue(captureReport(c, "Atlantis", 3).contains("No results found."));
        }
    }

    @Test
    void zeroPopulationAndNullDistrictAreHandled() throws Exception {
        try (Connection c = createDatabase(); Statement s = c.createStatement()) {
            s.executeUpdate("INSERT INTO city VALUES (30, 'ZeroCity', 'JPN', NULL, 0)");
            var last = fetch(c, "Japan", 10).get(4);
            assertEquals("ZeroCity", last.name());
            assertNull(last.district());
            assertEquals(0L, last.population());
            assertTrue(captureReport(c, "Japan", 10).contains("N/A"));
        }
    }

    @Test
    void sqlInjectionInputIsTreatedAsText() throws Exception {
        try (Connection c = createDatabase()) {
            assertTrue(fetch(c, "Japan' OR '1'='1", 3).isEmpty());
        }
    }

    @Test
    void configuredTopNAcceptsPositiveValuesAndRejectsInvalidValues() {
        String key = "city.topN.country";
        String old = System.getProperty(key);
        try {
            System.setProperty(key, " 7 ");
            assertEquals(7, TopNCitiesInCountryReport.configuredTopN());
            for (String input : List.of("seven", "0", "-1")) {
                System.setProperty(key, input);
                assertThrows(IllegalArgumentException.class, TopNCitiesInCountryReport::configuredTopN);
            }
        } finally {
            restoreProperty(key, old);
        }
    }

    @Test
    void configuredCountryAcceptsFullNameIncludingSpaces() throws Exception {
        String key = "city.country";
        String old = System.getProperty(key);
        try (Connection c = createDatabase()) {
            System.setProperty(key, "  United Kingdom  ");
            assertEquals("United Kingdom", TopNCitiesInCountryReport.configuredCountry());
            assertEquals(List.of("London"), names(c, TopNCitiesInCountryReport.configuredCountry(), 3));
        } finally {
            restoreProperty(key, old);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) System.clearProperty(key);
        else System.setProperty(key, value);
    }
}
