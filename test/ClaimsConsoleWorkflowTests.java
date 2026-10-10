import exception.ItemNotFoundException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import model.Claim;
import model.ClaimStatus;
import model.Item;
import model.ItemStatus;
import service.ClaimService;
import service.ItemService;
import service.MatchingService;
import util.InputHelper;

/**
 * Acceptance tests for F18: Claims console workflow (menu 6) and match hand-off.
 */
public class ClaimsConsoleWorkflowTests {

    public static void run() {
        System.out.println("--- Running F18 Claims Console Workflow Tests ---");

        // Scripted complete end-to-end scenario through console:
        // 1: Report lost item (menu 1)
        //    Name: MacBook
        //    Cat: 1 (ELECTRONICS)
        //    Color: Silver
        //    Loc: Library
        //    Details: desk 4
        //    Date: 2026-03-01
        //    Desc: Laptop
        //    Role: 1 (Student)
        //    Reporter: Alice
        //    Contact: alice@test.edu
        //    ID: S101
        // 2: Report found item (menu 2)
        //    Name: MacBook
        //    Cat: 1 (ELECTRONICS)
        //    Color: Silver
        //    Loc: Library
        //    Date: 2026-03-01
        //    Desc: Laptop
        //    Role: 2 (Staff)
        //    Reporter: Officer Bob
        //    Contact: bob@test.edu
        //    ID: ST01
        //    Private details: confidential note
        // 3: Find potential matches (menu 3)
        //    Pick 1 (lost item ITM-0001)
        //    Claim hand-off prompt: y
        //    (Only 1 candidate, so directly triggers submit claim)
        //    Claimant: Alice
        //    Contact: alice@test.edu
        //    Details: Proof of purchase with matching serial
        // 6: Manage claims (menu 6)
        //    2 (View all claims)
        //    3 (Request more info on CLM-0001)
        //       Claim ID: CLM-0001
        //       Staff: Officer Bob
        //       ID: ST01
        //       Notes: Bring photo ID to room 102
        //    4 (Approve CLM-0001)
        //       Claim ID: CLM-0001
        //       Staff: Officer Bob
        //       ID: ST01
        //       Notes: Photo ID verified in person
        //    4 (Illegal attempt: Approve already approved claim)
        //       Claim ID: CLM-0001
        //       Staff: Officer Bob
        //       ID: ST01
        //       Notes: Duplicate approval attempt
        //    6 (Complete handover)
        //       Claim ID: CLM-0001
        //       Staff: Officer Bob
        //       ID: ST01
        //       Notes: Handed to owner
        //    7 (Return to main menu)
        // 9: Save and exit

        StringBuilder sb = new StringBuilder();
        // Report lost item
        sb.append("1\nMacBook\n1\nSilver\nLibrary\ndesk 4\n2026-03-01\nLaptop\n1\nAlice\nalice@test.edu\nS101\n");
        // Report found item
        sb.append("2\nMacBook\n1\nSilver\nLibrary\n2026-03-01\nLaptop\n2\nOfficer Bob\nbob@test.edu\nST01\nconfidential note\n");
        // Find potential matches with hand-off to claim
        sb.append("3\n1\ny\nAlice\nalice@test.edu\nProof of purchase with matching serial\n");
        // Manage claims submenu
        sb.append("6\n");
        sb.append("2\n"); // view claims
        sb.append("3\nCLM-0001\nOfficer Bob\nST01\nBring photo ID to room 102\n"); // request info
        sb.append("4\nCLM-0001\nOfficer Bob\nST01\nPhoto ID verified in person\n"); // approve
        sb.append("4\nCLM-0001\nOfficer Bob\nST01\nDuplicate approval attempt\n"); // illegal approve
        sb.append("6\nCLM-0001\nOfficer Bob\nST01\nHanded to owner\n"); // complete
        sb.append("7\n"); // return to main menu
        // Exit
        sb.append("9\n");

        BufferedReader reader = new BufferedReader(new StringReader(sb.toString()));
        ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
        PrintStream captureOut = new PrintStream(outputCapture);

        ItemService itemService = new ItemService();
        MatchingService matchingService = new MatchingService();
        ClaimService claimService = new ClaimService(itemService);
        InputHelper inputHelper = new InputHelper(reader, captureOut);

        Main app = new Main(itemService, matchingService, claimService, inputHelper, captureOut, "data");
        app.run();

        String output = outputCapture.toString();

        // Verifications
        TestRunner.check("Console output shows match hand-off claim creation", output.contains("Generated Claim ID: CLM-0001"));
        TestRunner.check("Console output shows reviewer displayRole", output.contains("Operating as: Officer Bob [Staff (ID: ST01)]"));
        TestRunner.check("Console output shows approval success", output.contains("Claim CLM-0001 APPROVED!"));
        TestRunner.check("Console handles illegal transition with friendly error", output.contains("[Error] Only PENDING claims can be approved. Current status: APPROVED"));
        TestRunner.check("Console output shows handover completion", output.contains("Handover complete! Claim is COMPLETED and Item is marked RETURNED."));

        try {
            Item foundItem = itemService.getById("ITM-0002");
            Item lostItem = itemService.getById("ITM-0001");
            Claim claim = claimService.getClaimById("CLM-0001");

            TestRunner.assertEquals("Found item status is RETURNED", ItemStatus.RETURNED, foundItem.getStatus());
            TestRunner.assertEquals("Linked lost item status is RETURNED", ItemStatus.RETURNED, lostItem.getStatus());
            TestRunner.assertEquals("Claim status is COMPLETED", ClaimStatus.COMPLETED, claim.getStatus());
        } catch (Exception e) {
            TestRunner.check("Post-workflow verification failed: " + e.getMessage(), false);
        }
    }
}
