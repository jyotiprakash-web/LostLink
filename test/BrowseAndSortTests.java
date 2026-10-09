import exception.InvalidItemException;
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
 * Acceptance tests for F12: Browse and sort (menu 5).
 */
public class BrowseAndSortTests {

    public static void run() {
        System.out.println("--- Running F12 Browse and Sort Tests ---");

        ItemService service = new ItemService();
        User user = new Student("Alice", "alice@campus.edu", "S101");

        // 1. Empty list browse
        List<Item> emptyBrowse = service.browse("ALL", null, ItemService.SORT_BY_DATE_DESC);
        TestRunner.check("Empty service browse returns empty list", emptyBrowse.isEmpty());

        // Setup 3 items with distinct dates and alphabetical names
        try {
            LostItem itemA = service.reportLostItem("Zebra Scarf", ItemCategory.CLOTHING,
                    "Black/White", "Auditorium", LocalDate.of(2026, 3, 1), "Striped wool scarf", user, "", "");
            FoundItem itemB = service.reportFoundItem("Apple Watch", ItemCategory.ELECTRONICS,
                    "Black", "Sports Complex", LocalDate.of(2026, 3, 10), "Smartwatch with silicone strap", user, "", "Private engraving 'AW-2025'");
            LostItem itemC = service.reportLostItem("Calculus Book", ItemCategory.BOOKS,
                    "Blue", "Math Dept", LocalDate.of(2026, 3, 5), "Hardcover textbook", user, "", "");

            itemC.setStatus(ItemStatus.RETURNED);

            // 2. Sort by date descending (newest first)
            List<Item> byDateDesc = service.browse("ALL", null, ItemService.SORT_BY_DATE_DESC);
            TestRunner.assertEquals("byDateDesc size is 3", 3, byDateDesc.size());
            TestRunner.assertEquals("1st newest is Apple Watch (March 10)", "Apple Watch", byDateDesc.get(0).getName());
            TestRunner.assertEquals("2nd newest is Calculus Book (March 5)", "Calculus Book", byDateDesc.get(1).getName());
            TestRunner.assertEquals("3rd newest is Zebra Scarf (March 1)", "Zebra Scarf", byDateDesc.get(2).getName());

            // 3. Sort by date ascending (oldest first)
            List<Item> byDateAsc = service.browse("ALL", null, ItemService.SORT_BY_DATE_ASC);
            TestRunner.assertEquals("1st oldest is Zebra Scarf (March 1)", "Zebra Scarf", byDateAsc.get(0).getName());
            TestRunner.assertEquals("2nd oldest is Calculus Book (March 5)", "Calculus Book", byDateAsc.get(1).getName());
            TestRunner.assertEquals("3rd oldest is Apple Watch (March 10)", "Apple Watch", byDateAsc.get(2).getName());

            // 4. Sort by name ascending (A-Z)
            List<Item> byNameAsc = service.browse("ALL", null, ItemService.SORT_BY_NAME_ASC);
            TestRunner.assertEquals("1st alphabetically is Apple Watch", "Apple Watch", byNameAsc.get(0).getName());
            TestRunner.assertEquals("2nd alphabetically is Calculus Book", "Calculus Book", byNameAsc.get(1).getName());
            TestRunner.assertEquals("3rd alphabetically is Zebra Scarf", "Zebra Scarf", byNameAsc.get(2).getName());

            // 5. Filter by type
            List<Item> lostOnly = service.browse("LOST", null, null);
            TestRunner.assertEquals("Filter LOST only has 2 items", 2, lostOnly.size());
            TestRunner.check("Item A in lostOnly", lostOnly.contains(itemA));
            TestRunner.check("Item C in lostOnly", lostOnly.contains(itemC));

            List<Item> foundOnly = service.browse("FOUND", null, null);
            TestRunner.assertEquals("Filter FOUND only has 1 item", 1, foundOnly.size());
            TestRunner.assertEquals("Found item is Apple Watch", "Apple Watch", foundOnly.get(0).getName());

            // 6. Filter by status
            List<Item> returnedOnly = service.browse("ALL", ItemStatus.RETURNED, null);
            TestRunner.assertEquals("Filter RETURNED only has 1 item", 1, returnedOnly.size());
            TestRunner.assertEquals("Returned item is Calculus Book", "Calculus Book", returnedOnly.get(0).getName());

            List<Item> openOnly = service.browse("ALL", ItemStatus.OPEN, null);
            TestRunner.assertEquals("Filter OPEN only has 2 items", 2, openOnly.size());

            // 7. Combined type and status filter
            List<Item> lostOpenOnly = service.browse("LOST", ItemStatus.OPEN, null);
            TestRunner.assertEquals("Filter LOST and OPEN has 1 item", 1, lostOpenOnly.size());
            TestRunner.assertEquals("Item is Zebra Scarf", "Zebra Scarf", lostOpenOnly.get(0).getName());

            // 8. Console scripted check for Browse menu option 5
            {
                // Scripted: 5 (browse) -> 1 (All items) -> 1 (All statuses) -> 3 (Name A-Z) -> 9 (exit)
                String scripted = "5\n1\n1\n3\n9\n";
                BufferedReader reader = new BufferedReader(new StringReader(scripted));
                ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
                PrintStream captureOut = new PrintStream(outputCapture);

                InputHelper inputHelper = new InputHelper(reader, captureOut);
                Main app = new Main(service, inputHelper, captureOut, "data");
                app.run();

                String outStr = outputCapture.toString();
                TestRunner.check("Browse output contains Apple Watch", outStr.contains("Apple Watch"));
                TestRunner.check("Browse output contains Calculus Book", outStr.contains("Calculus Book"));
                TestRunner.check("Browse output contains Zebra Scarf", outStr.contains("Zebra Scarf"));
                // Check ordering: Apple Watch appears before Calculus Book, which appears before Zebra Scarf
                int idxApple = outStr.indexOf("Apple Watch");
                int idxCalc = outStr.indexOf("Calculus Book");
                int idxZebra = outStr.indexOf("Zebra Scarf");
                TestRunner.check("Alphabetical browse order preserved in console output", idxApple < idxCalc && idxCalc < idxZebra);
            }

        } catch (InvalidItemException e) {
            TestRunner.check("Setup for browse tests threw unexpectedly: " + e.getMessage(), false);
        }
    }
}
