import exception.InvalidItemException;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.FoundItem;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.Student;
import model.User;

/**
 * Acceptance tests for F4: Item hierarchy (Item, LostItem, FoundItem).
 */
public class ItemTests {

    public static void run() {
        System.out.println("--- Running F4 Item Hierarchy Tests ---");

        // 1. Item class is abstract
        TestRunner.check("Item class is abstract", Modifier.isAbstract(Item.class.getModifiers()));

        User reporter = new Student("Alice", "alice@campus.edu", "S101");
        LocalDate validDate = LocalDate.now().minusDays(1);

        // 2. Valid construction
        try {
            LostItem lost = new LostItem("ITM-0001", "MacBook Pro", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", validDate, "Laptop in black sleeve", reporter);
            TestRunner.assertEquals("LostItem ID", "ITM-0001", lost.getId());
            TestRunner.assertEquals("LostItem name", "MacBook Pro", lost.getName());
            TestRunner.assertEquals("LostItem category", ItemCategory.ELECTRONICS, lost.getCategory());
            TestRunner.assertEquals("LostItem color", "Silver", lost.getColor());
            TestRunner.assertEquals("LostItem location", "Library 2nd Floor", lost.getLocation());
            TestRunner.assertEquals("LostItem status", ItemStatus.OPEN, lost.getStatus());
            TestRunner.assertEquals("LostItem type", "LOST", lost.getItemType());
        } catch (InvalidItemException e) {
            TestRunner.check("Valid LostItem construction failed unexpectedly: " + e.getMessage(), false);
        }

        // 3. Blank required field throws InvalidItemException
        TestRunner.assertThrows("Blank ID throws InvalidItemException", InvalidItemException.class, () -> {
            new LostItem("   ", "MacBook", ItemCategory.ELECTRONICS, "Silver", "Library", validDate, "desc", reporter);
        });
        TestRunner.assertThrows("Blank name throws InvalidItemException", InvalidItemException.class, () -> {
            new LostItem("ITM-0002", "", ItemCategory.ELECTRONICS, "Silver", "Library", validDate, "desc", reporter);
        });
        TestRunner.assertThrows("Null category throws InvalidItemException", InvalidItemException.class, () -> {
            new LostItem("ITM-0002", "MacBook", null, "Silver", "Library", validDate, "desc", reporter);
        });
        TestRunner.assertThrows("Blank color throws InvalidItemException", InvalidItemException.class, () -> {
            new LostItem("ITM-0002", "MacBook", ItemCategory.ELECTRONICS, "  ", "Library", validDate, "desc", reporter);
        });
        TestRunner.assertThrows("Blank location throws InvalidItemException", InvalidItemException.class, () -> {
            new LostItem("ITM-0002", "MacBook", ItemCategory.ELECTRONICS, "Silver", " ", validDate, "desc", reporter);
        });
        TestRunner.assertThrows("Null date throws InvalidItemException", InvalidItemException.class, () -> {
            new LostItem("ITM-0002", "MacBook", ItemCategory.ELECTRONICS, "Silver", "Library", null, "desc", reporter);
        });
        TestRunner.assertThrows("Blank description throws InvalidItemException", InvalidItemException.class, () -> {
            new LostItem("ITM-0002", "MacBook", ItemCategory.ELECTRONICS, "Silver", "Library", validDate, "   ", reporter);
        });
        TestRunner.assertThrows("Null reporter throws InvalidItemException", InvalidItemException.class, () -> {
            new LostItem("ITM-0002", "MacBook", ItemCategory.ELECTRONICS, "Silver", "Library", validDate, "desc", null);
        });

        // 4. Future dates are rejected
        LocalDate futureDate = LocalDate.now().plusDays(2);
        TestRunner.assertThrows("Future date throws InvalidItemException", InvalidItemException.class, () -> {
            new LostItem("ITM-0003", "Keys", ItemCategory.ACCESSORIES, "Silver", "Cafeteria", futureDate, "Brass keys", reporter);
        });

        // 5. Polymorphic List<Item> returns correct getItemType()
        try {
            Item lostItem = new LostItem("ITM-0004", "Water Bottle", ItemCategory.OTHER, "Blue", "Gym", validDate, "Metal bottle", reporter);
            Item foundItem = new FoundItem("ITM-0005", "Blue Bottle", ItemCategory.OTHER, "Blue", "Gym Bench", validDate, "Hydro Flask",
                    ItemStatus.OPEN, reporter, reporter.getContact(), "Contains sticker with phone number");

            List<Item> items = new ArrayList<>();
            items.add(lostItem);
            items.add(foundItem);

            TestRunner.assertEquals("Polymorphic call 0 is LOST", "LOST", items.get(0).getItemType());
            TestRunner.assertEquals("Polymorphic call 1 is FOUND", "FOUND", items.get(1).getItemType());

            // 6. privateDetails is strictly absent from public summary and toString
            String publicSummary = foundItem.getPublicSummary();
            String toString = foundItem.toString();
            TestRunner.check("privateDetails absent from getPublicSummary", !publicSummary.contains("Contains sticker"));
            TestRunner.check("privateDetails absent from toString", !toString.contains("Contains sticker"));
            TestRunner.assertEquals("privateDetails retrieved via getter for verification only", "Contains sticker with phone number", ((FoundItem) foundItem).getPrivateDetails());
        } catch (InvalidItemException e) {
            TestRunner.check("Polymorphic item test threw unexpectedly: " + e.getMessage(), false);
        }
    }
}
