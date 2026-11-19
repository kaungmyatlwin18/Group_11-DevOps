import com.napier.sem.ReportDisplay;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Issue #76: pure unit tests for the shared formatter; no JDBC required. */
public class ReportDisplayTest {
    private String capture(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            action.run();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void alignsColumnsAndExpandsWidthForLongValues() {
        String output = capture(() -> ReportDisplay.printTable("Example",
                new String[]{"Name", "Count"}, List.of(new String[]{"Long name", "42"},
                        new String[]{"A", "7"})));
        assertEquals(List.of("===== Example =====", "Name       Count", "----------------",
                "Long name  42", "A          7"),
                output.lines().map(String::stripTrailing).filter(s -> !s.isEmpty()).toList());
    }

    @Test
    void rendersNullCellAsNotAvailable() {
        String output = capture(() -> ReportDisplay.printTable("Null values",
                new String[]{"Name", "Capital"}, List.<String[]>of(new String[]{"Example", null})));
        assertTrue(output.lines().anyMatch(s -> s.strip().matches("Example\\s+N/A")));
        assertFalse(output.contains("null"));
    }

    @Test
    void displaysHeaderAndMessageForEmptyResults() {
        String output = capture(() -> ReportDisplay.printTable("Empty", new String[]{"Name"}, List.of()));
        assertEquals(List.of("===== Empty =====", "Name", "----", "No results found."),
                output.lines().map(String::stripTrailing).filter(s -> !s.isEmpty()).toList());
    }

    @Test
    void rejectsRowsWithTooFewColumns() {
        capture(() -> assertEquals("Row width does not match headers",
                assertThrows(IllegalArgumentException.class, () -> ReportDisplay.printTable("Invalid",
                        new String[]{"Name", "Count"}, List.<String[]>of(new String[]{"A"}))).getMessage()));
    }

    @Test
    void rejectsRowsWithTooManyColumns() {
        capture(() -> assertThrows(IllegalArgumentException.class, () -> ReportDisplay.printTable("Invalid",
                new String[]{"Name"}, List.<String[]>of(new String[]{"A", "B"}))));
    }
}
