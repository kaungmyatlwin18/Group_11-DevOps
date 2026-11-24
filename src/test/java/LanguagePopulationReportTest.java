import com.napier.sem.AutoDisplay;
import com.napier.sem.report.population.LanguagePopulationQuery;
import com.napier.sem.report.population.LanguagePopulationQuery.LanguageRow;
import com.napier.sem.report.population.LanguagePopulationReport;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LanguagePopulationReportTest {
    private Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:language_" + UUID.randomUUID() + ";MODE=MySQL");
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
                        IsOfficial VARCHAR(1), Percentage DECIMAL(7,4), PRIMARY KEY(CountryCode, Language))
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name, Continent, Region, Population, Capital) VALUES
                    ('AAA','Country A','Asia','Eastern Asia',500,1),
                    ('BBB','Country B','Asia','Southern Asia',300,2),
                    ('CCC','Country C','Europe','Western Europe',200,3),
                    ('DDD','Country D','Africa','Eastern Africa',1000,4)
                    """);
            statement.executeUpdate("""
                    INSERT INTO city VALUES (1,'Alpha','AAA','Central',100),
                    (2,'Bravo','BBB','Central',80), (3,'Charlie','CCC','Central',70),
                    (4,'Delta','DDD','Central',50)
                    """);
            statement.executeUpdate("""
                    INSERT INTO countrylanguage VALUES
                    ('AAA','Chinese','T',80), ('BBB','Chinese','F',10),
                    ('AAA','English','F',20), ('BBB','English','T',50), ('CCC','English','T',100),
                    ('BBB','Hindi','T',20), ('CCC','Hindi','F',10),
                    ('AAA','Spanish','F',2), ('BBB','Spanish','F',10),
                    ('CCC','Arabic','F',25), ('CCC','French','T',50)
                    """);
            return connection;
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }

    private List<LanguageRow> rows(Connection c) throws SQLException {
        return new LanguagePopulationQuery().findLanguagePopulations(c);
    }
    private LanguageRow language(Connection c, String name) throws SQLException {
        return rows(c).stream().filter(r -> r.language().equals(name)).findFirst().orElseThrow();
    }
    private void update(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) { s.executeUpdate(sql); }
    }
    private String capture(Connection c) throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            new LanguagePopulationReport().display(c);
        } finally { System.setOut(original); }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test void includesExactlyFiveRequiredLanguagesInDescendingSpeakerOrder() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("English", "Chinese", "Hindi", "Arabic", "Spanish"),
                    rows(c).stream().map(LanguageRow::language).toList());
        }
    }
    @Test void calculatesSpeakerTotalsAcrossCountries() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of(450L, 430L, 80L, 50L, 40L),
                    rows(c).stream().map(LanguageRow::speakers).toList());
        }
    }
    @Test void usesAllCountriesForWorldDenominatorIncludingCountriesWithoutLanguageRows() throws SQLException {
        try (Connection c = database()) {
            assertEquals(List.of("22.50", "21.50", "4.00", "2.50", "2.00"),
                    rows(c).stream().map(r -> r.worldPercentage().toPlainString()).toList());
        }
    }
    @Test void includesNonOfficialSpeakers() throws SQLException {
        try (Connection c = database()) {
            assertEquals(50L, language(c, "Arabic").speakers());
            assertEquals(430L, language(c, "Chinese").speakers());
        }
    }
    @Test void excludesUnrelatedLanguages() throws SQLException {
        try (Connection c = database()) {
            assertFalse(rows(c).stream().anyMatch(r -> r.language().equals("French")));
        }
    }
    @Test void absentRequiredLanguageIsStillIncludedWithZeroSpeakers() throws SQLException {
        try (Connection c = database()) {
            update(c, "DELETE FROM countrylanguage WHERE Language = 'Arabic'");
            assertEquals(5, rows(c).size());
            assertEquals(0L, language(c, "Arabic").speakers());
            assertEquals(new BigDecimal("0.00"), language(c, "Arabic").worldPercentage());
        }
    }
    @Test void roundsOnlyAfterAggregationAndUsesUnroundedEstimateForPercentage() throws SQLException {
        try (Connection c = database()) {
            update(c, "UPDATE country SET Population = 3 WHERE Code = 'AAA'");
            update(c, "UPDATE country SET Population = 3 WHERE Code = 'BBB'");
            update(c, "UPDATE country SET Population = 0 WHERE Code IN ('CCC','DDD')");
            update(c, "UPDATE countrylanguage SET Percentage = 20 WHERE Language = 'Chinese'");
            assertEquals(1L, language(c, "Chinese").speakers());
            assertEquals(new BigDecimal("20.00"), language(c, "Chinese").worldPercentage());
        }
    }
    @Test void supportsSpeakerCountsLargerThanInt() throws SQLException {
        try (Connection c = database()) {
            update(c, "UPDATE country SET Population = 3000000000 WHERE Code = 'AAA'");
            update(c, "UPDATE countrylanguage SET Percentage = 100 WHERE CountryCode = 'AAA' AND Language = 'Chinese'");
            assertEquals(3000000030L, language(c, "Chinese").speakers());
        }
    }
    @Test void resolvesTiesAlphabetically() throws SQLException {
        try (Connection c = database()) {
            update(c, "DELETE FROM countrylanguage");
            assertEquals(List.of("Arabic", "Chinese", "English", "Hindi", "Spanish"),
                    rows(c).stream().map(LanguageRow::language).toList());
        }
    }
    @Test void matchesLanguageIgnoringCase() throws SQLException {
        try (Connection c = database()) {
            update(c, "UPDATE countrylanguage SET Language = 'cHiNeSe' WHERE CountryCode = 'AAA' AND Language = 'Chinese'");
            assertEquals(430L, language(c, "Chinese").speakers());
        }
    }
    @Test void handlesZeroWorldPopulation() throws SQLException {
        try (Connection c = database()) {
            update(c, "UPDATE country SET Population = 0");
            assertTrue(rows(c).stream().allMatch(r -> r.speakers() == 0 && r.worldPercentage().signum() == 0));
        }
    }
    @Test void handlesEmptyCountryTable() throws SQLException {
        try (Connection c = database()) {
            update(c, "DELETE FROM countrylanguage");
            update(c, "DELETE FROM country");
            assertEquals(5, rows(c).size());
            assertTrue(rows(c).stream().allMatch(r -> r.speakers() == 0 && r.worldPercentage().signum() == 0));
        }
    }
    @Test void rejectsNullConnectionAndPropagatesClosedConnectionError() throws SQLException {
        assertThrows(NullPointerException.class, () -> rows(null));
        Connection c = database(); c.close();
        assertThrows(SQLException.class, () -> rows(c));
    }
    @Test void displaysLanguageSpeakerAndPercentageColumnsInOrder() throws SQLException {
        try (Connection c = database()) {
            String text = capture(c);
            assertTrue(text.contains("Worldwide language population statistics"));
            assertTrue(text.contains("Language"));
            assertTrue(text.contains("Speakers"));
            assertTrue(text.contains("World Population (%)"));
            assertTrue(text.lines().anyMatch(line -> line.trim().matches("English\\s+450\\s+22\\.50%")));
            assertTrue(text.indexOf("English") < text.indexOf("Chinese"));
            assertFalse(text.contains("French"));
        }
    }
    @Test void schemaCheckSupportsFullDatabaseAndSkipsMissingLanguageTable() throws SQLException {
        try (Connection c = database()) {
            assertTrue(LanguagePopulationReport.isAvailable(c));
            update(c, "DROP TABLE countrylanguage");
            assertFalse(LanguagePopulationReport.isAvailable(c));
        }
    }
    @Test void schemaCheckSkipsIncompleteLanguageTable() throws SQLException {
        try (Connection c = database()) {
            update(c, "ALTER TABLE countrylanguage DROP COLUMN Percentage");
            assertFalse(LanguagePopulationReport.isAvailable(c));
        }
    }
    @Test void autoDisplayIncludesIssue35AndPreservesEarlierReports() throws SQLException {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Connection c = database(); PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            AutoDisplay.displayAllReports(c);
        } finally { System.setOut(original); }
        String output = bytes.toString(StandardCharsets.UTF_8);
        String heading = "===== ISSUE #35 =====";
        int start = output.indexOf(heading);
        assertTrue(start >= 0, "Missing Issue #35 report");
        // Stop only at an actual issue heading or the application completion banner.
        // ReportDisplay also uses equals signs in its own title formatting.
        int end = output.indexOf("\n===== ISSUE #", start + heading.length());
        if (end < 0) {
            end = output.indexOf("\n========== AVAILABLE REPORTS COMPLETED ==========", start + heading.length());
        }
        String section = output.substring(start, end < 0 ? output.length() : end);
        assertTrue(section.contains("English"));
        assertTrue(section.contains("22.50%"));
        assertTrue(output.contains("===== ISSUE #34 ====="));
        assertTrue(output.contains("AVAILABLE REPORTS COMPLETED"));
    }
}
