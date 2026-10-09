import exception.ItemNotFoundException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.LocalDate;
import model.FoundItem;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import service.ItemService;
import util.InputHelper;

/**
 * Acceptance tests for F10: Report a Found Item (menu 2).
 */
public class ReportFoundItemTests {

    public static void run() {
        System.out.println("--- Running F10 Report Found Item Tests ---");

        // 1. Scripted happy path report found item with private details
        String privateNote = "Contains student ID card for John and $40 cash in secret pocket";
        String scripted = "2\nLeather Wallet\n7\nBrown\nDining Hall\n2026-03-04\nBrown bifold wallet under table 12\n1\nSam Wilson\nsam@campus.edu\nSTU-9900\n"
                + privateNote + "\n9\n";

        BufferedReader reader = new BufferedReader(new StringReader(scripted));
        ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
        PrintStream captureOut = new PrintStream(outputCapture);

        InputHelper inputHelper = new InputHelper(reader, captureOut);
        ItemService service = new ItemService();
        Main app = new Main(service, inputHelper, captureOut, "data");

        app.run();

        String output = outputCapture.toString();
        TestRunner.check("Output contains found success message", output.contains("[Success] Found item report registered successfully!"));
        TestRunner.check("Output contains generated ID ITM-0001", output.contains("Generated Item ID: ITM-0001"));
        TestRunner.check("Console output strictly hides private details", !output.contains(privateNote));
        TestRunner.check("Console output strictly hides secret pocket mention", !output.contains("secret pocket"));

        TestRunner.assertEquals("Service contains 1 item", 1, service.getItemCount());

        try {
            Item item = service.getById("ITM-0001");
            TestRunner.check("Retrieved item is FoundItem instance", item instanceof FoundItem);
            TestRunner.assertEquals("Status is OPEN", ItemStatus.OPEN, item.getStatus());
            TestRunner.assertEquals("Item type is FOUND", "FOUND", item.getItemType());
            TestRunner.assertEquals("Category is OTHER", ItemCategory.OTHER, item.getCategory());
            TestRunner.assertEquals("Item location is Dining Hall", "Dining Hall", item.getLocation());
            TestRunner.assertEquals("Item date matches", LocalDate.of(2026, 3, 4), item.getDate());

            FoundItem foundItem = (FoundItem) item;
            TestRunner.assertEquals("Private details preserved internally", privateNote, foundItem.getPrivateDetails());
            TestRunner.check("Public summary strictly hides private details", !foundItem.getPublicSummary().contains(privateNote));
        } catch (ItemNotFoundException e) {
            TestRunner.check("Found item lookup failed: " + e.getMessage(), false);
        }
    }
}
