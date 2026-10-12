import com.napier.sem.AutoDisplay;
import com.napier.sem.report.country.TopNCountriesInWorldReport;
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

/** Issue #7 JUnit 5/H2 tests; no Docker/MySQL server is needed. */
public class TopNCountriesInWorldReportTest {

    private Connection createTestDatabase() throws SQLException {
        String name = "world_topn_" + UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + name + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100))");
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100),
                        Continent VARCHAR(50),
                        Region VARCHAR(100),
                        Population BIGINT,
                        Capital INT
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO city VALUES
                    (1, 'Peking'), (2, 'New Delhi'), (3, 'Washington'),
                    (4, 'Jakarta'), (5, 'Tokyo'), (6, 'Seoul')
                    """);
            statement.executeUpdate("""
                    INSERT INTO country VALUES
                    ('USA', 'United States', 'North America', 'North America', 940, 3),
                    ('KOR', 'South Korea', 'Asia', 'Eastern Asia', 850, 6),
                    ('CHN', 'China', 'Asia', 'Eastern Asia', 1000, 1),
                    ('JPN', 'Japan', 'Asia', 'Eastern Asia', 850, 5),
                    ('IND', 'India', 'Asia', 'Southern and Central Asia', 950, 2),
                    ('IDN', 'Indonesia', 'Asia', 'Southeast Asia', 900, 4)
                    """);
        }
        return connection;
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private String captureOutput(SqlAction action) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            action.run();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private List<String> countryCodes(String output) {
        return output.lines()
                .filter(line -> line.matches("^(CHN|IND|USA|IDN|JPN|KOR)\\s+.*"))
                .map(line -> line.trim().split("\\s+")[0])
                .toList();
    }

    @Test
    void topThreeCountriesAreSortedAcrossContinents() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new TopNCountriesInWorldReport().display(connection, 3));
            assertEquals(List.of("CHN", "IND", "USA"), countryCodes(output));
            assertTrue(output.contains("TOP 3 COUNTRIES IN THE WORLD"));
            assertFalse(output.contains("Indonesia"));
        }
    }

    @Test
    void displaysAllSixRequiredColumnsAndCapital() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new TopNCountriesInWorldReport().display(connection, 1));
            assertEquals(List.of("CHN"), countryCodes(output));
            for (String column : List.of("Code", "Name", "Continent", "Region", "Population", "Capital")) {
                assertTrue(output.contains(column), "Missing column: " + column);
            }
            assertTrue(output.contains("Peking"));
        }
    }

    @Test
    void equalPopulationUsesCountryCodeForStableOrdering() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new TopNCountriesInWorldReport().display(connection, 5));
            assertEquals(List.of("CHN", "IND", "USA", "IDN", "JPN"), countryCodes(output));
            assertFalse(output.contains("South Korea"));
        }
    }

    @Test
    void nLargerThanCountryCountReturnsAllAvailable() throws Exception {
        try (Connection connection = createTestDatabase()) {
            String output = captureOutput(() ->
                    new TopNCountriesInWorldReport().display(connection, 20));
            assertEquals(List.of("CHN", "IND", "USA", "IDN", "JPN", "KOR"), countryCodes(output));
        }
    }

    @Test
    void zeroOrNegativeTopNIsRejected() throws Exception {
        try (Connection connection = createTestDatabase()) {
            TopNCountriesInWorldReport report = new TopNCountriesInWorldReport();
            assertThrows(IllegalArgumentException.class, () -> report.display(connection, 0));
            assertThrows(IllegalArgumentException.class, () -> report.display(connection, -3));
        }
    }

    @Test
    void nullConnectionIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new TopNCountriesInWorldReport().display(null, 3));
    }

    @Test
    void userCanChooseWorldNFromIntelliJVmOption() throws Exception {
        String previous = System.getProperty("topN.world");
        try (Connection connection = createTestDatabase()) {
            System.setProperty("topN.world", "2");
            String allReports = captureOutput(() -> AutoDisplay.displayAllReports(connection));
            String worldReport = allReports.substring(allReports.indexOf("===== ISSUE #7 ====="));
            assertTrue(worldReport.contains("TOP 2 COUNTRIES IN THE WORLD"));
            assertEquals(List.of("CHN", "IND"), countryCodes(worldReport));
        } finally {
            restoreTopNProperty(previous);
        }
    }

    @Test
    void invalidUserSuppliedWorldNIsRejected() throws Exception {
        String previous = System.getProperty("topN.world");
        try (Connection connection = createTestDatabase()) {
            System.setProperty("topN.world", "abc");
            assertThrows(IllegalArgumentException.class,
                    () -> captureOutput(() -> AutoDisplay.displayAllReports(connection)));
            System.setProperty("topN.world", "0");
            assertThrows(IllegalArgumentException.class,
                    () -> captureOutput(() -> AutoDisplay.displayAllReports(connection)));
        } finally {
            restoreTopNProperty(previous);
        }
    }

    private void restoreTopNProperty(String previous) {
        if (previous == null) {
            System.clearProperty("topN.world");
        } else {
            System.setProperty("topN.world", previous);
        }
    }
}
