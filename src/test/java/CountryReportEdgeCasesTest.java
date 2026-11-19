import com.napier.sem.report.country.CountryReport;
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

/** Issue #76: additional country report edge cases using isolated H2 fixtures. */
public class CountryReportEdgeCasesTest {
    private Connection database() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:h2:mem:edge_" + UUID.randomUUID() + ";MODE=MySQL");
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100))");
            s.execute("CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100), Continent VARCHAR(50), Region VARCHAR(100), Population BIGINT, Capital INT)");
            s.executeUpdate("INSERT INTO city VALUES (1, 'Capital A')");
            s.executeUpdate("INSERT INTO country VALUES ('BBB','Beta','Asia','Eastern Asia',3000000000,NULL), ('AAA','Alpha','Asia','Eastern Asia',3000000000,1), ('CCC','Gamma','Asia','Eastern Asia',0,NULL), ('DDD','Delta','Europe','Western Europe',500,99)");
            return c;
        } catch (SQLException ex) {
            c.close();
            throw ex;
        }
    }

    @FunctionalInterface
    private interface SqlAction { void run() throws SQLException; }

    private String capture(SqlAction action) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            action.run();
        } finally { System.setOut(original); }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private List<String> rows(String output) {
        return output.lines().filter(s -> s.matches("^(AAA|BBB|CCC|DDD)\\s+.*")).toList();
    }

    @Test
    void allCountriesRetainMissingCapitalAndLargePopulation() throws SQLException {
        try (Connection c = database()) {
            List<String> data = rows(capture(() -> new CountryReport().displayAllCountries(c)));
            assertEquals(List.of("AAA", "BBB", "DDD", "CCC"),
                    data.stream().map(s -> s.substring(0, 3)).toList());
            assertTrue(data.get(0).contains("3,000,000,000"));
            assertTrue(data.get(0).stripTrailing().endsWith("Capital A"));
            assertTrue(data.get(1).stripTrailing().endsWith("N/A"));
            assertTrue(data.get(2).stripTrailing().endsWith("N/A"));
            assertTrue(data.get(3).matches(".*\\s0\\s+N/A\\s*"));
        }
    }

    @Test
    void continentFilterTrimsSpacesAndKeepsStableTieOrder() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("AAA", "BBB", "CCC"), rows(capture(() ->
                    new CountryReport().displayCountriesInContinent(c, "  Asia  ")))
                    .stream().map(s -> s.substring(0, 3)).toList());
        }
    }

    @Test
    void unknownContinentProducesEmptyResult() throws SQLException {
        try (Connection c = database()) {
            String result = capture(() -> new CountryReport().displayCountriesInContinent(c, "Atlantis"));
            assertTrue(rows(result).isEmpty());
            assertTrue(result.contains("No results found."));
        }
    }

    @Test
    void noCountryRecordsProducesEmptyResult() throws SQLException {
        try (Connection c = database(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM country");
            String result = capture(() -> new CountryReport().displayAllCountries(c));
            assertTrue(rows(result).isEmpty());
            assertTrue(result.contains("No results found."));
        }
    }

    @Test
    void nullContinentRejectedBeforeDatabaseAccess() {
        assertEquals("Continent must not be blank", assertThrows(IllegalArgumentException.class,
                () -> new CountryReport().displayCountriesInContinent(null, null)).getMessage());
    }

    @Test
    void missingConnectionRejectedForAllCountries() {
        assertEquals("Connection must not be null", assertThrows(NullPointerException.class,
                () -> new CountryReport().displayAllCountries(null)).getMessage());
    }

    @Test
    void topNFiltersAllowLimitLargerThanMatchingRows() throws SQLException {
        try (Connection c = database()) {
            CountryReport report = new CountryReport();
            for (String output : List.of(capture(() -> report.displayTopNCountriesInContinent(c, "Asia", 100)),
                    capture(() -> report.displayTopNCountriesInRegion(c, "Eastern Asia", 100)))) {
                assertEquals(List.of("AAA", "BBB", "CCC"), rows(output).stream().map(s -> s.substring(0, 3)).toList());
            }
        }
    }
}
