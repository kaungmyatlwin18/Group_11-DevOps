import com.napier.sem.report.population.SelectedRegionPopulationQuery;
import com.napier.sem.report.population.SelectedRegionPopulationReport;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** H2/JUnit tests for Issue #31; no live MySQL server is required. */
public class SelectedRegionPopulationReportTest {

    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:region_total_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100),
                        Region VARCHAR(50),
                        Population BIGINT
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name, Region, Population) VALUES
                    ('MMR', 'Myanmar', 'Southeast Asia', 50000000),
                    ('THA', 'Thailand', 'Southeast Asia', 70000000),
                    ('JPN', 'Japan', 'Eastern Asia', 120000000),
                    ('CHN', 'China', 'Eastern Asia', 1400000000),
                    ('FRA', 'France', 'Western Europe', 65000000)
                    """);
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    private Connection emptyDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:region_total_empty_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, "
                    + "Name VARCHAR(100), Region VARCHAR(50), Population BIGINT)");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    private long population(Connection connection, String region) throws SQLException {
        return new SelectedRegionPopulationQuery().findRegionPopulation(connection, region);
    }

    private String capture(Connection connection, String region) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            new SelectedRegionPopulationReport().display(connection, region);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void sumsPopulationForSelectedRegion() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(120000000L, population(connection, "Southeast Asia"));
        }
    }

    @Test
    void sumsPopulationForAnotherRegion() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(1520000000L, population(connection, "Eastern Asia"));
        }
    }

    @Test
    void matchesRegionCaseInsensitively() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(120000000L, population(connection, "sOuThEaSt aSiA"));
        }
    }

    @Test
    void trimsRegionInput() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(120000000L, population(connection, "  Southeast Asia  "));
        }
    }

    @Test
    void excludesCountriesFromOtherRegions() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(120000000L, population(connection, "Southeast Asia"));
            assertNotEquals(1520000000L, population(connection, "Southeast Asia"));
        }
    }

    @Test
    void returnsZeroForUnknownRegion() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(0L, population(connection, "Atlantis"));
        }
    }

    @Test
    void returnsZeroForEmptyCountryTable() throws SQLException {
        try (Connection connection = emptyDatabase()) {
            assertEquals(0L, population(connection, "Southeast Asia"));
        }
    }

    @Test
    void ignoresNullPopulationValues() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE country SET Population = NULL WHERE Code = 'THA'");
            assertEquals(50000000L, population(connection, "Southeast Asia"));
        }
    }

    @Test
    void returnsZeroWhenAllSelectedRegionPopulationsAreNull() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE country SET Population = NULL WHERE Region = 'Southeast Asia'");
            assertEquals(0L, population(connection, "Southeast Asia"));
        }
    }

    @Test
    void preservesZeroPopulationValues() throws SQLException {
        try (Connection connection = emptyDatabase(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO country (Code, Name, Region, Population) "
                    + "VALUES ('AAA', 'Zero Country', 'Southeast Asia', 0)");
            assertEquals(0L, population(connection, "Southeast Asia"));
        }
    }

    @Test
    void supportsTotalsAboveIntegerRange() throws SQLException {
        try (Connection connection = emptyDatabase(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO country (Code, Name, Region, Population) VALUES "
                    + "('AAA', 'Large A', 'Southeast Asia', 3000000000), "
                    + "('BBB', 'Large B', 'Southeast Asia', 3000000000)");
            assertEquals(6000000000L, population(connection, "Southeast Asia"));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> population(null, "Southeast Asia"));
    }

    @Test
    void rejectsNullRegion() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(NullPointerException.class, () -> population(connection, null));
        }
    }

    @Test
    void rejectsBlankRegion() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(IllegalArgumentException.class, () -> population(connection, "   "));
        }
    }

    @Test
    void queryDoesNotRequireCityTable() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(120000000L, population(connection, "Southeast Asia"));
        }
    }

    @Test
    void reportDisplaysSelectedRegionAndPopulation() throws SQLException {
        try (Connection connection = database()) {
            String output = capture(connection, "Southeast Asia");
            assertTrue(output.contains("Population of Region: Southeast Asia"), output);
            assertTrue(output.contains("Region"), output);
            assertTrue(output.contains("Population"), output);
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Southeast Asia\\s+120000000")), output);
        }
    }

    @Test
    void reportDisplaysZeroForUnknownRegion() throws SQLException {
        try (Connection connection = database()) {
            String output = capture(connection, "Atlantis");
            assertTrue(output.contains("Population of Region: Atlantis"), output);
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Atlantis\\s+0")), output);
        }
    }

    @Test
    void reflectsPopulationChangesOnSubsequentCalls() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            assertEquals(120000000L, population(connection, "Southeast Asia"));
            statement.executeUpdate("INSERT INTO country (Code, Name, Region, Population) "
                    + "VALUES ('VNM', 'Vietnam', 'Southeast Asia', 90000000)");
            assertEquals(210000000L, population(connection, "Southeast Asia"));
        }
    }
}
