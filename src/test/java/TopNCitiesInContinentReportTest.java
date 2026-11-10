import com.napier.sem.report.city.CityReport;
import com.napier.sem.report.city.TopNCitiesInContinentQuery;
import com.napier.sem.report.city.TopNCitiesInContinentReport;
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

/** Issue #16: H2 tests for continent filtering, N, sorting and report display. */
public class TopNCitiesInContinentReportTest {

    private Connection createDatabase() throws SQLException {
        String id = UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:top_continent_cities_" + id + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100) NOT NULL,
                        Continent VARCHAR(100) NOT NULL
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
                    INSERT INTO country (Code, Name, Continent) VALUES
                    ('MMR', 'Myanmar', 'Asia'),
                    ('JPN', 'Japan', 'Asia'),
                    ('CHN', 'China', 'Asia'),
                    ('KOR', 'South Korea', 'Asia'),
                    ('GBR', 'United Kingdom', 'Europe'),
                    ('FRA', 'France', 'Europe'),
                    ('EGY', 'Egypt', 'Africa'),
                    ('USA', 'United States', 'North America')
                    """);
            // Intentionally unsorted and with larger populations outside Asia.
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (1, 'Mandalay', 'MMR', 'Mandalay', 885300),
                    (2, 'Tokyo', 'JPN', 'Tokyo-to', 7980230),
                    (3, 'London', 'GBR', 'England', 7285000),
                    (4, 'Rangoon', 'MMR', 'Rangoon', 3361700),
                    (5, 'Shanghai', 'CHN', 'Shanghai', 9696300),
                    (6, 'Paris', 'FRA', 'Ile-de-France', 2125246),
                    (7, 'Seoul', 'KOR', 'Seoul', 9981619),
                    (8, 'Cairo', 'EGY', 'Cairo', 6789479),
                    (9, 'Los Angeles', 'USA', 'California', 3694820),
                    (10, 'Osaka', 'JPN', 'Osaka', 2595674)
                    """);
        }
        return connection;
    }

    private List<CityReport.CityRow> fetch(Connection connection, String continent, int n)
            throws SQLException {
        return new TopNCitiesInContinentQuery().findTopNCities(connection, continent, n);
    }

    private String captureReport(Connection connection, String continent, int n)
            throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(stream);
            new TopNCitiesInContinentReport().display(connection, continent, n);
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsOnlyTopThreeCitiesFromAsia() throws Exception {
        try (Connection connection = createDatabase()) {
            List<CityReport.CityRow> cities = fetch(connection, "Asia", 3);
            assertEquals(List.of("Seoul", "Shanghai", "Tokyo"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals(3, cities.size());
        }
    }

    @Test
    void changingContinentChangesResults() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(List.of("London", "Paris"),
                    fetch(connection, "Europe", 2).stream()
                            .map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void excludesNonmatchingContinentsEvenWhenTheyHaveLargeCities() throws Exception {
        try (Connection connection = createDatabase()) {
            List<CityReport.CityRow> cities = fetch(connection, "Asia", 10);
            assertEquals(6, cities.size());
            assertTrue(cities.stream().noneMatch(city -> city.name().equals("London")));
            assertTrue(cities.stream().noneMatch(city -> city.name().equals("Cairo")));
        }
    }

    @Test
    void returnsLargestPopulationFirst() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(List.of(9981619L, 9696300L, 7980230L, 3361700L,
                            2595674L, 885300L),
                    fetch(connection, "Asia", 10).stream()
                            .map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void reportsNameCountryDistrictAndPopulation() throws Exception {
        try (Connection connection = createDatabase()) {
            CityReport.CityRow first = fetch(connection, "Asia", 1).get(0);
            assertEquals("Seoul", first.name());
            assertEquals("South Korea", first.country());
            assertEquals("Seoul", first.district());
            assertEquals(9981619L, first.population());
        }
    }

    @Test
    void acceptsNEqualToOne() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(List.of("Seoul"),
                    fetch(connection, "Asia", 1).stream()
                            .map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void nGreaterThanMatchingCitiesReturnsAllAvailable() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(6, fetch(connection, "Asia", 1000).size());
        }
    }

    @Test
    void trimsWhitespaceAroundContinent() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(2, fetch(connection, "  Europe  ", 2).size());
        }
    }

    @Test
    void rejectsNullOrBlankContinent() throws Exception {
        try (Connection connection = createDatabase()) {
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, null, 3));
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, "", 3));
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, "  ", 3));
        }
    }

    @Test
    void rejectsZeroAndNegativeN() throws Exception {
        try (Connection connection = createDatabase()) {
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, "Asia", 0));
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, "Asia", -5));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> fetch(null, "Asia", 3));
    }

    @Test
    void closedConnectionThrowsSQLException() throws Exception {
        Connection connection = createDatabase();
        connection.close();
        assertThrows(SQLException.class, () -> fetch(connection, "Asia", 3));
    }

    @Test
    void unknownContinentReturnsEmptyList() throws Exception {
        try (Connection connection = createDatabase()) {
            assertTrue(fetch(connection, "Atlantis", 3).isEmpty());
        }
    }

    @Test
    void emptyCityTableReturnsNoResults() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM city");
            assertTrue(fetch(connection, "Asia", 3).isEmpty());
        }
    }

    @Test
    void equalPopulationSortsByNameThenId() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (20, 'Zulu', 'MMR', 'Tie', 7980230),
                    (21, 'Alpha', 'MMR', 'Tie', 7980230),
                    (22, 'Tokyo', 'JPN', 'Second', 7980230)
                    """);
            List<CityReport.CityRow> cities = fetch(connection, "Asia", 6);
            assertEquals(List.of("Seoul", "Shanghai", "Alpha", "Tokyo", "Tokyo", "Zulu"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals("Tokyo-to", cities.get(3).district());
            assertEquals("Second", cities.get(4).district());
        }
    }

    @Test
    void displayIncludesAllFourColumnsAndOnlyMatchingTopNRows() throws Exception {
        try (Connection connection = createDatabase()) {
            String output = captureReport(connection, "Asia", 3);
            assertTrue(output.contains("Top 3 cities in continent: Asia"));
            for (String heading : List.of("Name", "Country", "District", "Population")) {
                assertTrue(output.contains(heading), "Missing heading: " + heading);
            }
            assertTrue(output.contains("Seoul"));
            assertTrue(output.contains("South Korea"));
            assertTrue(output.contains("Tokyo-to"));
            assertTrue(output.contains("9981619"));
            assertTrue(output.indexOf("Seoul") < output.indexOf("Shanghai"));
            assertFalse(output.contains("London"));
            assertFalse(output.contains("Rangoon"));
        }
    }

    @Test
    void displayHandlesEmptyResult() throws Exception {
        try (Connection connection = createDatabase()) {
            assertTrue(captureReport(connection, "Atlantis", 3)
                    .contains("No results found."));
        }
    }

    @Test
    void nullDistrictAndZeroPopulationAppearInResults() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population)
                    VALUES (30, 'ZeroCity', 'MMR', NULL, 0)
                    """);
            CityReport.CityRow last = fetch(connection, "Asia", 10).get(6);
            assertEquals("ZeroCity", last.name());
            assertNull(last.district());
            assertEquals(0L, last.population());
            String displayed = captureReport(connection, "Asia", 10);
            assertTrue(displayed.contains("ZeroCity"));
            assertTrue(displayed.contains("N/A"));
        }
    }

    @Test
    void sqlInjectionLikeContinentIsTreatedAsPlainText() throws Exception {
        try (Connection connection = createDatabase()) {
            assertTrue(fetch(connection, "Asia' OR '1'='1", 3).isEmpty());
        }
    }

    @Test
    void configuredTopNUsesUserSuppliedNumberAndRejectsInvalidValues() {
        String old = System.getProperty("city.topN.continent");
        try {
            System.setProperty("city.topN.continent", " 7 ");
            assertEquals(7, TopNCitiesInContinentReport.configuredTopN());
            System.setProperty("city.topN.continent", "five");
            assertThrows(IllegalArgumentException.class,
                    TopNCitiesInContinentReport::configuredTopN);
            System.setProperty("city.topN.continent", "0");
            assertThrows(IllegalArgumentException.class,
                    TopNCitiesInContinentReport::configuredTopN);
        } finally {
            restore("city.topN.continent", old);
        }
    }

    @Test
    void configuredContinentReadsSelectedProperty() {
        String old = System.getProperty("city.continent");
        try {
            System.setProperty("city.continent", "  Europe  ");
            assertEquals("Europe", TopNCitiesInContinentReport.configuredContinent());
        } finally {
            restore("city.continent", old);
        }
    }

    private static void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
