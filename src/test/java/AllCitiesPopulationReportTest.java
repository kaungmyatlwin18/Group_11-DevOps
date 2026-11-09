import com.napier.sem.report.city.AllCitiesPopulationQuery;
import com.napier.sem.report.city.AllCitiesPopulationReport;
import com.napier.sem.report.city.CityReport;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #10: all cities worldwide ordered by population.
 * Uses an independent H2 database; no Docker/MySQL connection is needed.
 */
public class AllCitiesPopulationReportTest {

    /** Inserts cities in intentionally mixed order across several countries. */
    private Connection createTestDatabase() throws SQLException {
        String name = "all_cities_" + UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + name + ";MODE=MySQL");

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE city (
                        ID INT PRIMARY KEY,
                        Name VARCHAR(100),
                        CountryCode VARCHAR(3),
                        District VARCHAR(100),
                        Population BIGINT
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name) VALUES
                    ('MMR', 'Myanmar'),
                    ('JPN', 'Japan'),
                    ('CHN', 'China'),
                    ('KOR', 'South Korea')
                    """);
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (1, 'Mandalay', 'MMR', 'Mandalay', 885300),
                    (2, 'Tokyo', 'JPN', 'Tokyo-to', 7980230),
                    (3, 'Rangoon', 'MMR', 'Rangoon', 3361700),
                    (4, 'Shanghai', 'CHN', 'Shanghai', 9696300),
                    (5, 'Osaka', 'JPN', 'Osaka', 2595674),
                    (6, 'Seoul', 'KOR', 'Seoul', 9981619)
                    """);
        }
        return connection;
    }

    @Test
    void returnsAllCitiesAcrossEveryCountry() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new AllCitiesPopulationQuery().findAllCities(connection);

            assertEquals(6, cities.size(), "The report must include all six cities");
            assertEquals(List.of("South Korea", "China", "Japan", "Myanmar", "Japan", "Myanmar"),
                    cities.stream().map(CityReport.CityRow::country).toList());
            assertTrue(cities.stream().anyMatch(city -> city.name().equals("Mandalay")));
            assertTrue(cities.stream().anyMatch(city -> city.name().equals("Osaka")));
        }
    }

    @Test
    void ordersCitiesWorldwideByPopulationDescending() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new AllCitiesPopulationQuery().findAllCities(connection);

            assertEquals(List.of("Seoul", "Shanghai", "Tokyo", "Rangoon", "Osaka", "Mandalay"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals(List.of(9981619L, 9696300L, 7980230L, 3361700L, 2595674L, 885300L),
                    cities.stream().map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void displaysRequiredColumnsAndWorldwideResults() throws Exception {
        try (Connection connection = createTestDatabase()) {
            PrintStream originalOutput = System.out;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                new AllCitiesPopulationReport().display(connection);
            } finally {
                System.setOut(originalOutput);
            }

            String output = bytes.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("All cities in the world by population"));
            for (String header : List.of("Name", "Country", "District", "Population")) {
                assertTrue(output.contains(header), "Missing column: " + header);
            }
            assertTrue(output.contains("Seoul"));
            assertTrue(output.contains("South Korea"));
            assertTrue(output.contains("Tokyo-to"));
            assertTrue(output.contains("Myanmar"));
            assertTrue(output.contains("9981619"));
            assertTrue(output.contains("885300"));
            assertTrue(output.indexOf("Seoul") < output.indexOf("Shanghai"),
                    "Largest city should print before the second largest");
        }
    }

@Test
void rejectsNullConnection() {
    assertThrows(NullPointerException.class,
            () -> new AllCitiesPopulationQuery().findAllCities(null));
}

@Test
void rejectsClosedConnection() throws Exception {
    Connection connection = createTestDatabase();
    connection.close();

    assertThrows(SQLException.class,
            () -> new AllCitiesPopulationQuery().findAllCities(connection));
}

@Test
void returnsEmptyListWhenThereAreNoCities() throws Exception {
    try (Connection connection = createTestDatabase();
         Statement statement = connection.createStatement()) {
        statement.executeUpdate("DELETE FROM city");

        List<CityReport.CityRow> cities =
                new AllCitiesPopulationQuery().findAllCities(connection);
        assertTrue(cities.isEmpty(), "An empty city table should return no rows");
    }
}

@Test
void sortsEqualPopulationsAlphabeticallyByCityName() throws Exception {
    try (Connection connection = createTestDatabase();
         Statement statement = connection.createStatement()) {
        statement.executeUpdate("""
                INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                (7, 'Zulu',  'JPN', 'Tokyo-to', 500000),
                (8, 'Alpha', 'MMR', 'Mandalay', 500000),
                (9, 'Beta',  'CHN', 'Shanghai', 500000)
                """);

        List<String> tiedNames = new AllCitiesPopulationQuery()
                .findAllCities(connection).stream()
                .filter(city -> city.population() == 500000L)
                .map(CityReport.CityRow::name)
                .toList();

        assertEquals(List.of("Alpha", "Beta", "Zulu"), tiedNames,
                "Population ties should be ordered by city name");
    }
}

@Test
void includesZeroPopulationCitiesAtTheEnd() throws Exception {
    try (Connection connection = createTestDatabase();
         Statement statement = connection.createStatement()) {
        statement.executeUpdate("""
                INSERT INTO city (ID, Name, CountryCode, District, Population)
                VALUES (10, 'Zero City', 'MMR', 'Mandalay', 0)
                """);

        List<CityReport.CityRow> cities =
                new AllCitiesPopulationQuery().findAllCities(connection);
        CityReport.CityRow last = cities.get(cities.size() - 1);

        assertEquals(7, cities.size());
        assertEquals("Zero City", last.name());
        assertEquals(0L, last.population());
    }
}

@Test
void keepsCitiesWithNullDistrict() throws Exception {
    try (Connection connection = createTestDatabase();
         Statement statement = connection.createStatement()) {
        statement.executeUpdate("""
                INSERT INTO city (ID, Name, CountryCode, District, Population)
                VALUES (11, 'No District City', 'JPN', NULL, 125000)
                """);

        CityReport.CityRow city = new AllCitiesPopulationQuery()
                .findAllCities(connection).stream()
                .filter(row -> row.name().equals("No District City"))
                .findFirst()
                .orElseThrow();

        assertEquals("Japan", city.country());
        assertNull(city.district(), "The district should not be invented");
        assertEquals(125000L, city.population());
    }
}

@Test
void doesNotLimitResultsToTopOneHundredCities() throws Exception {
    try (Connection connection = createTestDatabase();
         PreparedStatement insert = connection.prepareStatement("""
                 INSERT INTO city (ID, Name, CountryCode, District, Population)
                 VALUES (?, ?, 'JPN', 'Osaka', ?)
                 """)) {
        for (int i = 0; i < 120; i++) {
            insert.setInt(1, 1000 + i);
            insert.setString(2, "Additional City " + i);
            insert.setLong(3, 100L + i);
            insert.addBatch();
        }
        insert.executeBatch();

        List<CityReport.CityRow> cities =
                new AllCitiesPopulationQuery().findAllCities(connection);
        assertEquals(126, cities.size(),
                "All cities must be returned, not just the top 100");
        assertEquals("Seoul", cities.get(0).name());
        assertEquals(100L, cities.get(cities.size() - 1).population());
    }
}

}
