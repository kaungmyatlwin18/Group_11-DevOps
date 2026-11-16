import com.napier.sem.report.city.CityReport;
import com.napier.sem.report.city.TopNCitiesInDistrictQuery;
import com.napier.sem.report.city.TopNCitiesInDistrictReport;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Issue #19: H2 tests for district selection, sorting, limits and display. */
public class TopNCitiesInDistrictReportTest {
    private Connection createDatabase() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:h2:mem:district_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100) NOT NULL)");
            s.execute("CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100), CountryCode VARCHAR(3), District VARCHAR(100), Population BIGINT)");
            s.executeUpdate("INSERT INTO country VALUES ('USA', 'United States'), ('CAN', 'Canada'), ('GBR', 'United Kingdom')");
            // Synthetic data, intentionally not in population order.
            s.executeUpdate("""
                INSERT INTO city VALUES
                (1, 'San Jose', 'USA', 'California', 894943),
                (2, 'New York', 'USA', 'New York', 8008278),
                (3, 'Los Angeles', 'USA', 'California', 3694820),
                (4, 'San Diego', 'USA', 'California', 1223400),
                (5, 'San Francisco', 'USA', 'California', 776733),
                (6, 'Toronto', 'CAN', 'Ontario', 2000000),
                (7, 'London', 'GBR', 'Greater London', 7285000)
                """);
            return c;
        } catch (SQLException e) {
            c.close();
            throw e;
        }
    }
    private List<CityReport.CityRow> fetch(Connection c, String district, int n) throws SQLException {
        return new TopNCitiesInDistrictQuery().findTopNCities(c, district, n);
    }
    private List<String> names(Connection c, String district, int n) throws SQLException {
        return fetch(c, district, n).stream().map(CityReport.CityRow::name).toList();
    }
    private String captureReport(Connection c, String district, int n) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(stream);
            new TopNCitiesInDistrictReport().display(c, district, n);
        } finally { System.setOut(original); }
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsTopThreeCitiesInCalifornia() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of("Los Angeles", "San Diego", "San Jose"), names(c, "California", 3));
        }
    }

    @Test
    void changingDistrictChangesResults() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of("Toronto"), names(c, "Ontario", 5));
        }
    }

    @Test
    void excludesOtherDistrictsEvenWithinSameCountry() throws Exception {
        try (Connection c = createDatabase()) {
            var cities = fetch(c, "California", 10);
            assertEquals(4, cities.size());
            assertTrue(cities.stream().allMatch(row -> row.district().equals("California")));
            assertFalse(names(c, "California", 10).contains("New York"));
        }
    }

    @Test
    void sortsByPopulationDescending() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of(3694820L, 1223400L, 894943L, 776733L), fetch(c, "California", 10).stream().map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void mapsAllFourRequiredFields() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(new CityReport.CityRow("Los Angeles", "United States", "California", 3694820L), fetch(c, "California", 1).get(0));
        }
    }

    @Test
    void supportsNEqualToOne() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of("Los Angeles"), names(c, "California", 1));
        }
    }

    @Test
    void returnsExactlyNWhenEnoughCitiesExist() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(2, fetch(c, "California", 2).size());
        }
    }

    @Test
    void returnsAllMatchesWhenNExceedsAvailable() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(4, fetch(c, "California", 1000).size());
        }
    }

    @Test
    void trimsDistrictWhitespace() throws Exception {
        try (Connection c = createDatabase()) {
            assertEquals(List.of("Los Angeles", "San Diego"), names(c, "  California  ", 2));
        }
    }

    @Test
    void rejectsNullOrBlankDistrict() throws Exception {
        try (Connection c = createDatabase()) {
            for (String input : new String[]{null, "", "   "}) {
                assertThrows(IllegalArgumentException.class, () -> fetch(c, input, 3));
            }
        }
    }

    @Test
    void rejectsZeroAndNegativeN() throws Exception {
        try (Connection c = createDatabase()) {
            assertThrows(IllegalArgumentException.class, () -> fetch(c, "California", 0));
            assertThrows(IllegalArgumentException.class, () -> fetch(c, "California", -1));
        }
    }

    @Test
    void unknownDistrictReturnsNoRows() throws Exception {
        try (Connection c = createDatabase()) {
            assertTrue(fetch(c, "Atlantis", 3).isEmpty());
        }
    }

    @Test
    void emptyCityTableReturnsNoRows() throws Exception {
        try (Connection c = createDatabase()) {
            try (Statement s = c.createStatement()) { s.executeUpdate("DELETE FROM city"); }
            assertTrue(fetch(c, "California", 3).isEmpty());
        }
    }

    @Test
    void tiesSortByNameThenId() throws Exception {
        try (Connection c = createDatabase()) {
            try (Statement s = c.createStatement()) {
                s.executeUpdate("INSERT INTO city VALUES (20, 'Zulu', 'USA', 'California', 894943), (21, 'Alpha', 'USA', 'California', 894943), (22, 'San Jose', 'CAN', 'California', 894943)");
            }
            var cities = fetch(c, "California", 7);
            assertEquals(List.of("Los Angeles", "San Diego", "Alpha", "San Jose", "San Jose", "Zulu", "San Francisco"), cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals("United States", cities.get(3).country());
            assertEquals("Canada", cities.get(4).country());
        }
    }

    @Test
    void displaysDistrictAndRequiredColumns() throws Exception {
        try (Connection c = createDatabase()) {
            String output = captureReport(c, "California", 3);
            assertTrue(output.contains("Top 3 cities in district: California"));
            for (String header : List.of("Name", "Country", "District", "Population")) assertTrue(output.contains(header));
            assertTrue(output.contains("United States"));
            assertTrue(output.contains("3694820"));
            assertTrue(output.indexOf("Los Angeles") < output.indexOf("San Diego"));
            assertFalse(output.contains("New York"));
        }
    }

    @Test
    void reportShowsOnlyNRequestedRows() throws Exception {
        try (Connection c = createDatabase()) {
            String output = captureReport(c, "California", 1);
            assertTrue(output.contains("Los Angeles"));
            assertFalse(output.contains("San Diego"));
        }
    }

    @Test
    void reportShowsNoResultsForUnknownDistrict() throws Exception {
        try (Connection c = createDatabase()) {
            assertTrue(captureReport(c, "Atlantis", 3).contains("No results found."));
        }
    }

    @Test
    void zeroPopulationIsIncludedAndNullDistrictExcluded() throws Exception {
        try (Connection c = createDatabase()) {
            try (Statement s = c.createStatement()) {
                s.executeUpdate("INSERT INTO city VALUES (30, 'ZeroCity', 'USA', 'California', 0), (31, 'MissingDistrict', 'USA', NULL, 99999999)");
            }
            var cities = fetch(c, "California", 10);
            assertEquals(5, cities.size());
            assertEquals("ZeroCity", cities.get(4).name());
            assertEquals(0L, cities.get(4).population());
        }
    }

    @Test
    void sqlInjectionInputIsTreatedAsText() throws Exception {
        try (Connection c = createDatabase()) {
            assertTrue(fetch(c, "California' OR '1'='1", 3).isEmpty());
        }
    }

    @Test
    void configuredDistrictAcceptsFullNameIncludingSpaces() throws Exception {
        try (Connection c = createDatabase()) {
            String key = "city.district", old = System.getProperty(key);
            try {
                System.setProperty(key, "  Greater London  ");
                assertEquals("Greater London", TopNCitiesInDistrictReport.configuredDistrict());
                assertEquals(List.of("London"), names(c, TopNCitiesInDistrictReport.configuredDistrict(), 5));
            } finally { restoreProperty(key, old); }
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> fetch(null, "California", 3));
    }
    @Test
    void closedConnectionThrowsSQLException() throws Exception {
        Connection c = createDatabase();
        c.close();
        assertThrows(SQLException.class, () -> fetch(c, "California", 3));
    }
    @Test
    void configuredTopNAcceptsPositiveValuesAndRejectsInvalidValues() {
        String key = "city.topN.district", old = System.getProperty(key);
        try {
            System.setProperty(key, " 7 ");
            assertEquals(7, TopNCitiesInDistrictReport.configuredTopN());
            for (String value : List.of("seven", "0", "-1")) {
                System.setProperty(key, value);
                assertThrows(IllegalArgumentException.class, TopNCitiesInDistrictReport::configuredTopN);
            }
        } finally { restoreProperty(key, old); }
    }
    private static void restoreProperty(String key, String value) {
        if (value == null) System.clearProperty(key);
        else System.setProperty(key, value);
    }
}
