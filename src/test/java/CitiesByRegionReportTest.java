import com.napier.sem.report.city.CitiesByRegionReport;
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
 * Integration tests for Issue #12 using an isolated H2 World database.
 * The production MySQL server is not required to run these tests.
 */
public class CitiesByRegionReportTest {

    /** Creates countries in two regions and cities inserted out of population order. */
    private Connection createTestDatabase() throws SQLException {
        String databaseName = "city_region_" + UUID.randomUUID().toString().replace("-", "");
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + databaseName + ";MODE=MySQL");

        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE country (
                        Code VARCHAR(3) PRIMARY KEY,
                        Name VARCHAR(100),
                        Region VARCHAR(100)
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
                    INSERT INTO country (Code, Name, Region) VALUES
                    ('CHN', 'China', 'Eastern Asia'),
                    ('JPN', 'Japan', 'Eastern Asia'),
                    ('KOR', 'South Korea', 'Eastern Asia'),
                    ('USA', 'United States', 'North America')
                    """);
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (1, 'Tokyo', 'JPN', 'Tokyo-to', 8000000),
                    (2, 'Los Angeles', 'USA', 'California', 3700000),
                    (3, 'Osaka', 'JPN', 'Osaka', 2000000),
                    (4, 'Seoul', 'KOR', 'Seoul', 10000000),
                    (5, 'Shanghai', 'CHN', 'Shanghai', 9000000),
                    (6, 'New York', 'USA', 'New York', 8000000)
                    """);
        }
        return connection;
    }

    @Test
    void returnsOnlyCitiesFromCountriesInSelectedRegion() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new CityReport().findCitiesInRegion(connection, "Eastern Asia");

            assertEquals(4, cities.size());
            assertEquals(List.of("South Korea", "China", "Japan", "Japan"),
                    cities.stream().map(CityReport.CityRow::country).toList());
            assertTrue(cities.stream().noneMatch(
                    city -> city.country().equals("United States")));
        }
    }

    @Test
    void ordersRegionalCitiesByPopulationDescending() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new CityReport().findCitiesInRegion(connection, "Eastern Asia");

            assertEquals(List.of("Seoul", "Shanghai", "Tokyo", "Osaka"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals(List.of(10000000L, 9000000L, 8000000L, 2000000L),
                    cities.stream().map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void displaysRequiredColumnsAndSelectedRegion() throws Exception {
        try (Connection connection = createTestDatabase()) {
            PrintStream originalOutput = System.out;
            ByteArrayOutputStream capturedBytes = new ByteArrayOutputStream();

            try (PrintStream capturedOutput =
                         new PrintStream(capturedBytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capturedOutput);
                new CitiesByRegionReport().display(connection, "Eastern Asia");
            } finally {
                System.setOut(originalOutput);
            }

            String output = capturedBytes.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Cities in region: Eastern Asia"));
            for (String column : List.of("Name", "Country", "District", "Population")) {
                assertTrue(output.contains(column), "Missing column: " + column);
            }
            assertTrue(output.contains("Seoul"));
            assertTrue(output.contains("South Korea"));
            assertTrue(output.contains("Tokyo-to"));
            assertTrue(output.contains("10000000"));
            assertFalse(output.contains("Los Angeles"));
            assertFalse(output.contains("New York"));
        }
    }

    @Test
    void rejectsNullRegion() throws Exception {
        try (Connection connection = createTestDatabase()) {
            assertThrows(IllegalArgumentException.class,
                    () -> new CityReport().findCitiesInRegion(connection, null));
        }
    }

    @Test
    void rejectsBlankOrWhitespaceOnlyRegion() throws Exception {
        try (Connection connection = createTestDatabase()) {
            for (String region : List.of("", "   ", "\t\n")) {
                assertThrows(IllegalArgumentException.class,
                        () -> new CityReport().findCitiesInRegion(connection, region),
                        "Should reject blank region: [" + region + "]");
            }
        }
    }

    @Test
    void rejectsNullDatabaseConnection() {
        assertThrows(NullPointerException.class,
                () -> new CityReport().findCitiesInRegion(null, "Eastern Asia"));
    }

    @Test
    void returnsNoCitiesForUnknownRegion() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new CityReport().findCitiesInRegion(connection, "Unknown Region");

            assertNotNull(cities);
            assertTrue(cities.isEmpty());
        }
    }

    @Test
    void trimsWhitespaceAroundSelectedRegion() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new CityReport().findCitiesInRegion(connection, "  Eastern Asia  ");

            assertEquals(List.of("Seoul", "Shanghai", "Tokyo", "Osaka"),
                    cities.stream().map(CityReport.CityRow::name).toList());
        }
    }

    @Test
    void propagatesDatabaseErrorForClosedConnection() throws Exception {
        Connection connection = createTestDatabase();
        connection.close();

        assertThrows(SQLException.class,
                () -> new CityReport().findCitiesInRegion(connection, "Eastern Asia"));
    }

    @Test
    void breaksPopulationTiesByCityNameThenId() throws Exception {
        try (Connection connection = createTestDatabase()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                        (7, 'Zulu', 'CHN', 'Extra', 8000000),
                        (8, 'Alpha', 'CHN', 'Extra', 8000000),
                        (9, 'Twin', 'CHN', 'First', 500000),
                        (10, 'Twin', 'CHN', 'Second', 500000)
                        """);
            }

            List<CityReport.CityRow> cities =
                    new CityReport().findCitiesInRegion(connection, "Eastern Asia");

            // Three cities share 8 million people; names break the tie.
            assertEquals(List.of("Alpha", "Tokyo", "Zulu"),
                    cities.stream()
                            .filter(city -> city.population() == 8000000)
                            .map(CityReport.CityRow::name)
                            .toList());

            // Same name and population: the lower city ID comes first.
            assertEquals(List.of("First", "Second"),
                    cities.stream()
                            .filter(city -> city.name().equals("Twin"))
                            .map(CityReport.CityRow::district)
                            .toList());
        }
    }
}
