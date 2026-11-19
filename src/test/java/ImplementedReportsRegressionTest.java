import com.napier.sem.AutoDisplay;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** Issue #76: H2-backed regression test for the 23 reports currently implemented. */
public class ImplementedReportsRegressionTest {
    @Test
    void eachImplementedReportDisplaysExactlyOneSectionWithFixtureData() throws Exception {
        Map<String, String> properties = Map.ofEntries(
                Map.entry("topN.world", "5"), Map.entry("city.continent", "Asia"),
                Map.entry("city.region", "Eastern Asia"), Map.entry("city.country", "Myanmar"),
                Map.entry("city.district", "California"), Map.entry("city.topN.world", "5"),
                Map.entry("city.topN.continent", "5"), Map.entry("city.topN.region", "5"),
                Map.entry("city.topN.country", "5"), Map.entry("city.topN.district", "5"),
                Map.entry("capital.continent", "Asia"), Map.entry("capital.region", "Eastern Asia"),
                Map.entry("capital.topN.world", "5"));
        Map<String, String> previous = new HashMap<>();
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Connection c = DriverManager.getConnection("jdbc:h2:mem:regression_" + UUID.randomUUID() + ";MODE=MySQL");
             Statement s = c.createStatement(); PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            s.execute("CREATE TABLE country (Code VARCHAR(3) PRIMARY KEY, Name VARCHAR(100), Continent VARCHAR(50), Region VARCHAR(100), Population BIGINT, Capital INT)");
            s.execute("CREATE TABLE city (ID INT PRIMARY KEY, Name VARCHAR(100), CountryCode VARCHAR(3), District VARCHAR(100), Population BIGINT NOT NULL)");
            // Synthetic fixture values deliberately exercise each configured filter.
            s.executeUpdate("INSERT INTO country VALUES ('MMR','Myanmar','Asia','Southeast Asia',1000,1), ('JPN','Japan','Asia','Eastern Asia',2000,2)");
            s.executeUpdate("INSERT INTO city VALUES (1,'Fixture Myanmar Capital','MMR','California',500), (2,'Fixture Japan Capital','JPN','Fixture District',1200)");
            properties.forEach((key, value) -> { previous.put(key, System.getProperty(key)); System.setProperty(key, value); });
            System.setOut(out);
            AutoDisplay.displayAllReports(c);
        } finally {
            System.setOut(original);
            previous.forEach((key, value) -> { if (value == null) System.clearProperty(key); else System.setProperty(key, value); });
        }
        String output = bytes.toString(StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("(?m)^===== ISSUE #(\\d+) =====\\s*$").matcher(output);
        Map<Integer, String> sections = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();
        int previousId = -1, start = 0;
        while (matcher.find()) {
            if (previousId != -1) sections.put(previousId, output.substring(start, matcher.start()));
            previousId = Integer.parseInt(matcher.group(1));
            counts.merge(previousId, 1, Integer::sum);
            start = matcher.end();
        }
        if (previousId != -1) sections.put(previousId, output.substring(start));
        Map<Integer, String> expected = Map.ofEntries(
                Map.entry(2,"Japan"), Map.entry(4,"Japan"), Map.entry(6,"Japan"),
                Map.entry(7,"Japan"), Map.entry(8,"Japan"), Map.entry(9,"Japan"),
                Map.entry(10,"Fixture Japan Capital"), Map.entry(11,"Fixture Japan Capital"),
                Map.entry(12,"Fixture Japan Capital"), Map.entry(13,"Fixture Myanmar Capital"),
                Map.entry(14,"Fixture Myanmar Capital"), Map.entry(15,"Fixture Japan Capital"),
                Map.entry(16,"Fixture Japan Capital"), Map.entry(17,"Fixture Japan Capital"),
                Map.entry(18,"Fixture Myanmar Capital"), Map.entry(19,"Fixture Myanmar Capital"),
                Map.entry(20,"Fixture Japan Capital"), Map.entry(21,"Fixture Japan Capital"),
                Map.entry(22,"Fixture Japan Capital"), Map.entry(23,"Fixture Japan Capital"),
                Map.entry(26,"3,000"), Map.entry(27,"2,000"), Map.entry(28,"2,000"));
        expected.forEach((id, value) -> {
            assertEquals(1, counts.getOrDefault(id, 0).intValue(), "Missing or duplicate issue #" + id);
            assertTrue(sections.get(id).contains(value), "Missing fixture data in issue #" + id);
            assertFalse(sections.get(id).contains("No results found."), "Empty issue #" + id);
        });
        // Future implemented reports are allowed, so this test will not block their addition.
        assertTrue(output.contains("AVAILABLE REPORTS COMPLETED"));
    }
}
