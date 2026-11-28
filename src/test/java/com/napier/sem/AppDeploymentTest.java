package com.napier.sem;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Regression tests for Issue #100 without requiring a live MySQL server. */
class AppDeploymentTest {
    @Test
    void successfulConnectionRunsReportsAndClosesConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:");
        AtomicInteger reports = new AtomicInteger();
        assertEquals(0, App.run(() -> connection, c -> {
            assertSame(connection, c);
            reports.incrementAndGet();
        }, 3, () -> fail("Unexpected retry")));
        assertEquals(1, reports.get());
        assertTrue(connection.isClosed());
    }

    @Test
    void temporaryStartupFailureRetriesThenRunsReports() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:");
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger waits = new AtomicInteger();
        AtomicInteger reports = new AtomicInteger();
        assertEquals(0, App.run(() -> {
            if (attempts.incrementAndGet() < 3) throw new SQLException("Not ready");
            return connection;
        }, c -> reports.incrementAndGet(), 3, waits::incrementAndGet));
        assertEquals(3, attempts.get());
        assertEquals(2, waits.get());
        assertEquals(1, reports.get());
        assertTrue(connection.isClosed());
    }

    @Test
    void persistentFailureReturnsNonZeroWithoutRunningReports() {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger waits = new AtomicInteger();
        assertEquals(1, App.run(() -> {
            attempts.incrementAndGet();
            throw new SQLException("Unavailable");
        }, c -> fail("Reports must not run"), 3, waits::incrementAndGet));
        assertEquals(3, attempts.get());
        assertEquals(2, waits.get());
    }

    @Test
    void nullConnectionReturnsFailure() {
        assertEquals(1, App.run(() -> null, c -> fail("Reports must not run"),
                1, () -> fail("Unexpected retry")));
    }

    @Test
    void reportSqlFailureDoesNotRetryAndClosesConnection() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:");
        AtomicInteger attempts = new AtomicInteger();
        assertEquals(1, App.run(() -> {
            attempts.incrementAndGet();
            return connection;
        }, c -> { throw new SQLException("Report failed"); }, 3,
                () -> fail("Reporting failure must not retry")));
        assertEquals(1, attempts.get());
        assertTrue(connection.isClosed());
    }

    @Test
    void interruptedRetryReturnsFailureAndRestoresInterruptFlag() {
        try {
            assertEquals(1, App.run(() -> { throw new SQLException("Unavailable"); },
                    c -> fail("Reports must not run"), 3,
                    () -> { throw new InterruptedException(); }));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
