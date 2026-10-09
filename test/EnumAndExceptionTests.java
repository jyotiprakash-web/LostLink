import exception.InvalidClaimException;
import exception.InvalidItemException;
import exception.ItemNotFoundException;
import model.ClaimStatus;
import model.ItemCategory;
import model.ItemStatus;

/**
 * Acceptance tests for F2: Enums and Custom Exceptions.
 */
public class EnumAndExceptionTests {

    public static void run() {
        System.out.println("--- Running F2 Enum and Exception Tests ---");

        // 1. InvalidItemException tests
        TestRunner.assertThrows("InvalidItemException can be thrown and caught", InvalidItemException.class, () -> {
            throw new InvalidItemException("Invalid item test");
        });
        try {
            Throwable cause = new RuntimeException("Root cause");
            throw new InvalidItemException("Item error", cause);
        } catch (InvalidItemException e) {
            TestRunner.assertEquals("InvalidItemException message preserved", "Item error", e.getMessage());
            TestRunner.assertEquals("InvalidItemException cause preserved", "Root cause", e.getCause().getMessage());
        }

        // 2. ItemNotFoundException tests
        TestRunner.assertThrows("ItemNotFoundException can be thrown and caught", ItemNotFoundException.class, () -> {
            throw new ItemNotFoundException("ITM-9999 not found");
        });
        try {
            Throwable cause = new IllegalStateException("Missing in map");
            throw new ItemNotFoundException("Not found error", cause);
        } catch (ItemNotFoundException e) {
            TestRunner.assertEquals("ItemNotFoundException message preserved", "Not found error", e.getMessage());
            TestRunner.assertEquals("ItemNotFoundException cause preserved", "Missing in map", e.getCause().getMessage());
        }

        // 3. InvalidClaimException tests
        TestRunner.assertThrows("InvalidClaimException can be thrown and caught", InvalidClaimException.class, () -> {
            throw new InvalidClaimException("Invalid claim state");
        });
        try {
            Throwable cause = new IllegalArgumentException("Illegal state");
            throw new InvalidClaimException("Claim transition error", cause);
        } catch (InvalidClaimException e) {
            TestRunner.assertEquals("InvalidClaimException message preserved", "Claim transition error", e.getMessage());
            TestRunner.assertEquals("InvalidClaimException cause preserved", "Illegal state", e.getCause().getMessage());
        }

        // 4. ItemStatus fromString tests
        TestRunner.assertEquals("ItemStatus fromString exact", ItemStatus.OPEN, ItemStatus.fromString("OPEN"));
        TestRunner.assertEquals("ItemStatus fromString case-insensitive", ItemStatus.CLAIM_PENDING, ItemStatus.fromString("claim_pending"));
        TestRunner.assertEquals("ItemStatus fromString with whitespace", ItemStatus.RETURNED, ItemStatus.fromString("  returned  "));
        TestRunner.assertEquals("ItemStatus fromString CLOSED", ItemStatus.CLOSED, ItemStatus.fromString("closed"));
        TestRunner.assertThrows("ItemStatus rejects unknown value", IllegalArgumentException.class, () -> {
            ItemStatus.fromString("INVALID_STATUS");
        });
        TestRunner.assertThrows("ItemStatus rejects blank string", IllegalArgumentException.class, () -> {
            ItemStatus.fromString("   ");
        });
        TestRunner.assertThrows("ItemStatus rejects null", IllegalArgumentException.class, () -> {
            ItemStatus.fromString(null);
        });

        // 5. ClaimStatus fromString tests
        TestRunner.assertEquals("ClaimStatus fromString exact", ClaimStatus.PENDING, ClaimStatus.fromString("PENDING"));
        TestRunner.assertEquals("ClaimStatus fromString case-insensitive", ClaimStatus.APPROVED, ClaimStatus.fromString("approved"));
        TestRunner.assertEquals("ClaimStatus fromString REJECTED", ClaimStatus.REJECTED, ClaimStatus.fromString("  rejected  "));
        TestRunner.assertEquals("ClaimStatus fromString COMPLETED", ClaimStatus.COMPLETED, ClaimStatus.fromString("completed"));
        TestRunner.assertThrows("ClaimStatus rejects unknown value", IllegalArgumentException.class, () -> {
            ClaimStatus.fromString("UNKNOWN_CLAIM");
        });
        TestRunner.assertThrows("ClaimStatus rejects blank", IllegalArgumentException.class, () -> {
            ClaimStatus.fromString("");
        });

        // 6. ItemCategory fromString and getNames tests
        TestRunner.assertEquals("ItemCategory ELECTRONICS", ItemCategory.ELECTRONICS, ItemCategory.fromString("electronics"));
        TestRunner.assertEquals("ItemCategory BAGS", ItemCategory.BAGS, ItemCategory.fromString(" Bags "));
        TestRunner.assertEquals("ItemCategory IDENTIFICATION", ItemCategory.IDENTIFICATION, ItemCategory.fromString("identification"));
        TestRunner.assertEquals("ItemCategory BOOKS", ItemCategory.BOOKS, ItemCategory.fromString("books"));
        TestRunner.assertEquals("ItemCategory CLOTHING", ItemCategory.CLOTHING, ItemCategory.fromString("clothing"));
        TestRunner.assertEquals("ItemCategory ACCESSORIES", ItemCategory.ACCESSORIES, ItemCategory.fromString("accessories"));
        TestRunner.assertEquals("ItemCategory OTHER", ItemCategory.OTHER, ItemCategory.fromString("other"));
        TestRunner.assertThrows("ItemCategory rejects unknown category", IllegalArgumentException.class, () -> {
            ItemCategory.fromString("VEHICLE");
        });

        String[] categoryNames = ItemCategory.getNames();
        TestRunner.assertEquals("ItemCategory.getNames length equals enum length", ItemCategory.values().length, categoryNames.length);
        TestRunner.assertEquals("ItemCategory.getNames first element", "ELECTRONICS", categoryNames[0]);
    }
}
