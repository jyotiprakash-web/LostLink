import exception.InvalidClaimException;
import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.time.LocalDate;
import model.Claim;
import model.ClaimStatus;
import model.FoundItem;
import model.ItemCategory;
import model.ItemStatus;
import model.Staff;
import model.User;
import service.ClaimService;
import service.ItemService;

/**
 * Acceptance tests for F16: Claim review.
 */
public class ClaimReviewTests {

    public static void run() {
        System.out.println("--- Running F16 Claim Review Tests ---");

        ItemService itemService = new ItemService();
        ClaimService claimService = new ClaimService(itemService);
        User operator = new Staff("Officer Miller", "miller@campus.edu", "STAFF-09");
        LocalDate today = LocalDate.now();

        try {
            // Setup a found item with two competing claims
            FoundItem wallet = itemService.reportFoundItem("Leather Wallet", ItemCategory.OTHER,
                    "Brown", "Cafeteria", today, "Bifold wallet", operator, "", "ID for Alice");

            Claim claimA = claimService.submitClaim(wallet.getId(), "", "Alice", "alice@campus.edu", "Alice's student ID is inside", today);
            Claim claimB = claimService.submitClaim(wallet.getId(), "", "Bob", "bob@campus.edu", "Has $20 bill", today);

            TestRunner.assertEquals("Wallet status is CLAIM_PENDING with 2 claims", ItemStatus.CLAIM_PENDING, wallet.getStatus());

            // 1. Request more information keeps claim PENDING
            claimService.requestMoreInformation(claimB.getId(), operator.getName(), "Please specify cash denomination");
            TestRunner.assertEquals("Claim B status remains PENDING after info request", ClaimStatus.PENDING, claimB.getStatus());
            TestRunner.assertEquals("Claim B reviewer saved", "Officer Miller", claimB.getReviewerName());
            TestRunner.assertEquals("Claim B notes saved", "Please specify cash denomination", claimB.getReviewNotes());

            // 2. Approve a pending claim
            claimService.approveClaim(claimA.getId(), operator.getName(), "Ownership verified by ID card");
            TestRunner.assertEquals("Claim A status is APPROVED", ClaimStatus.APPROVED, claimA.getStatus());
            TestRunner.assertEquals("Wallet status remains CLAIM_PENDING awaiting handover", ItemStatus.CLAIM_PENDING, wallet.getStatus());

            // 3. Approve a second claim while one is approved throws InvalidClaimException
            TestRunner.assertThrows("Approving second claim while one is approved throws", InvalidClaimException.class, () -> {
                claimService.approveClaim(claimB.getId(), operator.getName(), "Another approval");
            });

            // 4. Reject when another active (APPROVED) claim exists: item stays CLAIM_PENDING
            claimService.rejectClaim(claimB.getId(), operator.getName(), "Wrong description");
            TestRunner.assertEquals("Claim B is REJECTED", ClaimStatus.REJECTED, claimB.getStatus());
            TestRunner.assertEquals("Wallet remains CLAIM_PENDING because Claim A is approved", ItemStatus.CLAIM_PENDING, wallet.getStatus());

            // 5. Approve or reject a non-pending claim throws InvalidClaimException
            TestRunner.assertThrows("Approving an already APPROVED claim throws", InvalidClaimException.class, () -> {
                claimService.approveClaim(claimA.getId(), operator.getName(), "Approve again");
            });
            TestRunner.assertThrows("Rejecting an already REJECTED claim throws", InvalidClaimException.class, () -> {
                claimService.rejectClaim(claimB.getId(), operator.getName(), "Reject again");
            });

            // 6. Rejecting lone pending claim returns item to OPEN
            FoundItem umbrella = itemService.reportFoundItem("Umbrella", ItemCategory.ACCESSORIES,
                    "Blue", "Library", today, "Compact umbrella", operator, "", "");
            Claim loneClaim = claimService.submitClaim(umbrella.getId(), "", "Charlie", "charlie@test.edu", "My umbrella", today);
            TestRunner.assertEquals("Umbrella is CLAIM_PENDING after submit", ItemStatus.CLAIM_PENDING, umbrella.getStatus());

            claimService.rejectClaim(loneClaim.getId(), operator.getName(), "Wrong color");
            TestRunner.assertEquals("Umbrella returns to OPEN after sole claim is rejected", ItemStatus.OPEN, umbrella.getStatus());

            // 7. Unknown claim ID throws InvalidClaimException
            TestRunner.assertThrows("Unknown claim ID throws InvalidClaimException", InvalidClaimException.class, () -> {
                claimService.approveClaim("CLM-9999", operator.getName(), "Approve fake");
            });
            TestRunner.assertThrows("Unknown claim ID on reject throws", InvalidClaimException.class, () -> {
                claimService.rejectClaim("CLM-9999", operator.getName(), "Reject fake");
            });

        } catch (InvalidItemException | ItemNotFoundException | InvalidClaimException e) {
            TestRunner.check("Claim review test setup threw: " + e.getMessage(), false);
        }
    }
}
