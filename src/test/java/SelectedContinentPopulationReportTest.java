import com.napier.sem.report.population.SelectedContinentPopulationQuery;
import com.napier.sem.report.population.SelectedContinentPopulationReport;
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

/** H2/JUnit tests for Issue #30; no live MySQL server is required. */
public class SelectedContinentPopulationReportTest {

    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:continent_total_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100),
                        Continent VARCHAR(50),
                        Population BIGINT
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name, Continent, Population) VALUES
                    ('JPN', 'Japan', 'Asia', 120000000),
                    ('MMR', 'Myanmar', 'Asia', 50000000),
                    ('USA', 'United States', 'North America', 300000000),
                    ('CAN', 'Canada', 'North America', 30000000),
                    ('FRA', 'France', 'Europe', 65000000)
                    """);
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    private Connection emptyDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:continent_total_empty_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, "
                    + "Name VARCHAR(100), Continent VARCHAR(50), Population BIGINT)");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    private long population(Connection connection, String continent) throws SQLException {
        return new SelectedContinentPopulationQuery().findContinentPopulation(connection, continent);
    }

    private String capture(Connection connection, String continent) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            new SelectedContinentPopulationReport().display(connection, continent);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void sumsPopulationForSelectedContinent() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(170000000L, population(connection, "Asia"));
        }
    }

    @Test
    void sumsNorthAmericanCountriesOnly() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(330000000L, population(connection, "North America"));
        }
    }

    @Test
    void matchesContinentCaseInsensitively() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(170000000L, population(connection, "aSiA"));
        }
    }

    @Test
    void trimsContinentInput() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(170000000L, population(connection, "  Asia  "));
        }
    }

    @Test
    void excludesCountriesFromOtherContinents() throws SQLException {
        try (Connection connection = database()) {
            assertNotEquals(500000000L, population(connection, "Asia"));
            assertEquals(170000000L, population(connection, "Asia"));
        }
    }

    @Test
    void returnsZeroForUnknownContinent() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(0L, population(connection, "Atlantis"));
        }
    }

    @Test
    void returnsZeroForEmptyCountryTable() throws SQLException {
        try (Connection connection = emptyDatabase()) {
            assertEquals(0L, population(connection, "Asia"));
        }
    }

    @Test
    void ignoresNullPopulationValues() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE country SET Population = NULL WHERE Code = 'MMR'");
            assertEquals(120000000L, population(connection, "Asia"));
        }
    }

    @Test
    void returnsZeroWhenAllSelectedContinentPopulationsAreNull() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE country SET Population = NULL WHERE Continent = 'Asia'");
            assertEquals(0L, population(connection, "Asia"));
        }
    }

    @Test
    void preservesZeroPopulationValues() throws SQLException {
        try (Connection connection = emptyDatabase(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO country (Code, Name, Continent, Population) "
                    + "VALUES ('AAA', 'Zero Country', 'Asia', 0)");
            assertEquals(0L, population(connection, "Asia"));
        }
    }

    @Test
    void supportsTotalsAboveIntegerRange() throws SQLException {
        try (Connection connection = emptyDatabase(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO country (Code, Name, Continent, Population) VALUES "
                    + "('AAA', 'Large A', 'Asia', 3000000000), ('BBB', 'Large B', 'Asia', 3000000000)");
            assertEquals(6000000000L, population(connection, "Asia"));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> population(null, "Asia"));
    }

    @Test
    void rejectsNullContinent() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(NullPointerException.class, () -> population(connection, null));
        }
    }

    @Test
    void rejectsBlankContinent() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(IllegalArgumentException.class, () -> population(connection, "   "));
        }
    }

    @Test
    void queryDoesNotRequireCityTable() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(170000000L, population(connection, "Asia"));
        }
    }

    @Test
    void reportDisplaysSelectedContinentAndPopulation() throws SQLException {
        try (Connection connection = database()) {
            String output = capture(connection, "Asia");
            assertTrue(output.contains("Population of Continent: Asia"), output);
            assertTrue(output.contains("Continent"), output);
            assertTrue(output.contains("Population"), output);
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Asia\\s+170000000")), output);
        }
    }

    @Test
    void reportDisplaysZeroForUnknownContinent() throws SQLException {
        try (Connection connection = database()) {
            String output = capture(connection, "Atlantis");
            assertTrue(output.contains("Population of Continent: Atlantis"), output);
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Atlantis\\s+0")), output);
        }
    }

    @Test
    void reflectsPopulationChangesOnSubsequentCalls() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            assertEquals(170000000L, population(connection, "Asia"));
            statement.executeUpdate("INSERT INTO country (Code, Name, Continent, Population) "
                    + "VALUES ('IND', 'India', 'Asia', 100000000)");
            assertEquals(270000000L, population(connection, "Asia"));
        }
    }
}
