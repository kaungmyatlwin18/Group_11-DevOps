package com.napier.sem;

import java.util.List;

/** Shared console table formatter for Group 11 reports. */
public final class ReportDisplay {

    private ReportDisplay() {
    }

    public static void printTable(String title, String[] headers, List<String[]> rows) {
        System.out.println("\n===== " + title + " =====");

        int[] widths = new int[headers.length];
        for (int col = 0; col < headers.length; col++) {
            widths[col] = headers[col].length();
        }
        for (String[] row : rows) {
            if (row.length != headers.length) {
                throw new IllegalArgumentException("Row width does not match headers");
            }
            for (int col = 0; col < headers.length; col++) {
                String value = row[col] == null ? "N/A" : row[col];
                widths[col] = Math.max(widths[col], value.length());
            }
        }

        printRow(headers, widths);
        int totalWidth = 0;
        for (int width : widths) {
            totalWidth += width + 2;
        }
        System.out.println("-".repeat(Math.max(0, totalWidth - 2)));

        if (rows.isEmpty()) {
            System.out.println("No results found.");
        } else {
            for (String[] row : rows) {
                printRow(row, widths);
            }
        }
    }

    private static void printRow(String[] row, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int col = 0; col < row.length; col++) {
            String value = row[col] == null ? "N/A" : row[col];
            line.append(String.format("%-" + widths[col] + "s", value));
            if (col != row.length - 1) {
                line.append("  ");
            }
        }
        System.out.println(line);
    }
}
