import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import model.FoundItem;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.Student;
import model.User;
import service.ItemService;
import util.InputHelper;

/**
 * Acceptance tests for F19: My reports (menu 7).
 */
public class MyReportsTests {

    public static void run() {
        System.out.println("--- Running F19 My Reports Tests ---");

        ItemService service = new ItemService();
        User userAlice = new Student("Alice Cooper", "alice@campus.edu", "S101");
        User userBob = new Student("Bob Builder", "bob@campus.edu", "S102");
        LocalDate today = LocalDate.now();

        try {
            LostItem itemAlice1 = service.reportLostItem("Backpack", ItemCategory.BAGS, "Black",
                    "Library", today, "School bag", userAlice, "", "Near lockers");
            FoundItem itemAlice2 = service.reportFoundItem("Umbrella", ItemCategory.ACCESSORIES, "Red",
                    "Cafeteria", today, "Red umbrella", userAlice, "", "Has tag 'Cooper'");
            LostItem itemBob = service.reportLostItem("Calculator", ItemCategory.ELECTRONICS, "Gray",
                    "Math Lab", today, "TI-84", userBob, "", "");

            // 1. Lookup by name (case-insensitive)
            List<Item> byNameLower = service.getReportsByReporterName("alice cooper");
            List<Item> byNameUpper = service.getReportsByReporterName("ALICE");
            List<Item> byNamePartial = service.getReportsByReporterName("Cooper");

            TestRunner.assertEquals("Lookup 'alice cooper' found 2 items", 2, byNameLower.size());
            TestRunner.assertEquals("Lookup 'ALICE' found 2 items", 2, byNameUpper.size());
            TestRunner.assertEquals("Lookup 'Cooper' found 2 items", 2, byNamePartial.size());
            TestRunner.assertEquals("Bob lookup found 1 item", 1, service.getReportsByReporterName("bob").size());

            // 2. Lookup by ID
            Item retrieved = service.getById(itemAlice1.getId());
            TestRunner.assertEquals("Lookup by ID matches itemAlice1", itemAlice1, retrieved);

            // 3. Update works for an OPEN item
            service.updateOpenItemDetails(itemAlice1.getId(), "Dark Black", "Library 3rd Floor",
                    "Updated description: school bag with patches", "alice-new@campus.edu", "Near corner desk");

            TestRunner.assertEquals("Updated color", "Dark Black", itemAlice1.getColor());
            TestRunner.assertEquals("Updated location", "Library 3rd Floor", itemAlice1.getLocation());
            TestRunner.assertEquals("Updated description", "Updated description: school bag with patches", itemAlice1.getDescription());
            TestRunner.assertEquals("Updated contact", "alice-new@campus.edu", itemAlice1.getContact());
            TestRunner.assertEquals("Updated last seen details", "Near corner desk", itemAlice1.getLastSeenLocationDetails());

            // 4. Update is refused for a CLAIM_PENDING item
            itemAlice1.setStatus(ItemStatus.CLAIM_PENDING);
            TestRunner.assertThrows("Updating CLAIM_PENDING item throws InvalidItemException", InvalidItemException.class, () -> {
                service.updateOpenItemDetails(itemAlice1.getId(), "Blue", "Gym", "desc", "contact", "");
            });

            // 5. Update is refused for a RETURNED item
            itemAlice1.setStatus(ItemStatus.RETURNED);
            TestRunner.assertThrows("Updating RETURNED item throws InvalidItemException", InvalidItemException.class, () -> {
                service.updateOpenItemDetails(itemAlice1.getId(), "Blue", "Gym", "desc", "contact", "");
            });
            itemAlice1.setStatus(ItemStatus.OPEN); // restore

            // 6. Unknown ID throws ItemNotFoundException
            TestRunner.assertThrows("Updating unknown ID throws ItemNotFoundException", ItemNotFoundException.class, () -> {
                service.updateOpenItemDetails("ITM-9999", "Blue", "Gym", "desc", "contact", "");
            });

            // 7. Console-level check for Menu 7
            {
                // Scripted input:
                // 7 (My reports)
                // 1 (Lookup by reporter name)
                // "alice"
                // "y" (update an open item)
                // ITM-0002
                // "Crimson" (new color)
                // "" (keep location)
                // "" (keep description)
                // "" (keep contact)
                // "Has new tag Cooper 2026" (new private details)
                // 9 (Exit)
                String scripted = "7\n1\nalice\ny\n" + itemAlice2.getId() + "\nCrimson\n\n\n\nHas new tag Cooper 2026\n9\n";

                BufferedReader reader = new BufferedReader(new StringReader(scripted));
                ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
                PrintStream captureOut = new PrintStream(outputCapture);

                InputHelper inputHelper = new InputHelper(reader, captureOut);
                Main app = new Main(service, inputHelper, captureOut, "data");
                app.run();

                String outStr = outputCapture.toString();
                TestRunner.check("Console output shows found reports for Alice", outStr.contains("[My Reports for 'alice'] Found: 2"));
                TestRunner.check("Console output confirms item update success", outStr.contains("details updated successfully!"));
                TestRunner.assertEquals("itemAlice2 color updated to Crimson", "Crimson", itemAlice2.getColor());
            }

        } catch (InvalidItemException | ItemNotFoundException e) {
            TestRunner.check("MyReports test setup threw unexpectedly: " + e.getMessage(), false);
        }
    }
}
