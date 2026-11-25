import com.napier.sem.report.population.SelectedCountryPopulationQuery;
import com.napier.sem.report.population.SelectedCountryPopulationReport;
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

/** H2/JUnit tests for Issue #32; no live MySQL server is required. */
public class SelectedCountryPopulationReportTest {

    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:country_total_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100),
                        Continent VARCHAR(50),
                        Region VARCHAR(50),
                        Population BIGINT
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name, Continent, Region, Population) VALUES
                    ('MMR', 'Myanmar', 'Asia', 'Southeast Asia', 54000000),
                    ('THA', 'Thailand', 'Asia', 'Southeast Asia', 70000000),
                    ('CAN', 'Canada', 'North America', 'North America', 40000000),
                    ('JPN', 'Japan', 'Asia', 'Eastern Asia', 120000000)
                    """);
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    private Connection emptyDatabase() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:country_total_empty_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, "
                    + "Name VARCHAR(100), Continent VARCHAR(50), Region VARCHAR(50), Population BIGINT)");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    private long population(Connection connection, String country) throws SQLException {
        return new SelectedCountryPopulationQuery().findCountryPopulation(connection, country);
    }

    private String capture(Connection connection, String country) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            new SelectedCountryPopulationReport().display(connection, country);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void returnsPopulationForSelectedCountry() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(54000000L, population(connection, "Myanmar"));
        }
    }

    @Test
    void returnsPopulationForAnotherCountry() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(40000000L, population(connection, "Canada"));
        }
    }

    @Test
    void matchesCountryCaseInsensitively() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(54000000L, population(connection, "mYaNmAr"));
        }
    }

    @Test
    void trimsCountryInput() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(54000000L, population(connection, "  Myanmar  "));
        }
    }

    @Test
    void returnsZeroForUnknownCountry() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(0L, population(connection, "Atlantis"));
        }
    }

    @Test
    void returnsZeroForEmptyCountryTable() throws SQLException {
        try (Connection connection = emptyDatabase()) {
            assertEquals(0L, population(connection, "Myanmar"));
        }
    }

    @Test
    void treatsNullPopulationAsZero() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE country SET Population = NULL WHERE Code = 'MMR'");
            assertEquals(0L, population(connection, "Myanmar"));
        }
    }

    @Test
    void preservesZeroPopulation() throws SQLException {
        try (Connection connection = emptyDatabase(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO country (Code, Name, Continent, Region, Population) "
                    + "VALUES ('AAA', 'Zero Country', 'Asia', 'Test Region', 0)");
            assertEquals(0L, population(connection, "Zero Country"));
        }
    }

    @Test
    void supportsPopulationAboveIntegerRange() throws SQLException {
        try (Connection connection = emptyDatabase(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO country (Code, Name, Continent, Region, Population) "
                    + "VALUES ('BIG', 'Large Country', 'Test', 'Test', 3000000000)");
            assertEquals(3000000000L, population(connection, "Large Country"));
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> population(null, "Myanmar"));
    }

    @Test
    void rejectsNullCountry() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(NullPointerException.class, () -> population(connection, null));
        }
    }

    @Test
    void rejectsBlankCountry() throws SQLException {
        try (Connection connection = database()) {
            assertThrows(IllegalArgumentException.class, () -> population(connection, "   "));
        }
    }

    @Test
    void queryDoesNotRequireCityTable() throws SQLException {
        try (Connection connection = database()) {
            assertEquals(54000000L, population(connection, "Myanmar"));
        }
    }

    @Test
    void reportDisplaysSelectedCountryAndPopulation() throws SQLException {
        try (Connection connection = database()) {
            String output = capture(connection, "Myanmar");
            assertTrue(output.contains("Population of Country: Myanmar"), output);
            assertTrue(output.contains("Country"), output);
            assertTrue(output.contains("Population"), output);
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Myanmar\\s+54000000")), output);
        }
    }

    @Test
    void reportDisplaysZeroForUnknownCountry() throws SQLException {
        try (Connection connection = database()) {
            String output = capture(connection, "Atlantis");
            assertTrue(output.contains("Population of Country: Atlantis"), output);
            assertTrue(output.lines().anyMatch(line -> line.trim().matches("Atlantis\\s+0")), output);
        }
    }

    @Test
    void reflectsPopulationChangesOnSubsequentCalls() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            assertEquals(54000000L, population(connection, "Myanmar"));
            statement.executeUpdate("UPDATE country SET Population = 55000000 WHERE Code = 'MMR'");
            assertEquals(55000000L, population(connection, "Myanmar"));
        }
    }
}
