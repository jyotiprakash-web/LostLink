import exception.InvalidClaimException;
import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.LocalDate;
import model.FoundItem;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.Student;
import model.SystemStatistics;
import service.ClaimService;
import service.ItemService;
import service.MatchingService;
import util.InputHelper;

public class StatisticsTests {
    public static void run() {
        System.out.println("--- Running F20 Statistics Dashboard Tests ---");
        try {

        // 1. Initial empty system statistics
        {
            ItemService itemService = new ItemService();
            ClaimService claimService = new ClaimService(itemService);

            SystemStatistics stats = itemService.getStatistics(claimService);
            TestRunner.check("Empty total lost is 0", stats.getTotalLost() == 0);
            TestRunner.check("Empty total found is 0", stats.getTotalFound() == 0);
            TestRunner.check("Empty total items is 0", stats.getTotalItems() == 0);
            TestRunner.check("Empty unresolved items is 0", stats.getUnresolvedItems() == 0);
            TestRunner.check("Empty pending claims is 0", stats.getPendingClaims() == 0);
            TestRunner.check("Empty returned items is 0", stats.getReturnedItems() == 0);
            TestRunner.check("Empty recovery rate is 0.0", stats.getRecoveryRate() == 0.0);
            TestRunner.check("Formatted recovery rate on empty is safe N/A", stats.getFormattedRecoveryRate().contains("N/A"));
        }

        // 2. Statistics tracking items across lifecycle
        {
            ItemService itemService = new ItemService();
            ClaimService claimService = new ClaimService(itemService);
            Student user = new Student("Bob", "bob@uni.edu", "S123");

            // Add 2 lost items
            LostItem lost1 = itemService.reportLostItem("Blue Backpack", ItemCategory.BAGS, "Blue",
                    "Library", LocalDate.now(), "Bookbag", user, "bob@uni.edu", "Floor 2");
            LostItem lost2 = itemService.reportLostItem("Calculus Book", ItemCategory.BOOKS, "Green",
                    "Hall", LocalDate.now(), "Math", user, "bob@uni.edu", "Locker");

            // Add 2 found items
            FoundItem found1 = itemService.reportFoundItem("Backpack", ItemCategory.BAGS, "Blue",
                    "Library", LocalDate.now(), "Bag found", user, "bob@uni.edu", "Has ID badge");
            FoundItem found2 = itemService.reportFoundItem("Umbrella", ItemCategory.ACCESSORIES, "Black",
                    "Cafeteria", LocalDate.now(), "Rain umbrella", user, "bob@uni.edu", "Wooden handle");

            SystemStatistics stats1 = itemService.getStatistics(claimService);
            TestRunner.check("Total lost is 2", stats1.getTotalLost() == 2);
            TestRunner.check("Total found is 2", stats1.getTotalFound() == 2);
            TestRunner.check("Total items is 4", stats1.getTotalItems() == 4);
            TestRunner.check("All 4 are unresolved (OPEN)", stats1.getUnresolvedItems() == 4);
            TestRunner.check("Pending claims is 0", stats1.getPendingClaims() == 0);
            TestRunner.check("Returned is 0", stats1.getReturnedItems() == 0);
            TestRunner.check("Initial recovery rate is 0.0%", Math.abs(stats1.getRecoveryRate() - 0.0) < 0.001);

            // Submit claim for found1 linked to lost1
            model.Claim claim = claimService.submitClaim(found1.getId(), lost1.getId(), "Bob", "bob@uni.edu", "Student ID inside", LocalDate.now());
            String claimId = claim.getId();
            SystemStatistics stats2 = itemService.getStatistics(claimService);
            TestRunner.check("Pending claims is 1", stats2.getPendingClaims() == 1);
            TestRunner.check("Unresolved items remains 4 (1 CLAIM_PENDING, 3 OPEN)", stats2.getUnresolvedItems() == 4);

            // Approve claim and complete handover
            claimService.approveClaim(claimId, "Staff Sarah", "Verified ID");
            claimService.completeHandover(claimId, "Staff Sarah", "Handed over to Bob");

            SystemStatistics stats3 = itemService.getStatistics(claimService);
            TestRunner.check("Pending claims is now 0 after handover", stats3.getPendingClaims() == 0);
            TestRunner.check("Returned items is 2 (1 lost + 1 found)", stats3.getReturnedItems() == 2);
            TestRunner.check("Returned lost is 1", stats3.getReturnedLost() == 1);
            TestRunner.check("Returned found is 1", stats3.getReturnedFound() == 1);
            TestRunner.check("Unresolved items reduced to 2", stats3.getUnresolvedItems() == 2);

            // Recovery rate: 1 returned found out of 2 total found = 50.0%
            TestRunner.check("Recovery rate is 50.0%", Math.abs(stats3.getRecoveryRate() - 50.0) < 0.001);
            TestRunner.check("Formatted recovery rate contains 50.0%", stats3.getFormattedRecoveryRate().contains("50.0%"));

            // Helper query methods on ItemService match
            TestRunner.check("ItemService.getTotalLostCount() == 2", itemService.getTotalLostCount() == 2);
            TestRunner.check("ItemService.getTotalFoundCount() == 2", itemService.getTotalFoundCount() == 2);
            TestRunner.check("ItemService.getUnresolvedCount() == 2", itemService.getUnresolvedCount() == 2);
            TestRunner.check("ItemService.getReturnedCount() == 2", itemService.getReturnedCount() == 2);
            TestRunner.check("ItemService.getReturnedFoundCount() == 1", itemService.getReturnedFoundCount() == 1);
            TestRunner.check("ItemService.getReturnedLostCount() == 1", itemService.getReturnedLostCount() == 1);
            TestRunner.check("ItemService.getRecoveryRate() == 50.0", Math.abs(itemService.getRecoveryRate() - 50.0) < 0.001);
        }

        // 3. Null ClaimService safety
        {
            ItemService itemService = new ItemService();
            SystemStatistics stats = itemService.getStatistics(null);
            TestRunner.check("Null ClaimService returns 0 pending claims", stats.getPendingClaims() == 0);
            TestRunner.check("Null ClaimService returns 0 total claims", stats.getTotalClaims() == 0);
        }

        // 4. Console Dashboard workflow (menu option 8)
        {
            ItemService itemService = new ItemService();
            ClaimService claimService = new ClaimService(itemService);
            Student user = new Student("Alice", "alice@uni.edu", "S999");
            itemService.reportLostItem("Keys", ItemCategory.OTHER, "Silver", "Gym", LocalDate.now(),
                    "Brass ring", user, "alice@uni.edu", "Locker 12");
            itemService.reportFoundItem("Keys", ItemCategory.OTHER, "Silver", "Gym", LocalDate.now(),
                    "Keys found", user, "alice@uni.edu", "Brass ring with 3 keys");

            String scripted = "8\n9\n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            ByteArrayOutputStream outCapture = new ByteArrayOutputStream();
            PrintStream capturePs = new PrintStream(outCapture);

            InputHelper inputHelper = new InputHelper(reader, capturePs);
            Main app = new Main(itemService, new MatchingService(), claimService, inputHelper, capturePs, "data");
            app.run();

            String output = outCapture.toString();
            TestRunner.check("Dashboard header printed", output.contains("STATISTICS DASHBOARD"));
            TestRunner.check("Dashboard shows Total Lost Reports", output.contains("Total Lost Reports:    1"));
            TestRunner.check("Dashboard shows Total Found Reports", output.contains("Total Found Reports:   1"));
            TestRunner.check("Dashboard shows Total Reports: 2", output.contains("Total Reports:         2"));
            TestRunner.check("Dashboard shows Unresolved Items: 2", output.contains("Unresolved Items:      2"));
            TestRunner.check("Dashboard shows Pending Claims: 0", output.contains("Pending Claims:        0"));
            TestRunner.check("Dashboard shows Recovery Rate", output.contains("Recovery Rate:"));
        }
        } catch (Exception e) {
            TestRunner.check("Statistics tests threw unexpected exception: " + e.getMessage(), false);
        }
    }
}
