import com.napier.sem.AutoDisplay;
import com.napier.sem.report.population.RegionPopulationQuery;
import com.napier.sem.report.population.RegionPopulationQuery.PopulationRow;
import com.napier.sem.report.population.RegionPopulationReport;
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

/** Issue #27: H2 fixtures, independent of the Docker database. */
public class RegionPopulationReportTest {
    private Connection database() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:h2:mem:region_pop_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement s = c.createStatement()) {
            s.execute("""
                    CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100),
                        Continent VARCHAR(50), Region VARCHAR(100), Population BIGINT, Capital INT)
                    """);
            s.execute("""
                    CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100), CountryCode VARCHAR(3),
                        District VARCHAR(100), Population BIGINT NOT NULL)
                    """);
            s.executeUpdate("""
                    INSERT INTO country VALUES
                    ('AAA','Country A','Asia','Eastern Asia',3000000000,1),
                    ('BBB','Country B','Asia','Eastern Asia',1000000000,3),
                    ('CCC','Country C','Europe','Western Europe',1000,4),
                    ('DDD','Country D','Africa','Western Africa',500,NULL),
                    ('EEE','Country E','Antarctica','Antarctica',0,NULL),
                    ('FFF','Country F','Asia','Southeast Asia',1000000000,NULL)
                    """);
            s.executeUpdate("""
                    INSERT INTO city VALUES
                    (1,'Alpha','AAA','Central',1000000000),
                    (2,'Beta','AAA','North',1000000000),
                    (3,'Gamma','BBB','Central',500000000),
                    (4,'Delta','CCC','Central',1000),
                    (5,'Unlinked','ZZZ','North',999)
                    """);
            return c;
        } catch (SQLException error) {
            c.close();
            throw error;
        }
    }

    private List<PopulationRow> rows(Connection c) throws SQLException {
        return new RegionPopulationQuery().findAllRegions(c);
    }

    private PopulationRow row(Connection c, String region) throws SQLException {
        return rows(c).stream().filter(r -> r.region().equals(region)).findFirst().orElseThrow();
    }

    private String capture(Connection c, boolean all) throws SQLException {
        PrintStream previous = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            if (all) AutoDisplay.displayAllReports(c);
            else new RegionPopulationReport().display(c);
        } finally {
            System.setOut(previous);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsEveryRegionInNameOrder() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("Antarctica", "Eastern Asia", "Southeast Asia", "Western Africa", "Western Europe"), rows(c).stream().map(PopulationRow::region).toList());
        }
    }

    @Test
    void avoidsDuplicatingCountryPopulationAndSupportsLongTotals() throws SQLException {
        try (Connection c = database()) {
            assertEquals(4000000000L, row(c, "Eastern Asia").totalPopulation());
            assertEquals(1000000000L, row(c, "Southeast Asia").totalPopulation());
        }
    }

    @Test
    void countsAllRecordedCitiesNotOnlyCapitals() throws SQLException {
        try (Connection c = database()) {
            assertEquals(2500000000L, row(c, "Eastern Asia").cityPopulation());
        }
    }

    @Test
    void calculatesPopulationOutsideRecordedCities() throws SQLException {
        try (Connection c = database()) {
            assertEquals(1500000000L, row(c, "Eastern Asia").nonCityPopulation());
        }
    }

    @Test
    void calculatesPercentagesWithoutIntegerDivision() throws SQLException {
        try (Connection c = database(); Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE city SET Population = 250 WHERE ID = 4");
            PopulationRow europe = row(c, "Western Europe");
            assertEquals(25.0, europe.cityPercentage(), 0.00001);
            assertEquals(75.0, europe.nonCityPercentage(), 0.00001);
            assertEquals(62.5, row(c, "Eastern Asia").cityPercentage(), 0.00001);
            assertEquals(37.5, row(c, "Eastern Asia").nonCityPercentage(), 0.00001);
        }
    }

    @Test
    void includesCountriesAndRegionsWithoutCities() throws SQLException {
        try (Connection c = database()) {
            PopulationRow africa = row(c, "Western Africa");
            assertEquals(500L, africa.totalPopulation());
            assertEquals(0L, africa.cityPopulation());
            assertEquals(500L, africa.nonCityPopulation());
            assertEquals(100.0, africa.nonCityPercentage(), 0.00001);
            assertEquals(0L, row(c, "Southeast Asia").cityPopulation());
        }
    }

    @Test
    void zeroPopulationDoesNotDivideByZero() throws SQLException {
        try (Connection c = database()) {
            PopulationRow zero = row(c, "Antarctica");
            assertEquals(0L, zero.totalPopulation());
            assertEquals(0.0, zero.cityPercentage(), 0.00001);
            assertEquals(0.0, zero.nonCityPercentage(), 0.00001);
        }
    }

    @Test
    void excludesOrphanCitiesAndSupportsEntirelyUrbanRegion() throws SQLException {
        try (Connection c = database()) {
            PopulationRow europe = row(c, "Western Europe");
            assertEquals(1000L, europe.cityPopulation());
            assertEquals(100.0, europe.cityPercentage(), 0.00001);
            assertEquals(0L, europe.nonCityPopulation());
            assertEquals(2500001000L, rows(c).stream().mapToLong(PopulationRow::cityPopulation).sum());
        }
    }

    @Test
    void handlesEmptyCountryTable() throws SQLException {
        try (Connection c = database(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM country");
            assertTrue(rows(c).isEmpty());
            assertTrue(capture(c, false).contains("No results found."));
        }
    }

    @Test
    void rejectsNullConnectionAndPropagatesSqlErrors() throws SQLException {
        assertThrows(NullPointerException.class, () -> rows(null));
        Connection c = database();
        c.close();
        assertThrows(SQLException.class, () -> rows(c));
    }

    @Test
    void displaysAllSixColumnsAndCorrectlyFormattedValues() throws SQLException {
        try (Connection c = database()) {
            String output = capture(c, false);
            for (String header : List.of("Region", "Total Population", "Population in Cities", "% in Cities", "Population not in Cities", "% not in Cities")) {
                assertTrue(output.contains(header));
            }
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Eastern Asia\\s+4,000,000,000\\s+2,500,000,000\\s+62\\.50%\\s+1,500,000,000\\s+37\\.50%")));
            assertFalse(output.contains("NaN"));
            assertFalse(output.contains("Infinity"));
        }
    }

    @Test
    void autoDisplayPreservesKnownReportsAndRunsIssue27() throws SQLException {
        try (Connection c = database(); Statement s = c.createStatement()) {
            s.executeUpdate("UPDATE country SET Population = 2000000000 WHERE Code = 'AAA'");
            String output = capture(c, true);
            for (int issue : new int[]{2,4,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,26,27}) {
                assertTrue(output.contains("===== ISSUE #" + issue + " ====="), "Missing issue " + issue);
            }
            int start = output.indexOf("===== ISSUE #27 =====");
            assertTrue(start >= 0);
            int next = output.indexOf("\n===== ISSUE #", start + 1);
            String section = output.substring(start, next < 0 ? output.length() : next);
            assertTrue(section.contains("Population report for each region"));
            assertTrue(section.contains("3,000,000,000"));
            assertFalse(section.contains("Alpha"));
            assertTrue(output.contains("AVAILABLE REPORTS COMPLETED"));
        }
    }
}
