import exception.InvalidClaimException;
import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.time.LocalDate;
import model.Claim;
import model.ClaimStatus;
import model.FoundItem;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.Student;
import model.User;
import service.ClaimService;
import service.ItemService;

/**
 * Acceptance tests for F15: Claim submission.
 */
public class ClaimSubmissionTests {

    public static void run() {
        System.out.println("--- Running F15 Claim Submission Tests ---");

        ItemService itemService = new ItemService();
        ClaimService claimService = new ClaimService(itemService);
        User user = new Student("Bob", "bob@campus.edu", "S101");
        LocalDate today = LocalDate.now();

        try {
            FoundItem foundItem = itemService.reportFoundItem("Calculator", ItemCategory.ELECTRONICS,
                    "Black", "Math Hall", today, "TI-84 Plus", user, "", "Engraved with initials B.J.");
            LostItem lostItem = itemService.reportLostItem("Calculator", ItemCategory.ELECTRONICS,
                    "Black", "Math Hall", today, "My TI-84 Plus", user, "", "");

            // 1. Valid submit: found item moves from OPEN to CLAIM_PENDING
            TestRunner.assertEquals("Initial found item status is OPEN", ItemStatus.OPEN, foundItem.getStatus());
            Claim claim1 = claimService.submitClaim(foundItem.getId(), lostItem.getId(),
                    "Charlie", "charlie@campus.edu", "Initials on back are B.J.", today);

            TestRunner.assertEquals("Generated claim ID is CLM-0001", "CLM-0001", claim1.getId());
            TestRunner.assertEquals("Claim status is PENDING", ClaimStatus.PENDING, claim1.getStatus());
            TestRunner.assertEquals("Found item transitioned to CLAIM_PENDING", ItemStatus.CLAIM_PENDING, foundItem.getStatus());
            TestRunner.assertEquals("Claim count is 1", 1, claimService.getClaimCount());

            Claim retrieved = claimService.getClaimById("CLM-0001");
            TestRunner.assertEquals("Retrieved claim matches", claim1, retrieved);

            // 2. Submit second competing claim while item is CLAIM_PENDING
            Claim claim2 = claimService.submitClaim(foundItem.getId(), "",
                    "Dave", "dave@campus.edu", "Lost black calculator yesterday", today);
            TestRunner.assertEquals("Second claim ID is CLM-0002", "CLM-0002", claim2.getId());
            TestRunner.assertEquals("Found item remains CLAIM_PENDING", ItemStatus.CLAIM_PENDING, foundItem.getStatus());
            TestRunner.assertEquals("Claims for item is 2", 2, claimService.getClaimsForFoundItem(foundItem.getId()).size());

            // 3. Unknown found item throws ItemNotFoundException
            TestRunner.assertThrows("Unknown item ID throws ItemNotFoundException", ItemNotFoundException.class, () -> {
                claimService.submitClaim("ITM-9999", "", "Eve", "eve@campus.edu", "details", today);
            });

            // 4. Claim against Lost item throws InvalidClaimException
            TestRunner.assertThrows("Claim against lost item throws InvalidClaimException", InvalidClaimException.class, () -> {
                claimService.submitClaim(lostItem.getId(), "", "Frank", "frank@campus.edu", "details", today);
            });

            // 5. Returned found item throws InvalidClaimException
            foundItem.setStatus(ItemStatus.RETURNED);
            TestRunner.assertThrows("Returned item cannot receive new claims", InvalidClaimException.class, () -> {
                claimService.submitClaim(foundItem.getId(), "", "Grace", "grace@campus.edu", "details", today);
            });

            // 6. Closed found item throws InvalidClaimException
            foundItem.setStatus(ItemStatus.CLOSED);
            TestRunner.assertThrows("Closed item cannot receive new claims", InvalidClaimException.class, () -> {
                claimService.submitClaim(foundItem.getId(), "", "Heidi", "heidi@campus.edu", "details", today);
            });
            foundItem.setStatus(ItemStatus.CLAIM_PENDING); // restore

            // 7. Blank claimant info is rejected
            TestRunner.assertThrows("Blank claimant name throws InvalidClaimException", InvalidClaimException.class, () -> {
                claimService.submitClaim(foundItem.getId(), "", "  ", "valid@email.com", "details", today);
            });
            TestRunner.assertThrows("Blank claimant contact throws InvalidClaimException", InvalidClaimException.class, () -> {
                claimService.submitClaim(foundItem.getId(), "", "Ivan", "  ", "details", today);
            });
            TestRunner.assertThrows("Blank identifying details throws InvalidClaimException", InvalidClaimException.class, () -> {
                claimService.submitClaim(foundItem.getId(), "", "Ivan", "ivan@campus.edu", "   ", today);
            });

        } catch (InvalidItemException | ItemNotFoundException | InvalidClaimException e) {
            TestRunner.check("Claim submission tests setup threw: " + e.getMessage(), false);
        }
    }
}
