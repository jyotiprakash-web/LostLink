import exception.InvalidClaimException;
import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.time.LocalDate;
import java.util.Collections;
import model.Claim;
import model.ClaimStatus;
import model.FoundItem;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.MatchResult;
import model.Staff;
import model.User;
import service.ClaimService;
import service.ItemService;
import service.MatchingService;

/**
 * Acceptance tests for F17: Handover completion.
 */
public class HandoverCompletionTests {

    public static void run() {
        System.out.println("--- Running F17 Handover Completion Tests ---");

        ItemService itemService = new ItemService();
        ClaimService claimService = new ClaimService(itemService);
        MatchingService matchingService = new MatchingService();
        User staff = new Staff("Officer Miller", "miller@campus.edu", "STAFF-09");
        LocalDate today = LocalDate.now();

        try {
            // Setup items: found laptop and linked lost report
            FoundItem foundLaptop = itemService.reportFoundItem("MacBook Air", ItemCategory.ELECTRONICS,
                    "Space Gray", "Library Study Room", today, "M2 MacBook Air", staff, "", "Password hint on sticky note");
            LostItem lostLaptop = itemService.reportLostItem("MacBook Air", ItemCategory.ELECTRONICS,
                    "Space Gray", "Library", today, "My M2 laptop", staff, "", "");

            // Two claims: Alice (valid owner) and Bob (competing)
            Claim claimAlice = claimService.submitClaim(foundLaptop.getId(), lostLaptop.getId(),
                    "Alice", "alice@campus.edu", "Matches serial number C02G...", today);
            Claim claimBob = claimService.submitClaim(foundLaptop.getId(), "",
                    "Bob", "bob@campus.edu", "I left it there this morning", today);

            // 1. Completing a PENDING claim throws InvalidClaimException
            TestRunner.assertThrows("Completing PENDING claim throws", InvalidClaimException.class, () -> {
                claimService.completeHandover(claimAlice.getId(), staff.getName(), "Early handover attempt");
            });

            // 2. Approve Alice's claim
            claimService.approveClaim(claimAlice.getId(), staff.getName(), "Serial number verified against box");
            TestRunner.assertEquals("Alice's claim is APPROVED", ClaimStatus.APPROVED, claimAlice.getStatus());

            // 3. Completing a REJECTED claim throws InvalidClaimException
            Claim dummyClaim = claimService.submitClaim(foundLaptop.getId(), "", "Charlie", "charlie@test.edu", "some details", today);
            claimService.rejectClaim(dummyClaim.getId(), staff.getName(), "Not matching");
            TestRunner.assertThrows("Completing REJECTED claim throws", InvalidClaimException.class, () -> {
                claimService.completeHandover(dummyClaim.getId(), staff.getName(), "Handover rejected claim");
            });

            // 4. Complete handover for Alice's approved claim
            claimService.completeHandover(claimAlice.getId(), staff.getName(), "Device handed over in person. ID checked.");
            TestRunner.assertEquals("Alice's claim is COMPLETED", ClaimStatus.COMPLETED, claimAlice.getStatus());
            TestRunner.assertEquals("Found item status is RETURNED", ItemStatus.RETURNED, foundLaptop.getStatus());
            TestRunner.assertEquals("Linked lost item status is RETURNED", ItemStatus.RETURNED, lostLaptop.getStatus());

            // 5. Competing pending claim (Bob) is auto-rejected
            TestRunner.assertEquals("Bob's competing claim is auto-rejected", ClaimStatus.REJECTED, claimBob.getStatus());
            TestRunner.check("Bob's review notes explain auto-rejection", claimBob.getReviewNotes().contains("Auto-rejected"));

            // 6. Completing an already COMPLETED claim throws
            TestRunner.assertThrows("Completing an already COMPLETED claim throws", InvalidClaimException.class, () -> {
                claimService.completeHandover(claimAlice.getId(), staff.getName(), "Repeat handover");
            });

            // 7. A returned item cannot be claimed again
            TestRunner.assertThrows("Returned item cannot receive new claims", InvalidClaimException.class, () -> {
                claimService.submitClaim(foundLaptop.getId(), "", "Late Claimant", "late@test.edu", "details", today);
            });

            // 8. Returned items no longer appear in match results
            LostItem anotherLost = new LostItem("ITM-0099", "MacBook Air", ItemCategory.ELECTRONICS,
                    "Space Gray", "Library", today, "Another lost macbook", staff);
            MatchResult[] matches = matchingService.findTopMatches(anotherLost, Collections.singletonList(foundLaptop));
            TestRunner.assertEquals("Returned found item excluded from matches (0 candidates)", 0, matches.length);

        } catch (InvalidItemException | ItemNotFoundException | InvalidClaimException e) {
            TestRunner.check("Handover completion test setup threw: " + e.getMessage(), false);
        }
    }
}
