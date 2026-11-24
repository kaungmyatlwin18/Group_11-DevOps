import com.napier.sem.AutoDisplay;
import com.napier.sem.report.population.DistrictPopulationQuery;
import com.napier.sem.report.population.DistrictPopulationQuery.PopulationRow;
import com.napier.sem.report.population.DistrictPopulationReport;
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

/** Issue #33: H2 fixtures compatible with the other report integration tests. */
public class DistrictPopulationReportTest {
    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:district_population_" + UUID.randomUUID() + ";MODE=MySQL");
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
                    (10,'Long City','JPN','Tokyo-to',3000000000),
                    (11,'Los Angeles','USA','California',3000000),
                    (12,'San Diego','USA','California',1000000),
                    (13,'Example City','CAN','California',1234),
                    (14,'Zero Place','CAN','Zero District',0)
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

    private List<PopulationRow> population(Connection c, String district, String country) throws SQLException {
        return new DistrictPopulationQuery().findPopulation(c, district, country);
    }

    private String capture(Connection c, String district, String country) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            new DistrictPopulationReport().display(c, district, country);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test void sumsAllCitiesInSelectedDistrict() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(new PopulationRow("Ontario", "Canada", 305000L)), population(c, "Ontario", null));
        }
    }
    @Test void excludesOtherDistrictsAndCountryPopulation() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(new PopulationRow("Illinois", "United States", 110000L)), population(c, "Illinois", null));
        }
    }
    @Test void matchesDistrictIgnoringCaseAndWhitespace() throws SQLException {
        try (Connection c = database()) {
            assertEquals(population(c, "Ontario", null), population(c, "  oNtArIo  ", null));
        }
    }
    @Test void separatesSameNamedDistrictsByCountry() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(new PopulationRow("California", "Canada", 1234L),
                    new PopulationRow("California", "United States", 4000000L)), population(c, "California", null));
        }
    }
    @Test void filtersByCountryCode() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(new PopulationRow("California", "United States", 4000000L)), population(c, "California", " usa "));
        }
    }
    @Test void filtersByCountryName() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(new PopulationRow("California", "Canada", 1234L)), population(c, "California", " canada "));
        }
    }
    @Test void unknownDistrictOrCountryReturnsNoRows() throws SQLException {
        try (Connection c = database()) {
            assertTrue(population(c, "Unknown District", null).isEmpty());
            assertTrue(population(c, "California", "JPN").isEmpty());
        }
    }
    @Test void preservesZeroPopulation() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(new PopulationRow("Zero District", "Canada", 0L)), population(c, "Zero District", null));
        }
    }
    @Test void supportsTotalsLargerThanIntegerRange() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(new PopulationRow("Tokyo-to", "Japan", 3008000000L)), population(c, "Tokyo-to", null));
        }
    }
    @Test void rejectsBlankOrNullDistrict() throws SQLException {
        try (Connection c = database()) {
            assertThrows(IllegalArgumentException.class, () -> population(c, "  ", null));
            assertThrows(IllegalArgumentException.class, () -> population(c, null, null));
        }
    }
    @Test void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> population(null, "Ontario", null));
    }
    @Test void treatsSqlLikeInputAsLiteral() throws SQLException {
        try (Connection c = database()) {
            assertTrue(population(c, "Ontario' OR '1'='1", null).isEmpty());
            assertTrue(population(c, "California", "USA' OR '1'='1").isEmpty());
        }
    }
    @Test void displaysDistrictCountryAndPopulation() throws SQLException {
        try (Connection c = database()) {
            String output = capture(c, "California", "USA");
            assertTrue(output.contains("Population of district: California (USA)"), output);
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("District\\s+Country\\s+Population")), output);
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("California\\s+United States\\s+4000000")), output);
            assertFalse(output.contains("Canada"), output);
        }
    }
    @Test void displaysNoResultsAndAmbiguityClearly() throws SQLException {
        try (Connection c = database()) {
            assertTrue(capture(c, "Unknown District", null).contains("No results found."));
            assertTrue(capture(c, "California", null).contains("Multiple countries have this district name"));
        }
    }
    @Test void readsConfiguredDistrictAndCountry() {
        String oldDistrict = System.getProperty("population.district");
        String oldCountry = System.getProperty("population.district.country");
        try {
            System.setProperty("population.district", "  Ontario  ");
            System.setProperty("population.district.country", " CAN ");
            assertEquals("Ontario", DistrictPopulationReport.configuredDistrict());
            assertEquals("CAN", DistrictPopulationReport.configuredCountry());
        } finally {
            restoreProperty("population.district", oldDistrict);
            restoreProperty("population.district.country", oldCountry);
        }
    }
    private static void restoreProperty(String key, String previous) {
        if (previous == null) System.clearProperty(key);
        else System.setProperty(key, previous);
    }
    @Test void autoDisplayIncludesIssue33WithConfiguredDistrictAndCountry() throws SQLException {
        String oldDistrict = System.getProperty("population.district");
        String oldCountry = System.getProperty("population.district.country");
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Connection c = database(); PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setProperty("population.district", "California");
            System.setProperty("population.district.country", "USA");
            System.setOut(output);
            AutoDisplay.displayAllReports(c);
        } finally {
            System.setOut(original);
            restoreProperty("population.district", oldDistrict);
            restoreProperty("population.district.country", oldCountry);
        }
        String output = bytes.toString(StandardCharsets.UTF_8);
        String heading = "===== ISSUE #33 =====";
        int start = output.indexOf(heading);
        assertTrue(start >= 0, "Missing Issue #33 report: " + output);
        int end = output.indexOf("\n===== ISSUE #", start + heading.length());
        if (end < 0) end = output.indexOf("\n========== AVAILABLE REPORTS COMPLETED ==========", start + heading.length());
        String section = output.substring(start, end < 0 ? output.length() : end);
        assertTrue(section.contains("Population of district: California (USA)"), section);
        assertTrue(section.lines().anyMatch(line -> line.trim().matches("California\\s+United States\\s+4000000")), section);
        assertFalse(section.contains("Canada"), section);
        assertTrue(output.contains("AVAILABLE REPORTS COMPLETED"));
    }
}
