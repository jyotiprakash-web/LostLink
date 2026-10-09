import exception.ItemNotFoundException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.LocalDate;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import service.ItemService;
import util.InputHelper;

/**
 * Acceptance tests for F9: Report a Lost Item (menu 1).
 */
public class ReportLostItemTests {

    public static void run() {
        System.out.println("--- Running F9 Report Lost Item Tests ---");

        // 1. Scripted happy path report lost item
        {
            // Flow:
            // Choice 1
            // Name: MacBook Pro
            // Category: 1 (ELECTRONICS)
            // Color: Silver
            // Location: Library 2nd Floor
            // Last-seen details: Near table 4
            // Date: 2026-03-01
            // Description: Silver laptop with blue case
            // Role: 1 (Student)
            // Reporter Name: Alice Smith
            // Contact: alice@campus.edu
            // Student ID: STU-8821
            // Choice 9 (Exit)
            String scripted = "1\nMacBook Pro\n1\nSilver\nLibrary 2nd Floor\nNear table 4\n2026-03-01\nSilver laptop with blue case\n1\nAlice Smith\nalice@campus.edu\nSTU-8821\n9\n";

            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
            PrintStream captureOut = new PrintStream(outputCapture);

            InputHelper inputHelper = new InputHelper(reader, captureOut);
            ItemService service = new ItemService();
            Main app = new Main(service, inputHelper, captureOut, "data");

            app.run();

            String output = outputCapture.toString();
            TestRunner.check("Output contains success message", output.contains("[Success] Lost item report created successfully!"));
            TestRunner.check("Output displays generated ID ITM-0001", output.contains("Generated Item ID: ITM-0001"));
            TestRunner.assertEquals("Service has 1 item", 1, service.getItemCount());

            try {
                Item item = service.getById("ITM-0001");
                TestRunner.check("Retrieved item is LostItem", item instanceof LostItem);
                TestRunner.assertEquals("Status is OPEN", ItemStatus.OPEN, item.getStatus());
                TestRunner.assertEquals("Category is ELECTRONICS", ItemCategory.ELECTRONICS, item.getCategory());
                TestRunner.assertEquals("Item name matches", "MacBook Pro", item.getName());
                TestRunner.assertEquals("Item color matches", "Silver", item.getColor());
                TestRunner.assertEquals("Date matches", LocalDate.of(2026, 3, 1), item.getDate());
                TestRunner.assertEquals("Reporter role", "Student (ID: STU-8821)", item.getReporter().displayRole());
                TestRunner.assertEquals("Last seen details", "Near table 4", ((LostItem) item).getLastSeenLocationDetails());
            } catch (ItemNotFoundException e) {
                TestRunner.check("Created item was not found by ID: " + e.getMessage(), false);
            }
        }

        // 2. Re-prompt on blank inputs and invalid dates
        {
            // Name: \n \nKeys\n
            // Category: 6 (ACCESSORIES)
            // Color: Brass
            // Location: Gym Locker
            // Details: \n
            // Date: invalid-date\n2026-13-40\n2026-03-05\n
            // Description: Set of locker keys
            // Role: 2 (Staff)
            // Reporter Name: Officer Davis
            // Contact: davis@campus.edu
            // Staff ID: SEC-01
            // 9 (Exit)
            String scripted = "1\n\n  \nKeys\n6\nBrass\nGym Locker\n\ninvalid-date\n2026-13-40\n2026-03-05\nSet of locker keys\n2\nOfficer Davis\ndavis@campus.edu\nSEC-01\n9\n";

            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
            PrintStream captureOut = new PrintStream(outputCapture);

            InputHelper inputHelper = new InputHelper(reader, captureOut);
            ItemService service = new ItemService();
            Main app = new Main(service, inputHelper, captureOut, "data");

            app.run();

            String output = outputCapture.toString();
            TestRunner.check("Output shows re-prompt for blank", output.contains("Field cannot be blank. Please try again."));
            TestRunner.check("Output shows re-prompt for invalid date", output.contains("Invalid date format"));
            TestRunner.check("Report created successfully after re-prompts", output.contains("[Success] Lost item report created successfully!"));

            try {
                Item item = service.getById("ITM-0001");
                TestRunner.assertEquals("Item name", "Keys", item.getName());
                TestRunner.assertEquals("Category is ACCESSORIES", ItemCategory.ACCESSORIES, item.getCategory());
                TestRunner.assertEquals("Reporter role", "Staff (ID: SEC-01)", item.getReporter().displayRole());
            } catch (ItemNotFoundException e) {
                TestRunner.check("Item not found: " + e.getMessage(), false);
            }
        }
    }
}
