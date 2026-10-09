package util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

/**
 * Reusable console input utility supporting validated input, re-prompting, and clean EOF handling.
 */
public class InputHelper {
    private final BufferedReader reader;
    private final PrintStream out;
    private boolean eof = false;

    public InputHelper(BufferedReader reader, PrintStream out) {
        this.reader = reader;
        this.out = (out != null) ? out : System.out;
    }

    public InputHelper(BufferedReader reader) {
        this(reader, System.out);
    }

    public InputHelper(InputStream in, PrintStream out) {
        this(new BufferedReader(new InputStreamReader(in)), out);
    }

    public InputHelper() {
        this(System.in, System.out);
    }

    public boolean isEof() {
        return eof;
    }

    private String readLineInternal() {
        if (eof) {
            return null;
        }
        try {
            String line = reader.readLine();
            if (line == null) {
                eof = true;
                return null;
            }
            return line;
        } catch (Exception e) {
            eof = true;
            return null;
        }
    }

    /**
     * Reads a numeric menu choice in range [min, max].
     * Returns -1 if EOF is encountered.
     */
    public int readMenuChoice(int min, int max) {
        while (true) {
            out.print("Enter your choice (" + min + "-" + max + "): ");
            String input = readLineInternal();
            if (input == null) {
                return -1; // EOF signal
            }
            input = input.trim();
            if (input.isEmpty()) {
                out.println("Input cannot be empty. Please enter a number between " + min + " and " + max + ".");
                continue;
            }
            try {
                int choice = Integer.parseInt(input);
                if (choice >= min && choice <= max) {
                    return choice;
                } else {
                    out.println("Choice out of range. Please enter a number between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                out.println("Invalid number format. Please enter digits only.");
            }
        }
    }

    /**
     * Reads a required non-blank string. Re-prompts until valid.
     */
    public String readNonBlank(String prompt) {
        while (true) {
            out.print(prompt + ": ");
            String input = readLineInternal();
            if (input == null) {
                throw new NoSuchElementException("End of input reached while reading: " + prompt);
            }
            input = input.trim();
            if (!input.isEmpty()) {
                return input;
            }
            out.println("Field cannot be blank. Please try again.");
        }
    }

    /**
     * Reads an optional string. Returns empty string if user presses Enter or on EOF.
     */
    public String readOptional(String prompt) {
        out.print(prompt + " (optional, press Enter to skip): ");
        String input = readLineInternal();
        if (input == null) {
            return "";
        }
        return input.trim();
    }

    /**
     * Reads and parses a date in yyyy-MM-dd format.
     * Re-prompts on format error or if date is in the future.
     */
    public LocalDate readDate(String prompt) {
        while (true) {
            out.print(prompt + " (yyyy-MM-dd): ");
            String input = readLineInternal();
            if (input == null) {
                throw new NoSuchElementException("End of input reached while reading date: " + prompt);
            }
            input = input.trim();
            if (input.isEmpty()) {
                out.println("Date cannot be blank. Expected format: yyyy-MM-dd.");
                continue;
            }
            try {
                LocalDate parsed = LocalDate.parse(input);
                if (parsed.isAfter(LocalDate.now())) {
                    out.println("Date cannot be in the future. Today is " + LocalDate.now() + ".");
                    continue;
                }
                return parsed;
            } catch (DateTimeParseException e) {
                out.println("Invalid date format ('" + input + "'). Please use yyyy-MM-dd (e.g. 2026-03-15).");
            }
        }
    }

    /**
     * Displays a numbered list of options from a String[] and prompts the user to choose one.
     * Traverses the options array using a loop.
     */
    public String readChoice(String prompt, String[] options) {
        if (options == null || options.length == 0) {
            throw new IllegalArgumentException("Options array cannot be null or empty.");
        }
        out.println(prompt + ":");
        for (int i = 0; i < options.length; i++) {
            out.printf("  %d. %s%n", (i + 1), options[i]);
        }
        int selectedIndex = readMenuChoice(1, options.length);
        if (selectedIndex == -1) {
            throw new NoSuchElementException("End of input reached while choosing option.");
        }
        return options[selectedIndex - 1];
    }
}
