import com.napier.sem.report.city.CityReport;
import com.napier.sem.report.city.TopNCitiesInRegionQuery;
import com.napier.sem.report.city.TopNCitiesInRegionReport;
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

/** Issue #17: H2 tests of region selection, limits, sorting, and display. */
public class TopNCitiesInRegionReportTest {

    private Connection createDatabase() throws SQLException {
        String id = UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:top_region_cities_" + id + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100) NOT NULL,
                        Region VARCHAR(100) NOT NULL
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
                    INSERT INTO country (Code, Name, Region) VALUES
                    ('MMR', 'Myanmar', 'Southeast Asia'),
                    ('JPN', 'Japan', 'Eastern Asia'),
                    ('CHN', 'China', 'Eastern Asia'),
                    ('KOR', 'South Korea', 'Eastern Asia'),
                    ('GBR', 'United Kingdom', 'British Islands'),
                    ('FRA', 'France', 'Western Europe'),
                    ('EGY', 'Egypt', 'Northern Africa'),
                    ('USA', 'United States', 'North America')
                    """);
            // Cities intentionally inserted out of population order.
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

    private List<CityReport.CityRow> fetch(Connection connection, String region, int n)
            throws SQLException {
        return new TopNCitiesInRegionQuery().findTopNCities(connection, region, n);
    }

    private String captureReport(Connection connection, String region, int n)
            throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(stream);
            new TopNCitiesInRegionReport().display(connection, region, n);
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsTopThreeCitiesInEasternAsia() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(List.of("Seoul", "Shanghai", "Tokyo"),
                    fetch(connection, "Eastern Asia", 3).stream()
                            .map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void changingRegionChangesResults() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(List.of("Rangoon", "Mandalay"),
                    fetch(connection, "Southeast Asia", 2).stream()
                            .map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void filtersOutCitiesFromOtherRegions() throws Exception {
        try (Connection connection = createDatabase()) {
            List<CityReport.CityRow> cities = fetch(connection, "Eastern Asia", 10);
            assertEquals(4, cities.size());
            assertTrue(cities.stream().noneMatch(c -> c.name().equals("London")));
            assertTrue(cities.stream().noneMatch(c -> c.name().equals("Rangoon")));
            assertTrue(cities.stream().noneMatch(c -> c.name().equals("Cairo")));
        }
    }

    @Test
    void sortsByDescendingPopulation() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(List.of(9981619L, 9696300L, 7980230L, 2595674L),
                    fetch(connection, "Eastern Asia", 10).stream()
                            .map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void mapsAllFourCityFields() throws Exception {
        try (Connection connection = createDatabase()) {
            CityReport.CityRow first = fetch(connection, "Eastern Asia", 1).get(0);
            assertEquals("Seoul", first.name());
            assertEquals("South Korea", first.country());
            assertEquals("Seoul", first.district());
            assertEquals(9981619L, first.population());
        }
    }

    @Test
    void supportsNEqualToOne() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(List.of("Seoul"), fetch(connection, "Eastern Asia", 1)
                    .stream().map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void returnsExactlyNWhenEnoughCitiesExist() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(2, fetch(connection, "Eastern Asia", 2).size());
        }
    }

    @Test
    void returnsAllMatchingCitiesWhenNExceedsAvailable() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(4, fetch(connection, "Eastern Asia", 1000).size());
        }
    }

    @Test
    void trimsRegionWhitespace() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(List.of("Seoul", "Shanghai"),
                    fetch(connection, "  Eastern Asia  ", 2)
                            .stream().map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void rejectsNullAndBlankRegions() throws Exception {
        try (Connection connection = createDatabase()) {
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, null, 3));
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, "", 3));
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, "  ", 3));
        }
    }

    @Test
    void rejectsZeroAndNegativeN() throws Exception {
        try (Connection connection = createDatabase()) {
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, "Eastern Asia", 0));
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, "Eastern Asia", -1));
        }
    }

    @Test
    void rejectsNullDatabaseConnection() {
        assertThrows(NullPointerException.class, () -> fetch(null, "Eastern Asia", 3));
    }

    @Test
    void closedDatabaseConnectionThrowsSQLException() throws Exception {
        Connection connection = createDatabase();
        connection.close();
        assertThrows(SQLException.class, () -> fetch(connection, "Eastern Asia", 3));
    }

    @Test
    void unknownRegionReturnsEmptyResults() throws Exception {
        try (Connection connection = createDatabase()) {
            assertTrue(fetch(connection, "Atlantis", 3).isEmpty());
        }
    }

    @Test
    void noCityRowsReturnsEmptyResults() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM city");
            assertTrue(fetch(connection, "Eastern Asia", 5).isEmpty());
        }
    }

    @Test
    void tiedPopulationsSortByCityNameThenId() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (20, 'Zulu', 'CHN', 'Tie', 7980230),
                    (21, 'Alpha', 'CHN', 'Tie', 7980230),
                    (22, 'Tokyo', 'JPN', 'Second', 7980230)
                    """);
            List<CityReport.CityRow> cities = fetch(connection, "Eastern Asia", 7);
            assertEquals(List.of("Seoul", "Shanghai", "Alpha", "Tokyo", "Tokyo", "Zulu", "Osaka"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals("Tokyo-to", cities.get(3).district());
            assertEquals("Second", cities.get(4).district());
        }
    }

