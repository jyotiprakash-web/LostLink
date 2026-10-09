import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.FoundItem;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.Student;
import model.User;
import service.ItemService;

/**
 * Acceptance tests for F7: ItemService Core.
 */
public class ItemServiceTests {

    public static void run() {
        System.out.println("--- Running F7 ItemService Core Tests ---");

        ItemService service = new ItemService();
        User reporter = new Student("Bob", "bob@campus.edu", "S202");
        LocalDate date = LocalDate.now();

        // 1. Safe handling of empty collections
        TestRunner.assertEquals("Empty service count is 0", 0, service.getItemCount());
        TestRunner.check("getAllItems is empty and non-null", service.getAllItems().isEmpty());
        TestRunner.check("getLostItems is empty", service.getLostItems().isEmpty());
        TestRunner.check("getFoundItems is empty", service.getFoundItems().isEmpty());

        // 2. Report Lost and Found items
        try {
            LostItem lost = service.reportLostItem("Blue Umbrella", ItemCategory.ACCESSORIES, "Blue",
                    "Library entrance", date, "Compact folding umbrella", reporter, "", "Near coat rack");
            TestRunner.assertEquals("First item ID", "ITM-0001", lost.getId());
            TestRunner.assertEquals("Service count after 1 add", 1, service.getItemCount());

            FoundItem found = service.reportFoundItem("Wallet", ItemCategory.OTHER, "Brown",
                    "Cafeteria", date, "Leather bifold wallet", reporter, "", "Contains ID card for Sam");
            TestRunner.assertEquals("Second item ID", "ITM-0002", found.getId());
            TestRunner.assertEquals("Service count after 2 adds", 2, service.getItemCount());

            // 3. Consistency between list and map
            Item retrievedLost = service.getById("ITM-0001");
            Item retrievedFound = service.getById("ITM-0002");
            TestRunner.assertEquals("Retrieved lost item matches", lost, retrievedLost);
            TestRunner.assertEquals("Retrieved found item matches", found, retrievedFound);
            TestRunner.assertEquals("getAllItems contains 2 items", 2, service.getAllItems().size());
            TestRunner.assertEquals("getLostItems contains 1 item", 1, service.getLostItems().size());
            TestRunner.assertEquals("getFoundItems contains 1 item", 1, service.getFoundItems().size());

        } catch (Exception e) {
            TestRunner.check("Item creation threw unexpectedly: " + e.getMessage(), false);
        }

        // 4. Unknown ID throws ItemNotFoundException
        TestRunner.assertThrows("Unknown ID throws ItemNotFoundException", ItemNotFoundException.class, () -> {
            service.getById("ITM-9999");
        });
        TestRunner.assertThrows("Blank ID throws ItemNotFoundException", ItemNotFoundException.class, () -> {
            service.getById("   ");
        });

        // 5. Duplicate ID is rejected
        TestRunner.assertThrows("Duplicate ID throws InvalidItemException", InvalidItemException.class, () -> {
            LostItem duplicate = new LostItem("ITM-0001", "Another umbrella", ItemCategory.ACCESSORIES,
                    "Blue", "Library", date, "desc", reporter);
            service.addItem(duplicate);
        });

        // 6. Mass creation verifies unique IDs and sequence maintenance
        Set<String> generatedIds = new HashSet<>();
        generatedIds.add("ITM-0001");
        generatedIds.add("ITM-0002");
        for (int i = 3; i <= 25; i++) {
            try {
                Item item = service.reportLostItem("Item " + i, ItemCategory.OTHER, "Color " + i,
                        "Room " + i, date, "Description " + i, reporter, "", "");
                TestRunner.check("ID is unique: " + item.getId(), !generatedIds.contains(item.getId()));
                generatedIds.add(item.getId());
            } catch (InvalidItemException e) {
                TestRunner.check("Mass item creation failed: " + e.getMessage(), false);
            }
        }
        TestRunner.assertEquals("Total unique items generated", 25, service.getItemCount());

        // 7. Status update safety
        try {
            service.updateItemStatus("ITM-0001", ItemStatus.CLAIM_PENDING);
            TestRunner.assertEquals("Status updated in map/list", ItemStatus.CLAIM_PENDING, service.getById("ITM-0001").getStatus());
        } catch (ItemNotFoundException e) {
            TestRunner.check("Status update failed: " + e.getMessage(), false);
        }
    }
}
