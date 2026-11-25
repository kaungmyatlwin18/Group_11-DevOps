import com.napier.sem.AutoDisplay;
import com.napier.sem.report.population.WorldPopulationQuery;
import com.napier.sem.report.population.WorldPopulationReport;
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

/** H2 tests for Issue #29; no live MySQL server is required. */
public class WorldPopulationReportTest {

    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:world_population_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100),
                        Continent VARCHAR(50), Region VARCHAR(100), SurfaceArea DECIMAL(12,2),
                        IndepYear INT, Population BIGINT, LifeExpectancy DECIMAL(5,2),
                        GNP DECIMAL(15,2), GNPOld DECIMAL(15,2), LocalName VARCHAR(100),
                        GovernmentForm VARCHAR(100), HeadOfState VARCHAR(100), Capital INT,
                        Code2 VARCHAR(2))
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name, Continent, Region, SurfaceArea, Population, Capital) VALUES
                    ('JPN','Japan','Asia','Eastern Asia',377975,120000000,1),
                    ('USA','United States','North America','North America',9833520,300000000,2),
                    ('CAN','Canada','North America','North America',9984670,30000000,3),
                    ('MMR','Myanmar','Asia','Southeast Asia',676578,50000000,4)
                    """);
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    private Connection emptyDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:world_population_empty_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100), Population BIGINT)");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    private long population(Connection connection) throws SQLException {
        return new WorldPopulationQuery().findWorldPopulation(connection);
    }

    private String capture(Connection connection) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            new WorldPopulationReport().display(connection);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void sumsPopulationAcrossAllCountries() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(500000000L, population(connection));
        }
    }

    @Test
    void returnsZeroWhenCountryTableIsEmpty() throws SQLException {
        try (Connection connection = emptyDatabase()) {
            assertEquals(0L, population(connection));
        }
    }

    @Test
    void ignoresNullPopulationValues() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE country SET Population = NULL WHERE Code = 'CAN'");
            assertEquals(470000000L, population(connection));
        }
    }

    @Test
    void returnsZeroWhenAllPopulationValuesAreNull() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE country SET Population = NULL");
            assertEquals(0L, population(connection));
        }
    }

    @Test
    void preservesZeroPopulationValues() throws SQLException {
        try (Connection connection = emptyDatabase(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO country (Code, Name, Population) VALUES ('AAA','Zero Country',0)");
            assertEquals(0L, population(connection));
        }
    }

    @Test
    void supportsTotalsAboveIntegerRange() throws SQLException {
        try (Connection connection = emptyDatabase(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO country (Code, Name, Population) VALUES "
                    + "('AAA','Large Country A',3000000000), ('BBB','Large Country B',3000000000)");
            assertEquals(6000000000L, population(connection));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> population(null));
    }

    @Test
    void queryDoesNotRequireCityTable() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(500000000L, population(connection));
        }
    }

    @Test
    void reflectsPopulationChangesOnSubsequentCalls() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            assertEquals(500000000L, population(connection));
            statement.executeUpdate("INSERT INTO country (Code, Name, Population) VALUES ('GBR','United Kingdom',60000000)");
            assertEquals(560000000L, population(connection));
        }
    }

    @Test
    void reportDisplaysWorldPopulationClearly() throws SQLException {
        try (Connection connection = database()) {
            String output = capture(connection);
            assertTrue(output.contains("Total Population of the World"), output);
            assertTrue(output.lines().anyMatch(line -> line.contains("500000000")), output);
            assertTrue(output.contains("Population"), output);
        }
    }

    @Test
    void reportDisplaysZeroForEmptyCountryTable() throws SQLException {
        try (Connection connection = emptyDatabase()) {
            String output = capture(connection);
            assertTrue(output.contains("Total Population of the World"), output);
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("0")), output);
        }
    }

    @Test
    void autoDisplayIncludesIssue29WithMinimalCapitalCityTable() throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Connection connection = database();
             PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            // Existing country reports join city to look up capital names.
            // Keep this fixture minimal so city-only reports remain gated off.
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100))");
                statement.executeUpdate("INSERT INTO city (ID, Name) VALUES "
                        + "(1, 'Tokyo'), (2, 'Washington'), (3, 'Ottawa'), (4, 'Yangon')");
            }
            System.setOut(output);
            AutoDisplay.displayAllReports(connection);
        } finally {
            System.setOut(original);
        }
        String output = bytes.toString(StandardCharsets.UTF_8);
        String heading = "===== ISSUE #29 =====";
        int start = output.indexOf(heading);
        assertTrue(start >= 0, output);
        int end = output.indexOf("===== ISSUE #", start + heading.length());
        if (end < 0) {
            end = output.indexOf("========== AVAILABLE REPORTS COMPLETED ==========", start + heading.length());
        }
        String section = output.substring(start, end < 0 ? output.length() : end);
        assertTrue(section.contains("Total Population of the World"), section);
        assertTrue(section.lines().anyMatch(line -> line.trim().matches("500000000")), section);
        assertTrue(output.contains("AVAILABLE REPORTS COMPLETED"), output);
    }
}
