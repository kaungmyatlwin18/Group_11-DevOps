import com.napier.sem.report.city.CitiesByContinentQuery;
import com.napier.sem.report.city.CitiesByContinentReport;
import com.napier.sem.report.city.CityReport;
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

/**
 * Tests for Issue #11. Uses H2 in MySQL mode, without depending on Docker.
 */
public class CitiesByContinentReportTest {

    private Connection createDatabase() throws SQLException {
        String unique = UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:continent_" + unique + ";MODE=MySQL");
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100) NOT NULL,
                        Continent VARCHAR(100) NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE city (
                        ID INT PRIMARY KEY,
                        Name VARCHAR(100) NOT NULL,
                        CountryCode VARCHAR(3) NOT NULL,
                        District VARCHAR(100),
                        Population BIGINT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO country (Code, Name, Continent) VALUES
                    ('MMR', 'Myanmar', 'Asia'),
                    ('JPN', 'Japan', 'Asia'),
                    ('CHN', 'China', 'Asia'),
                    ('KOR', 'South Korea', 'Asia'),
                    ('GBR', 'United Kingdom', 'Europe'),
                    ('FRA', 'France', 'Europe'),
                    ('EGY', 'Egypt', 'Africa'),
                    ('USA', 'United States', 'North America')
                    """);
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (1, 'Mandalay', 'MMR', 'Mandalay', 885300),
                    (2, 'Tokyo', 'JPN', 'Tokyo-to', 7980230),
                    (3, 'Rangoon', 'MMR', 'Rangoon', 3361700),
                    (4, 'Shanghai', 'CHN', 'Shanghai', 9696300),
                    (5, 'London', 'GBR', 'England', 7285000),
                    (6, 'Seoul', 'KOR', 'Seoul', 9981619),
                    (7, 'Paris', 'FRA', 'Ile-de-France', 2125246),
                    (8, 'Cairo', 'EGY', 'Cairo', 6789479),
                    (9, 'Los Angeles', 'USA', 'California', 3694820)
                    """);
        }
        return connection;
    }

    private List<CityReport.CityRow> fetch(Connection connection, String continent)
            throws SQLException {
        return new CitiesByContinentQuery().findCitiesInContinent(connection, continent);
    }

    @Test
    void includesOnlyCitiesInSelectedContinent() throws Exception {
        try (Connection connection = createDatabase()) {
            List<CityReport.CityRow> cities = fetch(connection, "Asia");
            assertEquals(5, cities.size());
            assertEquals(List.of("South Korea", "China", "Japan", "Myanmar", "Myanmar"),
                    cities.stream().map(CityReport.CityRow::country).toList());
            assertFalse(cities.stream().anyMatch(city -> city.name().equals("London")));
        }
    }

    @Test
    void sortsPopulationFromLargestToSmallest() throws Exception {
        try (Connection connection = createDatabase()) {
            List<CityReport.CityRow> cities = fetch(connection, "Asia");
            assertEquals(List.of("Seoul", "Shanghai", "Tokyo", "Rangoon", "Mandalay"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals(List.of(9981619L, 9696300L, 7980230L, 3361700L, 885300L),
                    cities.stream().map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void displaysAllRequiredColumns() throws Exception {
        try (Connection connection = createDatabase()) {
            PrintStream original = System.out;
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                new CitiesByContinentReport().display(connection, "Asia");
            } finally {
                System.setOut(original);
            }
            String output = bytes.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Cities in continent: Asia"));
            for (String header : List.of("Name", "Country", "District", "Population")) {
                assertTrue(output.contains(header), "Missing column: " + header);
            }
            assertTrue(output.contains("South Korea"));
            assertTrue(output.contains("Tokyo-to"));
            assertTrue(output.contains("9981619"));
            assertTrue(output.indexOf("Seoul") < output.indexOf("Shanghai"));
            assertFalse(output.contains("Los Angeles"));
        }
    }

    @Test
    void supportsSelectingASecondContinent() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(List.of("London", "Paris"),
                    fetch(connection, "Europe").stream().map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void trimsWhitespaceAroundContinentName() throws Exception {
        try (Connection connection = createDatabase()) {
            assertEquals(5, fetch(connection, "  Asia  ").size());
        }
    }

    @Test
    void rejectsNullContinent() throws Exception {
        try (Connection connection = createDatabase()) {
            assertThrows(IllegalArgumentException.class, () -> fetch(connection, null));
        }
    }

    @Test
    void rejectsBlankContinent() throws Exception {
        try (Connection connection = createDatabase()) {
            for (String continent : List.of("", " ", "\t\n")) {
                assertThrows(IllegalArgumentException.class,
                        () -> fetch(connection, continent));
            }
        }
    }

    @Test
    void rejectsNullConnection() {
        assertThrows(NullPointerException.class, () -> fetch(null, "Asia"));
    }

    @Test
    void propagatesErrorForClosedConnection() throws Exception {
        Connection connection = createDatabase();
        connection.close();
        assertThrows(SQLException.class, () -> fetch(connection, "Asia"));
    }

    @Test
    void unknownContinentReturnsNoCities() throws Exception {
        try (Connection connection = createDatabase()) {
            assertTrue(fetch(connection, "Atlantis").isEmpty());
        }
    }

    @Test
    void sortsEqualPopulationByNameThenCityId() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (20, 'Zulu', 'MMR', 'Tie', 885300),
                    (21, 'Alpha', 'MMR', 'Tie', 885300),
                    (22, 'Twin', 'MMR', 'First', 200),
                    (23, 'Twin', 'MMR', 'Second', 200)
                    """);
            List<CityReport.CityRow> cities = fetch(connection, "Asia");
            assertEquals(List.of("Alpha", "Mandalay", "Zulu"),
                    cities.stream().filter(c -> c.population() == 885300)
                            .map(CityReport.CityRow::name).toList());
            assertEquals(List.of("First", "Second"),
                    cities.stream().filter(c -> c.name().equals("Twin"))
                            .map(CityReport.CityRow::district).toList());
        }
    }

    @Test
    void doesNotLimitResultsToTopN() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            for (int i = 10; i < 130; i++) {
                statement.executeUpdate("INSERT INTO city (ID, Name, CountryCode, District, Population) "
                        + "VALUES (" + i + ", 'TestCity" + i + "', 'MMR', 'Sample', " + i + ")");
            }
            assertEquals(125, fetch(connection, "Asia").size());
        }
    }

    @Test
    void emptyCityTableReturnsEmptyResults() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM city");
            assertTrue(fetch(connection, "Asia").isEmpty());
        }
    }

    @Test
    void preservesNullDistrictAndZeroPopulation() throws Exception {
        try (Connection connection = createDatabase();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population)
                    VALUES (30, 'ZeroCity', 'JPN', NULL, 0)
                    """);
            List<CityReport.CityRow> cities = fetch(connection, "Asia");
            CityReport.CityRow last = cities.get(cities.size() - 1);
            assertEquals("ZeroCity", last.name());
            assertNull(last.district());
            assertEquals(0L, last.population());
        }
    }

    @Test
    void treatsSqlLikeInputAsAParameterNotAnExpression() throws Exception {
        try (Connection connection = createDatabase()) {
            assertTrue(fetch(connection, "Asia' OR '1'='1").isEmpty());
        }
    }
}
