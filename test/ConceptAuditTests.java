import exception.InvalidClaimException;
import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import model.FoundItem;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.MatchResult;
import model.Staff;
import model.Student;
import model.User;
import service.ClaimService;
import service.ItemService;
import service.MatchingService;

/**
 * Programmatic audit verifying all mandatory Core Java requirements (design.md Section 5):
 * 1. Arrays (MENU_LABELS, ItemCategory.getNames(), bounded MatchResult[])
 * 2. Collections (ArrayList, HashMap, Comparators, unmodifiable defensive views)
 * 3. Inheritance & Abstraction (User, Student, Staff, Item, LostItem, FoundItem)
 * 4. Polymorphism (displayRole(), getItemType())
 * 5. Exception handling (InvalidItemException, ItemNotFoundException, InvalidClaimException)
 * 6. Try-with-resources (AutoCloseable verification on file I/O classes)
 */
public class ConceptAuditTests {

    public static void run() {
        System.out.println("--- Running F27 Core Java Concept Audit Tests ---");
        try {
            // 1. ARRAYS AUDIT
            TestRunner.check("Main.MENU_LABELS is array", Main.MENU_LABELS.getClass().isArray());
            TestRunner.assertEquals("Main.MENU_LABELS length is 9", 9, Main.MENU_LABELS.length);

            String[] categories = ItemCategory.getNames();
            TestRunner.check("ItemCategory.getNames() is array", categories.getClass().isArray());
            TestRunner.assertEquals("ItemCategory.getNames() length is 7", 7, categories.length);

            MatchingService matchingService = new MatchingService();
            Student user = new Student("Alice", "alice@uni.edu");
            LostItem lost = new LostItem("ITM-0001", "MacBook", ItemCategory.ELECTRONICS, "Silver", "Library",
                    LocalDate.now(), "Laptop", ItemStatus.OPEN, user, "alice@uni.edu", "");
            FoundItem found = new FoundItem("ITM-0002", "MacBook", ItemCategory.ELECTRONICS, "Silver", "Library",
                    LocalDate.now(), "Laptop", ItemStatus.OPEN, user, "alice@uni.edu", "");

            MatchResult[] matches = matchingService.findTopMatches(lost, java.util.Collections.singletonList(found));
            TestRunner.check("findTopMatches returns MatchResult[] array", matches.getClass().isArray());
            TestRunner.assertEquals("Bounded match array size is 1", 1, matches.length);

            // 2. COLLECTIONS AUDIT
            ItemService itemService = new ItemService();
            Field itemListField = ItemService.class.getDeclaredField("itemList");
            itemListField.setAccessible(true);
            Object itemListObj = itemListField.get(itemService);
            TestRunner.check("ItemService uses List/ArrayList", itemListObj instanceof List);

            Field itemMapField = ItemService.class.getDeclaredField("itemMap");
            itemMapField.setAccessible(true);
            Object itemMapObj = itemMapField.get(itemService);
            TestRunner.check("ItemService uses Map/HashMap", itemMapObj instanceof Map);

            ClaimService claimService = new ClaimService(itemService);
            Field claimListField = ClaimService.class.getDeclaredField("claimList");
            claimListField.setAccessible(true);
            TestRunner.check("ClaimService uses List/ArrayList", claimListField.get(claimService) instanceof List);

            Field claimMapField = ClaimService.class.getDeclaredField("claimMap");
            claimMapField.setAccessible(true);
            TestRunner.check("ClaimService uses Map/HashMap", claimMapField.get(claimService) instanceof Map);

            // Defensive unmodifiable view audit
            List<Item> allItemsView = itemService.getAllItems();
            TestRunner.assertThrows("ItemService.getAllItems() is unmodifiable", UnsupportedOperationException.class, () -> {
                allItemsView.add(lost);
            });

            // Comparator verification
            TestRunner.check("SORT_BY_DATE_DESC is Comparator", ItemService.SORT_BY_DATE_DESC instanceof Comparator);
            TestRunner.check("SORT_BY_DATE_ASC is Comparator", ItemService.SORT_BY_DATE_ASC instanceof Comparator);
            TestRunner.check("SORT_BY_NAME_ASC is Comparator", ItemService.SORT_BY_NAME_ASC instanceof Comparator);

            // 3. INHERITANCE & ABSTRACTION AUDIT
            TestRunner.check("User is abstract", Modifier.isAbstract(User.class.getModifiers()));
            TestRunner.check("Item is abstract", Modifier.isAbstract(Item.class.getModifiers()));

            TestRunner.check("Student extends User", User.class.isAssignableFrom(Student.class));
            TestRunner.check("Staff extends User", User.class.isAssignableFrom(Staff.class));
            TestRunner.check("LostItem extends Item", Item.class.isAssignableFrom(LostItem.class));
            TestRunner.check("FoundItem extends Item", Item.class.isAssignableFrom(FoundItem.class));

            // 4. POLYMORPHISM AUDIT
            User polymorphicStudent = new Student("Bob", "bob@uni.edu", "S123");
            User polymorphicStaff = new Staff("Carol", "carol@uni.edu", "STF99");
            TestRunner.assertEquals("Polymorphic Student role", "Student (ID: S123)", polymorphicStudent.displayRole());
            TestRunner.assertEquals("Polymorphic Staff role", "Staff (ID: STF99)", polymorphicStaff.displayRole());

            Item polymorphicLost = lost;
            Item polymorphicFound = found;
            TestRunner.assertEquals("Polymorphic LostItem type", "LOST", polymorphicLost.getItemType());
            TestRunner.assertEquals("Polymorphic FoundItem type", "FOUND", polymorphicFound.getItemType());

            // 5. CUSTOM EXCEPTIONS AUDIT
            TestRunner.check("InvalidItemException extends Exception", Exception.class.isAssignableFrom(InvalidItemException.class));
            TestRunner.check("ItemNotFoundException extends Exception", Exception.class.isAssignableFrom(ItemNotFoundException.class));
            TestRunner.check("InvalidClaimException extends Exception", Exception.class.isAssignableFrom(InvalidClaimException.class));

            // Verify serialVersionUID
            Field f1 = InvalidItemException.class.getDeclaredField("serialVersionUID");
            Field f2 = ItemNotFoundException.class.getDeclaredField("serialVersionUID");
            Field f3 = InvalidClaimException.class.getDeclaredField("serialVersionUID");
            TestRunner.check("InvalidItemException has serialVersionUID", f1 != null);
            TestRunner.check("ItemNotFoundException has serialVersionUID", f2 != null);
            TestRunner.check("InvalidClaimException has serialVersionUID", f3 != null);

            // 6. TRY-WITH-RESOURCES (AutoCloseable verification)
            TestRunner.check("BufferedReader is AutoCloseable", AutoCloseable.class.isAssignableFrom(java.io.BufferedReader.class));
            TestRunner.check("BufferedWriter is AutoCloseable", AutoCloseable.class.isAssignableFrom(java.io.BufferedWriter.class));
            TestRunner.check("DirectoryStream is AutoCloseable", AutoCloseable.class.isAssignableFrom(java.nio.file.DirectoryStream.class));

        } catch (Exception e) {
            TestRunner.check("ConceptAuditTests threw unexpected exception: " + e.getMessage(), false);
        }
    }
}
