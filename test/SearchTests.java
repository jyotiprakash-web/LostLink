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
 * Acceptance tests for F11: Search all records (menu 4).
 */
public class SearchTests {

    public static void run() {
        System.out.println("--- Running F11 Search Records Tests ---");

        ItemService service = new ItemService();
        User user = new Student("Alice", "alice@campus.edu", "S101");
        LocalDate date = LocalDate.now();

        // 1. Empty item list search
        List<Item> emptyResults = service.search("lap", null, null, null, null);
        TestRunner.check("Empty service returns empty search result", emptyResults.isEmpty());

        // Setup test data
        try {
            LostItem laptop = service.reportLostItem("MacBook Laptop", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", date, "A slim silver laptop", user, "", "Near desk 4");
            FoundItem bottle = service.reportFoundItem("Hydro Flask Bottle", ItemCategory.OTHER,
                    "Blue", "Gym Bench", date, "Water bottle with stickers", user, "", "Has campus gym sticker");
            FoundItem wallet = service.reportFoundItem("Leather Wallet", ItemCategory.OTHER,
                    "Brown", "Cafeteria", date, "Lost bifold wallet", user, "", "Secret confidential note with $100");

            // 2. Case-insensitivity: lap, LAP, LaP all match "MacBook Laptop"
            List<Item> r1 = service.search("lap", null, null, null, null);
            List<Item> r2 = service.search("LAP", null, null, null, null);
            List<Item> r3 = service.search("LaP", null, null, null, null);

            TestRunner.assertEquals("Search 'lap' found 1 item", 1, r1.size());
            TestRunner.assertEquals("Search 'lap' item name", "MacBook Laptop", r1.get(0).getName());
            TestRunner.assertEquals("Search 'LAP' found 1 item", 1, r2.size());
            TestRunner.assertEquals("Search 'LaP' found 1 item", 1, r3.size());

            // 3. Category filter alone
            List<Item> catElectronics = service.search(null, ItemCategory.ELECTRONICS, null, null, null);
            TestRunner.assertEquals("Category ELECTRONICS alone found 1 item", 1, catElectronics.size());
            List<Item> catOther = service.search(null, ItemCategory.OTHER, null, null, null);
            TestRunner.assertEquals("Category OTHER alone found 2 items", 2, catOther.size());

            // 4. Color filter alone
            List<Item> colorSilver = service.search(null, null, "silver", null, null);
            TestRunner.assertEquals("Color 'silver' alone found 1 item", 1, colorSilver.size());
            List<Item> colorBlue = service.search(null, null, "blue", null, null);
            TestRunner.assertEquals("Color 'blue' alone found 1 item", 1, colorBlue.size());

            // 5. Location filter alone
            List<Item> locLibrary = service.search(null, null, null, "library", null);
            TestRunner.assertEquals("Location 'library' found 1 item", 1, locLibrary.size());
            List<Item> locGym = service.search(null, null, null, "gym", null);
            TestRunner.assertEquals("Location 'gym' found 1 item", 1, locGym.size());

            // 6. Status filter alone
            wallet.setStatus(ItemStatus.CLAIM_PENDING);
            List<Item> statusClaimPending = service.search(null, null, null, null, ItemStatus.CLAIM_PENDING);
            TestRunner.assertEquals("Status CLAIM_PENDING found 1 item", 1, statusClaimPending.size());
            TestRunner.assertEquals("Status CLAIM_PENDING is wallet", "Leather Wallet", statusClaimPending.get(0).getName());

            List<Item> statusOpen = service.search(null, null, null, null, ItemStatus.OPEN);
            TestRunner.assertEquals("Status OPEN found 2 items", 2, statusOpen.size());

            // 7. Combined filters
            List<Item> combinedMatch = service.search("laptop", ItemCategory.ELECTRONICS, "silver", "library", ItemStatus.OPEN);
            TestRunner.assertEquals("Combined matching filters return 1 item", 1, combinedMatch.size());

            List<Item> combinedMismatch = service.search("laptop", ItemCategory.OTHER, "silver", "library", ItemStatus.OPEN);
            TestRunner.assertEquals("Combined mismatching filters return 0 items", 0, combinedMismatch.size());

            // 8. No-results check
            List<Item> noResults = service.search("nonexistent_item_xyz", null, null, null, null);
            TestRunner.assertEquals("No results query returns empty list", 0, noResults.size());

            // 9. Confidentiality check: searching found items never displays private details
            for (Item item : service.getAllItems()) {
                TestRunner.check("Search public summary hides confidential note", !item.getPublicSummary().contains("Secret confidential note"));
            }

            // 10. Console-level search check
            {
                // Scripted input: 4 (search) -> keyword "lap" -> blank cat -> blank color -> blank loc -> blank status -> 9 (exit)
                String scripted = "4\nlap\n\n\n\n\n9\n";
                BufferedReader reader = new BufferedReader(new StringReader(scripted));
                ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
                PrintStream captureOut = new PrintStream(outputCapture);

                InputHelper inputHelper = new InputHelper(reader, captureOut);
                Main app = new Main(service, inputHelper, captureOut, "data");
                app.run();

                String outStr = outputCapture.toString();
                TestRunner.check("Console search outputs MacBook Laptop", outStr.contains("MacBook Laptop"));
                TestRunner.check("Console search hides private details", !outStr.contains("Secret confidential note"));
            }

        } catch (InvalidItemException e) {
            TestRunner.check("Setup for search tests threw unexpectedly: " + e.getMessage(), false);
        }
    }
}