    @Test
    void displaysRegionAndRequiredColumnsInOrder() throws Exception {
        try (Connection connection = createDatabase()) {
            String output = captureReport(connection, "Eastern Asia", 3);
            assertTrue(output.contains("Top 3 cities in region: Eastern Asia"));
            for (String header : List.of("Name", "Country", "District", "Population")) {
                assertTrue(output.contains(header), "Missing header: " + header);
            }
            assertTrue(output.contains("Seoul"));
            assertTrue(output.contains("South Korea"));
            assertTrue(output.contains("Shanghai"));
            assertTrue(output.contains("Tokyo-to"));
            assertTrue(output.contains("9981619"));
            assertTrue(output.indexOf("Seoul") < output.indexOf("Shanghai"));
            assertFalse(output.contains("Rangoon"));
            assertFalse(output.contains("London"));
        }
    }

    @Test
    void reportShowsOnlyRequestedNRows() throws Exception {
        try (Connection connection = createDatabase()) {
            String output = captureReport(connection, "Eastern Asia", 1);
            assertTrue(output.contains("Seoul"));
            assertFalse(output.contains("Shanghai"));
            assertFalse(output.contains("Tokyo"));
        }
    }

    @Test
    void reportDisplaysNoResultsForUnknownRegion() throws Exception {
        try (Connection connection = createDatabase()) {
            assertTrue(captureReport(connection, "Atlantis", 3).contains("No results found."));
        }
    }

    @Test
    void zeroPopulationAndNullDistrictAreHandled() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population)
                    VALUES (30, 'ZeroCity', 'CHN', NULL, 0)
                    """);
            List<CityReport.CityRow> cities = fetch(connection, "Eastern Asia", 10);
            CityReport.CityRow last = cities.get(4);
            assertEquals("ZeroCity", last.name());
            assertNull(last.district());
            assertEquals(0L, last.population());
            String output = captureReport(connection, "Eastern Asia", 10);
            assertTrue(output.contains("ZeroCity"));
            assertTrue(output.contains("N/A"));
        }
    }

    @Test
    void sqlInjectionStringIsTreatedAsText() throws Exception {
        try (Connection connection = createDatabase()) {
            assertTrue(fetch(connection, "Eastern Asia' OR '1'='1", 3).isEmpty());
        }
    }

    @Test
    void userSuppliedTopNPropertyWorksAndInvalidValuesAreRejected() {
        String before = System.getProperty("city.topN.region");
        try {
            System.setProperty("city.topN.region", " 7 ");
            assertEquals(7, TopNCitiesInRegionReport.configuredTopN());
            System.setProperty("city.topN.region", "seven");
            assertThrows(IllegalArgumentException.class, TopNCitiesInRegionReport::configuredTopN);
            System.setProperty("city.topN.region", "0");
            assertThrows(IllegalArgumentException.class, TopNCitiesInRegionReport::configuredTopN);
        } finally {
            restoreProperty("city.topN.region", before);
        }
    }

    @Test
    void userSuppliedRegionPropertyIsApplied() {
        String before = System.getProperty("city.region");
        try {
            System.setProperty("city.region", "  Southeast Asia  ");
            assertEquals("Southeast Asia", TopNCitiesInRegionReport.configuredRegion());
        } finally {
            restoreProperty("city.region", before);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
