import com.napier.sem.AutoDisplay;
import com.napier.sem.report.population.CityPopulationQuery;
import com.napier.sem.report.population.CityPopulationQuery.PopulationRow;
import com.napier.sem.report.population.CityPopulationReport;
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

/** Independent H2 fixtures for Issue #34; no live MySQL server is needed. */
public class CityPopulationReportTest {

    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:city_population_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100),
                        Continent VARCHAR(50), Region VARCHAR(100), SurfaceArea DECIMAL(12,2),
                        IndepYear INT, Population BIGINT, LifeExpectancy DECIMAL(5,2),
                        GNP DECIMAL(15,2), GNPOld DECIMAL(15,2), LocalName VARCHAR(100),
                        GovernmentForm VARCHAR(100), HeadOfState VARCHAR(100), Capital INT,
                        Code2 VARCHAR(2))
                    """);
            statement.execute("""
                    CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100), CountryCode VARCHAR(3),
                        District VARCHAR(100), Population BIGINT NOT NULL)
                    """);
            statement.execute("""
                    CREATE TABLE countrylanguage (CountryCode VARCHAR(3), Language VARCHAR(100),
                        IsOfficial VARCHAR(1), Percentage DECIMAL(5,2), PRIMARY KEY(CountryCode, Language))
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name, Continent, Region, Population, Capital) VALUES
                    ('JPN','Japan','Asia','Eastern Asia',120000000,1),
                    ('USA','United States','North America','North America',300000000,6),
                    ('CAN','Canada','North America','North America',30000000,7),
                    ('GBR','United Kingdom','Europe','British Islands',60000000,5),
                    ('MMR','Myanmar','Asia','Southeast Asia',50000000,8)
                    """);
            statement.executeUpdate("""
                    INSERT INTO city VALUES
                    (1,'Tokyo','JPN','Tokyo-to',8000000),
                    (2,'Springfield','USA','Illinois',110000),
                    (3,'Springfield','USA','Missouri',150000),
                    (4,'Springfield','CAN','Ontario',5000),
                    (5,'London','GBR','England',7000000),
                    (6,'Washington','USA','District of Columbia',600000),
                    (7,'Ottawa','CAN','Ontario',300000),
                    (8,'Rangoon','MMR','Rangoon',4000000),
                    (9,'Zero City','JPN','Tokyo-to',0),
                    (10,'Long City','JPN','Tokyo-to',3000000000)
                    """);
            statement.executeUpdate("""
                    INSERT INTO countrylanguage VALUES ('JPN','Japanese','T',99.0),
                    ('USA','English','T',80.0), ('CAN','English','T',60.0),
                    ('GBR','English','T',95.0), ('MMR','Burmese','T',70.0)
                    """);
            return connection;
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
    }

    private List<PopulationRow> population(Connection c, String city, String country) throws SQLException {
        return new CityPopulationQuery().findPopulation(c, city, country);
    }

    private String capture(Connection c, String city, String country) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            new CityPopulationReport().display(c, city, country);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsCorrectCityPopulationNotCountryPopulation() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(new PopulationRow("Tokyo", "Japan", "Tokyo-to", 8000000L)),
                    population(c, "Tokyo", null));
        }
    }

    @Test
    void trimsCityNameAndIgnoresCase() throws SQLException {
        try (Connection c = database()) {
            assertEquals(population(c, "Tokyo", null), population(c, "  tOkYo  ", ""));
        }
    }

    @Test
    void matchesWholeCityNameRatherThanSubstring() throws SQLException {
        try (Connection c = database()) {
            assertTrue(population(c, "Tok", null).isEmpty());
        }
    }

    @Test
    void returnsDuplicateNamesSeparatelyWithoutSummingPopulations() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(
                    new PopulationRow("Springfield", "Canada", "Ontario", 5000L),
                    new PopulationRow("Springfield", "United States", "Illinois", 110000L),
                    new PopulationRow("Springfield", "United States", "Missouri", 150000L)),
                    population(c, "Springfield", null));
        }
    }

    @Test
    void filtersByCountryNameIgnoringCaseAndWhitespace() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(new PopulationRow("Springfield", "Canada", "Ontario", 5000L)),
                    population(c, "Springfield", "  cAnAdA  "));
        }
    }

    @Test
    void filtersByCountryCodeAndRetainsSameCountryDuplicates() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(110000L, 150000L), population(c, "Springfield", "usa")
                    .stream().map(PopulationRow::population).toList());
        }
    }

    @Test
    void unknownCityAndNonMatchingCountryReturnNoRows() throws SQLException {
        try (Connection c = database()) {
            assertTrue(population(c, "Unknown City", null).isEmpty());
            assertTrue(population(c, "Tokyo", "Canada").isEmpty());
        }
    }

    @Test
    void handlesZeroAndLargePopulationValues() throws SQLException {
        try (Connection c = database()) {
            assertEquals(0L, population(c, "Zero City", null).getFirst().population());
            assertEquals(3000000000L, population(c, "Long City", null).getFirst().population());
        }
    }

    @Test
    void rejectsBlankAndNullCity() throws SQLException {
        try (Connection c = database()) {
            assertThrows(IllegalArgumentException.class, () -> population(c, "  ", null));
            assertThrows(IllegalArgumentException.class, () -> population(c, null, null));
        }
    }

    @Test
    void treatsSqlLikeInputsAsParameterValues() throws SQLException {
        try (Connection c = database()) {
            assertTrue(population(c, "Tokyo' OR '1'='1", null).isEmpty());
            assertTrue(population(c, "Tokyo", "Japan' OR '1'='1").isEmpty());
        }
    }

    @Test
    void rejectsNullConnectionAndPropagatesClosedConnectionError() throws SQLException {
        assertThrows(NullPointerException.class, () -> population(null, "Tokyo", null));
        Connection c = database();
        c.close();
        assertThrows(SQLException.class, () -> population(c, "Tokyo", null));
    }

    @Test
    void displaysRequiredCityAndPopulationAndContext() throws SQLException {
        try (Connection c = database()) {
            String output = capture(c, "Tokyo", "JPN");
            assertTrue(output.contains("Population of city: Tokyo (JPN)"));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("City\\s+Country\\s+District\\s+Population")));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Tokyo\\s+Japan\\s+Tokyo-to\\s+8000000")));
            assertFalse(output.contains("London"));
        }
    }

    @Test
    void displaysAmbiguousMatchesClearly() throws SQLException {
        try (Connection c = database()) {
            String output = capture(c, "Springfield", "USA");
            assertTrue(output.contains("Multiple matching cities found."));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Springfield\\s+United States\\s+Illinois\\s+110000")));
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Springfield\\s+United States\\s+Missouri\\s+150000")));
            assertFalse(output.contains("Ontario"));
        }
    }

    @Test
    void displaysNoResultsMessageForUnknownCity() throws SQLException {
        try (Connection c = database()) {
            assertTrue(capture(c, "Unknown City", null).contains("No results found."));
        }
    }

    @Test
    void readsAndTrimsConfiguredCityAndCountry() {
        String oldCity = System.getProperty("population.city");
        String oldCountry = System.getProperty("population.city.country");
        try {
            System.setProperty("population.city", "  London  ");
            System.setProperty("population.city.country", " GBR ");
            assertEquals("London", CityPopulationReport.configuredCity());
            assertEquals("GBR", CityPopulationReport.configuredCountry());
        } finally {
            restoreProperty("population.city", oldCity);
            restoreProperty("population.city.country", oldCountry);
        }
    }

    private static void restoreProperty(String key, String previous) {
        if (previous == null) System.clearProperty(key);
        else System.setProperty(key, previous);
    }

    @Test
    void autoDisplayIncludesIssue34AndUsesConfiguredCityAndCountry() throws SQLException {
        String oldCity = System.getProperty("population.city");
        String oldCountry = System.getProperty("population.city.country");
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Connection c = database(); PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setProperty("population.city", "Springfield");
            System.setProperty("population.city.country", "CAN");
            System.setOut(output);
            AutoDisplay.displayAllReports(c);
        } finally {
            System.setOut(original);
            restoreProperty("population.city", oldCity);
            restoreProperty("population.city.country", oldCountry);
        }
        String output = bytes.toString(StandardCharsets.UTF_8);
        String heading = "===== ISSUE #34 =====";
        int start = output.indexOf(heading);
        assertTrue(start >= 0, "Missing Issue #34 report");
        // Stop only at an actual issue heading or the application completion banner.
        // ReportDisplay also uses equals signs in its own title formatting.
        int end = output.indexOf("\n===== ISSUE #", start + heading.length());
        if (end < 0) {
            end = output.indexOf("\n========== AVAILABLE REPORTS COMPLETED ==========", start + heading.length());
        }
        String section = output.substring(start, end < 0 ? output.length() : end);
        assertTrue(section.contains("Population of city: Springfield (CAN)"));
        assertTrue(section.lines().anyMatch(line -> line.trim().matches("Springfield\\s+Canada\\s+Ontario\\s+5000")));
        assertFalse(section.contains("Illinois"));
        assertTrue(output.contains("AVAILABLE REPORTS COMPLETED"));
    }
}
