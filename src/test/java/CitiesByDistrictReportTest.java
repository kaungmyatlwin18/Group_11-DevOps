import com.napier.sem.report.city.CityReport;
import com.napier.sem.report.city.CitiesByDistrictReport;
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

/** Tests Issue #14 using an isolated H2 database instead of MySQL. */
public class CitiesByDistrictReportTest {

    /** Creates only the tables and sample data needed by the city report. */
    private Connection createTestDatabase() throws SQLException {
        String name = "district_" + UUID.randomUUID().toString().replace("-", "");
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
                    ('USA', 'United States'),
                    ('CAN', 'Canada')
                    """);
            // Insert out of population order to verify the SQL ORDER BY clause.
            statement.executeUpdate("""
                    INSERT INTO city (ID, Name, CountryCode, District, Population) VALUES
                    (1, 'Riverside', 'USA', 'California', 255166),
                    (2, 'Portland', 'USA', 'Oregon', 529121),
                    (3, 'San Diego', 'USA', 'California', 1223400),
                    (4, 'Vancouver', 'CAN', 'British Columbia', 514008),
                    (5, 'Los Angeles', 'USA', 'California', 3694820)
                    """);
        }
        return connection;
    }

    @Test
    void returnsOnlyCitiesInSelectedDistrict() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new CityReport().findCitiesInDistrict(connection, "California");

            assertEquals(3, cities.size());
            assertTrue(cities.stream().allMatch(
                    city -> city.district().equals("California")));
            assertFalse(cities.stream().anyMatch(
                    city -> city.name().equals("Portland")));
        }
    }

    @Test
    void ordersCitiesByPopulationDescending() throws Exception {
        try (Connection connection = createTestDatabase()) {
            List<CityReport.CityRow> cities =
                    new CityReport().findCitiesInDistrict(connection, "California");

            assertEquals(List.of("Los Angeles", "San Diego", "Riverside"),
                    cities.stream().map(CityReport.CityRow::name).toList());
            assertEquals(List.of(3694820L, 1223400L, 255166L),
                    cities.stream().map(CityReport.CityRow::population).toList());
        }
    }

    @Test
    void showsAllRequiredColumnsInDistrictReport() throws Exception {
        try (Connection connection = createTestDatabase()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            PrintStream original = System.out;

            try (PrintStream capture =
                         new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
                System.setOut(capture);
                new CitiesByDistrictReport().display(connection, "California");
            } finally {
                System.setOut(original);
            }

            String output = bytes.toString(StandardCharsets.UTF_8);
            for (String column : List.of(
                    "Name", "Country", "District", "Population")) {
                assertTrue(output.contains(column), "Missing column: " + column);
            }
            assertTrue(output.contains("Los Angeles"));
            assertTrue(output.contains("United States"));
            assertTrue(output.contains("California"));
            assertFalse(output.contains("Portland"));
        }
    }
}
