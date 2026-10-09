import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.LocalDate;
import util.InputHelper;

/**
 * Acceptance tests for F6: InputHelper.
 */
public class InputHelperTests {

    public static void run() {
        System.out.println("--- Running F6 InputHelper Tests ---");

        // 1. readMenuChoice with invalid text, out-of-range numbers, then valid
        {
            String scripted = "letters\n0\n10\n  \n3\n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            PrintStream nullOut = new PrintStream(new ByteArrayOutputStream());
            InputHelper helper = new InputHelper(reader, nullOut);

            int choice = helper.readMenuChoice(1, 9);
            TestRunner.assertEquals("readMenuChoice re-prompts on letters/range and accepts valid 3", 3, choice);
        }

        // 2. readMenuChoice EOF handling
        {
            String scripted = "bad\n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            PrintStream nullOut = new PrintStream(new ByteArrayOutputStream());
            InputHelper helper = new InputHelper(reader, nullOut);

            int choice = helper.readMenuChoice(1, 9);
            TestRunner.assertEquals("readMenuChoice returns -1 on EOF", -1, choice);
            TestRunner.check("helper.isEof() is true", helper.isEof());
        }

        // 3. readNonBlank with blank and whitespace lines, then valid input
        {
            String scripted = "\n   \n\t\n  MacBook Pro  \n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            PrintStream nullOut = new PrintStream(new ByteArrayOutputStream());
            InputHelper helper = new InputHelper(reader, nullOut);

            String text = helper.readNonBlank("Item name");
            TestRunner.assertEquals("readNonBlank trims and accepts valid text after blanks", "MacBook Pro", text);
        }

        // 4. readOptional with empty and populated lines
        {
            String scripted = "\nPrivate notes\n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            PrintStream nullOut = new PrintStream(new ByteArrayOutputStream());
            InputHelper helper = new InputHelper(reader, nullOut);

            String opt1 = helper.readOptional("Notes");
            TestRunner.assertEquals("readOptional empty string on Enter", "", opt1);
            String opt2 = helper.readOptional("Notes 2");
            TestRunner.assertEquals("readOptional returns trimmed text", "Private notes", opt2);
        }

        // 5. readDate with invalid format, month 13 (DateTimeParseException), future date, then valid date
        {
            String scripted = "invalid-date\n2026-13-45\n2099-12-31\n2026-01-15\n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            PrintStream nullOut = new PrintStream(new ByteArrayOutputStream());
            InputHelper helper = new InputHelper(reader, nullOut);

            LocalDate date = helper.readDate("Lost Date");
            TestRunner.assertEquals("readDate parses valid date after rejecting invalid strings", LocalDate.of(2026, 1, 15), date);
        }

        // 6. readChoice from String[] with loop and re-prompting
        {
            String[] categories = new String[]{"ELECTRONICS", "BOOKS", "BAGS"};
            String scripted = "99\n2\n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            PrintStream nullOut = new PrintStream(new ByteArrayOutputStream());
            InputHelper helper = new InputHelper(reader, nullOut);

            String chosen = helper.readChoice("Select Category", categories);
            TestRunner.assertEquals("readChoice returns selected option from array", "BOOKS", chosen);
        }
    }
}
