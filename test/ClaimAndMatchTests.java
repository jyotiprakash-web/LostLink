import exception.InvalidClaimException;
import exception.InvalidItemException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Claim;
import model.ClaimStatus;
import model.FoundItem;
import model.ItemCategory;
import model.LostItem;
import model.MatchResult;
import model.Student;
import model.User;

/**
 * Acceptance tests for F5: Claim and MatchResult.
 */
public class ClaimAndMatchTests {

    public static void run() {
        System.out.println("--- Running F5 Claim and MatchResult Tests ---");

        LocalDate today = LocalDate.now();

        // 1. Valid Claim construction
        try {
            Claim claim = new Claim("CLM-0001", "ITM-0002", "ITM-0001", "John Doe", "john@campus.edu",
                    "My laptop has a red sticker on bottom", today);
            TestRunner.assertEquals("Claim ID", "CLM-0001", claim.getId());
            TestRunner.assertEquals("Found item ID", "ITM-0002", claim.getFoundItemId());
            TestRunner.assertEquals("Linked lost item ID", "ITM-0001", claim.getLinkedLostItemId());
            TestRunner.assertEquals("Claimant name", "John Doe", claim.getClaimantName());
            TestRunner.assertEquals("Claimant contact", "john@campus.edu", claim.getClaimantContact());
            TestRunner.assertEquals("Claim status is PENDING by default", ClaimStatus.PENDING, claim.getStatus());
        } catch (InvalidClaimException e) {
            TestRunner.check("Valid claim threw unexpectedly: " + e.getMessage(), false);
        }

        // 2. Claim field validations
        TestRunner.assertThrows("Blank claim ID throws InvalidClaimException", InvalidClaimException.class, () -> {
            new Claim("  ", "ITM-0002", null, "John", "contact", "details", today);
        });
        TestRunner.assertThrows("Blank foundItemId throws InvalidClaimException", InvalidClaimException.class, () -> {
            new Claim("CLM-0002", " ", null, "John", "contact", "details", today);
        });
        TestRunner.assertThrows("Blank claimantName throws InvalidClaimException", InvalidClaimException.class, () -> {
            new Claim("CLM-0002", "ITM-0002", null, "   ", "contact", "details", today);
        });
        TestRunner.assertThrows("Blank claimantContact throws InvalidClaimException", InvalidClaimException.class, () -> {
            new Claim("CLM-0002", "ITM-0002", null, "John", "", "details", today);
        });
        TestRunner.assertThrows("Blank identifyingDetails throws InvalidClaimException", InvalidClaimException.class, () -> {
            new Claim("CLM-0002", "ITM-0002", null, "John", "contact", "   ", today);
        });
        TestRunner.assertThrows("Null claim date throws InvalidClaimException", InvalidClaimException.class, () -> {
            new Claim("CLM-0002", "ITM-0002", null, "John", "contact", "details", null);
        });
        TestRunner.assertThrows("Future claim date throws InvalidClaimException", InvalidClaimException.class, () -> {
            new Claim("CLM-0002", "ITM-0002", null, "John", "contact", "details", today.plusDays(1));
        });

        // 3. MatchResult tests
        try {
            User user = new Student("Bob", "bob@test.edu", "S11");
            LostItem lost = new LostItem("ITM-0001", "Backpack", ItemCategory.BAGS, "Black", "Hallway", today, "Nike backpack", user);
            FoundItem found = new FoundItem("ITM-0002", "Backpack", ItemCategory.BAGS, "Black", "Hallway", today, "Nike bag", user);

            List<String> reasons = new ArrayList<>();
            reasons.add("Category matched (+30)");
            reasons.add("Color matched (+15)");

            MatchResult result = new MatchResult(lost, found, 75, reasons);
            TestRunner.assertEquals("MatchResult score", 75, result.getScore());
            TestRunner.assertEquals("MatchResult reasons size", 2, result.getReasons().size());

            // Defensive copy test 1: mutating the original list does not alter MatchResult
            reasons.add("Location matched (+20)");
            TestRunner.assertEquals("MatchResult reasons defended against external mutation", 2, result.getReasons().size());

            // Defensive copy test 2: trying to mutate getReasons() throws UnsupportedOperationException
            TestRunner.assertThrows("getReasons() is unmodifiable", UnsupportedOperationException.class, () -> {
                result.getReasons().add("Hacked reason (+10)");
            });

            // Score bounds 0-100
            TestRunner.assertThrows("Score below 0 rejected", IllegalArgumentException.class, () -> {
                new MatchResult(lost, found, -1, reasons);
            });
            TestRunner.assertThrows("Score above 100 rejected", IllegalArgumentException.class, () -> {
                new MatchResult(lost, found, 101, reasons);
            });

            // Boundary values 0 and 100 accepted
            MatchResult zeroScore = new MatchResult(lost, found, 0, new ArrayList<>());
            TestRunner.assertEquals("Score 0 allowed", 0, zeroScore.getScore());
            MatchResult maxScore = new MatchResult(lost, found, 100, new ArrayList<>());
            TestRunner.assertEquals("Score 100 allowed", 100, maxScore.getScore());

        } catch (InvalidItemException e) {
            TestRunner.check("Item creation for MatchResult failed: " + e.getMessage(), false);
        }
    }
}
