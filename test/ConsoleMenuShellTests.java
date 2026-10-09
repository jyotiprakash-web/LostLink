import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import service.ItemService;
import util.InputHelper;

/**
 * Acceptance tests for F8: Console Menu Shell.
 */
public class ConsoleMenuShellTests {

    public static void run() {
        System.out.println("--- Running F8 Console Menu Shell Tests ---");

        // 1. Menu labels array verification
        TestRunner.assertEquals("Menu has exactly 9 options", 9, Main.MENU_LABELS.length);
        TestRunner.assertEquals("Option 1 label", "Report a lost item", Main.MENU_LABELS[0]);
        TestRunner.assertEquals("Option 9 label", "Save data and exit", Main.MENU_LABELS[8]);

        // 2. Scripted invalid input sequence followed by option 9 exit
        {
            String scripted = "bad\n0\n10\n \n8\n9\n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
            PrintStream captureOut = new PrintStream(outputCapture);

            InputHelper inputHelper = new InputHelper(reader, captureOut);
            ItemService service = new ItemService();
            Main app = new Main(service, inputHelper, captureOut, "data");

            app.run();

            String output = outputCapture.toString();
            TestRunner.check("App stopped running after option 9", !app.isRunning());
            TestRunner.check("Output contains Invalid number format message", output.contains("Invalid number format"));
            TestRunner.check("Output contains Choice out of range message", output.contains("Choice out of range"));
            TestRunner.check("Output contains Coming Soon message for option 8", output.contains("[Coming Soon] Statistics dashboard is not yet implemented."));
            TestRunner.check("Output contains Goodbye message", output.contains("Saving data and exiting LostLink. Goodbye!"));
        }

        // 3. Scripted EOF handling terminates without infinite loop
        {
            String scripted = "invalid\n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
            PrintStream captureOut = new PrintStream(outputCapture);

            InputHelper inputHelper = new InputHelper(reader, captureOut);
            ItemService service = new ItemService();
            Main app = new Main(service, inputHelper, captureOut, "data");

            app.run();

            String output = outputCapture.toString();
            TestRunner.check("App exited on EOF", !app.isRunning());
            TestRunner.check("Output indicates EOF exit", output.contains("[EOF] End of input detected. Exiting cleanly."));
        }
    }
}
