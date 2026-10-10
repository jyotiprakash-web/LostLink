package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * RFC 4180-compliant CSV serializer and parser.
 * Handles quoting, commas, escaped double-quotes (""), and multiline records.
 */
public final class CsvCodec {

    private CsvCodec() {
        // Utility class; prevent instantiation
    }

    /**
     * Escapes a single CSV field according to RFC 4180 rules.
     * Encloses the field in double quotes if it contains commas, double quotes,
     * or line breaks (\n or \r). Any double quotes within are escaped as "".
     */
    public static String escape(String field) {
        if (field == null) {
            return "";
        }
        boolean needsQuotes = field.contains(",") || field.contains("\"")
                || field.contains("\n") || field.contains("\r");
        if (!needsQuotes) {
            return field;
        }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    /**
     * Formats a list of fields into a single CSV row.
     */
    public static String formatRow(List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields.get(i)));
        }
        return sb.toString();
    }

    /**
     * Formats an array of fields into a single CSV row.
     */
    public static String formatRow(String... fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }
        return formatRow(Arrays.asList(fields));
    }

    /**
     * Parses a single CSV line/record into its constituent fields.
     * Returns an empty list if the input is null or empty.
     */
    public static List<String> parseRecord(String record) {
        if (record == null || record.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        int len = record.length();

        for (int i = 0; i < len; i++) {
            char c = record.charAt(i);
            if (c == '"') {
                if (inQuotes) {
                    if (i + 1 < len && record.charAt(i + 1) == '"') {
                        // Escaped double-quote
                        currentField.append('"');
                        i++;
                    } else {
                        // Closing quote
                        inQuotes = false;
                    }
                } else {
                    // Opening quote
                    inQuotes = true;
                }
            } else if (c == ',') {
                if (inQuotes) {
                    currentField.append(',');
                } else {
                    fields.add(currentField.toString());
                    currentField.setLength(0);
                }
            } else if (c == '\r') {
                // If in quotes, preserve; otherwise ignore carriage return
                if (inQuotes) {
                    currentField.append('\r');
                }
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());
        return Collections.unmodifiableList(fields);
    }

    /**
     * Checks if a text buffer has unclosed quotation marks.
     */
    public static boolean hasUnclosedQuotes(String text) {
        if (text == null) {
            return false;
        }
        boolean inQuotes = false;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < len && text.charAt(i + 1) == '"') {
                    i++; // Skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            }
        }
        return inQuotes;
    }

    /**
     * Reads the next complete CSV record from a BufferedReader,
     * seamlessly spanning multiple lines if a quoted field contains embedded newlines.
     * Returns null when end of file is reached.
     */
    public static List<String> readNextRecord(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        StringBuilder recordBuilder = new StringBuilder(line);
        while (hasUnclosedQuotes(recordBuilder.toString())) {
            String nextLine = reader.readLine();
            if (nextLine == null) {
                // EOF with open quote; finish with what we have
                break;
            }
            recordBuilder.append("\n").append(nextLine);
        }
        return parseRecord(recordBuilder.toString());
    }
}
