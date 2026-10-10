import exception.InvalidClaimException;
import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import model.Claim;
import model.ClaimStatus;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.MatchResult;
import model.Staff;
import model.Student;
import service.ClaimService;
import service.FileStorage;
import service.ItemService;
import service.MatchingService;
import util.InputHelper;

/**
 * Unit and integration tests for F28: Opt-in Demo Data Flag (--demo).
 */
public class DemoDataTests {

    public static void run() {
        System.out.println("--- Running F28 Demo Data Tests ---");

        testFlagParsing();
        testNoDemoDataSilentPopulation();
        testDemoDataPopulationInMemory();
        testDemoDataDoesNotOverwriteExisting();
        testDemoDataPersistenceToDisk();
        testDemoDataOperationalWorkflow();
    }

    private static void testFlagParsing() {
        TestRunner.check("isDemo(null) is false", !Main.isDemo(null));
        TestRunner.check("isDemo(empty) is false", !Main.isDemo(new String[0]));
        TestRunner.check("isDemo(--other) is false", !Main.isDemo(new String[]{"--data-dir", "data"}));
        TestRunner.check("isDemo(--demo) is true", Main.isDemo(new String[]{"--demo"}));
        TestRunner.check("isDemo(--DEMO case insensitive) is true", Main.isDemo(new String[]{"--data-dir", "foo", "--DEMO"}));

        System.setProperty("lostlink.demo", "true");
        TestRunner.check("isDemo respects system property", Main.isDemo(new String[0]));
        System.clearProperty("lostlink.demo");
        TestRunner.check("isDemo false after clearing property", !Main.isDemo(new String[0]));
    }

    private static void testNoDemoDataSilentPopulation() {
        ItemService itemService = new ItemService();
        ClaimService claimService = new ClaimService(itemService);
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes);
        InputHelper inputHelper = new InputHelper(new ByteArrayInputStream("9\n".getBytes()), out);

        // Run Main without demoMode
        Main app = new Main(itemService, new MatchingService(), claimService, null, inputHelper, out, "temp_data", false);
        app.run();

        TestRunner.assertEquals("No items added when --demo flag is absent", 0, itemService.getAllItems().size());
        TestRunner.assertEquals("No claims added when --demo flag is absent", 0, claimService.getAllClaims().size());
        TestRunner.check("Output does not contain [Demo Mode]", !outBytes.toString().contains("[Demo Mode]"));
    }

    private static void testDemoDataPopulationInMemory() {
        ItemService itemService = new ItemService();
        ClaimService claimService = new ClaimService(itemService);
        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes);
        InputHelper inputHelper = new InputHelper(new ByteArrayInputStream("9\n".getBytes()), out);

        // Run Main with demoMode = true
        Main app = new Main(itemService, new MatchingService(), claimService, null, inputHelper, out, "temp_data", true);
        app.run();

        List<Item> items = itemService.getAllItems();
        TestRunner.assertEquals("Demo populated exactly 6 items", 6, items.size());
        TestRunner.assertEquals("Demo populated 3 lost items", 3, itemService.getLostItems().size());
        TestRunner.assertEquals("Demo populated 3 found items", 3, itemService.getFoundItems().size());

        List<Claim> claims = claimService.getAllClaims();
        TestRunner.assertEquals("Demo populated exactly 1 claim", 1, claims.size());
        Claim demoClaim = claims.get(0);
        TestRunner.assertEquals("Demo claim ID is CLM-0001", "CLM-0001", demoClaim.getId());
        TestRunner.assertEquals("Demo claim found item is ITM-0002", "ITM-0002", demoClaim.getFoundItemId());
        TestRunner.assertEquals("Demo claim linked lost item is ITM-0001", "ITM-0001", demoClaim.getLinkedLostItemId());
        TestRunner.assertEquals("Demo claim is PENDING", ClaimStatus.PENDING, demoClaim.getStatus());

        String output = outBytes.toString();
        TestRunner.check("Output announces demo mode population", output.contains("[Demo Mode] Populated sample dataset:"));
    }

    private static void testDemoDataDoesNotOverwriteExisting() {
        ItemService itemService = new ItemService();
        ClaimService claimService = new ClaimService(itemService);
        try {
            itemService.reportLostItem("Pre-existing Wallet", ItemCategory.BAGS, "Brown", "Dining Hall",
                    LocalDate.now(), "Leather wallet", new Student("Sam", "sam@uni.edu"), "sam@uni.edu", "");
        } catch (InvalidItemException e) {
            TestRunner.check("Pre-population failed: " + e.getMessage(), false);
        }

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes);
        InputHelper inputHelper = new InputHelper(new ByteArrayInputStream("9\n".getBytes()), out);

        Main app = new Main(itemService, new MatchingService(), claimService, null, inputHelper, out, "temp_data", true);
        app.run();

        TestRunner.assertEquals("Pre-existing items preserved; count is still 1", 1, itemService.getAllItems().size());
        TestRunner.assertEquals("Item is Sam's wallet", "Pre-existing Wallet", itemService.getAllItems().get(0).getName());
        TestRunner.check("Output notes skipping demo population",
                outBytes.toString().contains("skipping demo population to preserve existing data"));
    }

    private static void testDemoDataPersistenceToDisk() {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("lostlink_demo_test_");
            ItemService itemService = new ItemService();
            ClaimService claimService = new ClaimService(itemService);
            FileStorage fileStorage = new FileStorage(tempDir.toString());

            ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(outBytes);
            InputHelper inputHelper = new InputHelper(new ByteArrayInputStream("9\n".getBytes()), out);

            Main app = new Main(itemService, new MatchingService(), claimService, fileStorage, inputHelper, out, tempDir.toString(), true);
            app.run();

            Path itemsPath = tempDir.resolve("items.csv");
            Path claimsPath = tempDir.resolve("claims.csv");
            Path activityPath = tempDir.resolve("activity.log");

            TestRunner.check("Demo persisted items.csv", Files.exists(itemsPath));
            TestRunner.check("Demo persisted claims.csv", Files.exists(claimsPath));
            TestRunner.check("Demo persisted activity.log", Files.exists(activityPath));

            List<String> itemLines = Files.readAllLines(itemsPath);
            TestRunner.assertEquals("items.csv contains header + 6 records", 7, itemLines.size());

            List<String> claimLines = Files.readAllLines(claimsPath);
            TestRunner.assertEquals("claims.csv contains header + 1 record", 2, claimLines.size());

            String logContent = new String(Files.readAllBytes(activityPath));
            TestRunner.check("activity.log records DEMO_DATA_POPULATED", logContent.contains("DEMO_DATA_POPULATED"));

        } catch (IOException e) {
            TestRunner.check("Demo persistence test threw IOException: " + e.getMessage(), false);
        } finally {
            if (tempDir != null) {
                deleteDirRecursive(tempDir);
            }
        }
    }

    private static void testDemoDataOperationalWorkflow() {
        ItemService itemService = new ItemService();
        MatchingService matchingService = new MatchingService();
        ClaimService claimService = new ClaimService(itemService);

        ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(outBytes);
        Main app = new Main(itemService, matchingService, claimService, null, new InputHelper(), out, "temp_data", true);
        app.populateDemoData();

        // 1. Search for "MacBook"
        List<Item> searchResults = itemService.search("MacBook", null, null, null, null);
        TestRunner.assertEquals("Search for 'MacBook' returns 2 items", 2, searchResults.size());

        // 2. Find matches for ITM-0001 (Lost MacBook) against found items
        try {
            Item lostItem = itemService.getById("ITM-0001");
            TestRunner.check("ITM-0001 is LostItem", lostItem instanceof LostItem);
            MatchResult[] matches = matchingService.findTopMatches((LostItem) lostItem, itemService.getFoundItems());
            TestRunner.check("Matches found for MacBook", matches.length > 0);
            TestRunner.assertEquals("Top match is ITM-0002", "ITM-0002", matches[0].getFoundItem().getId());
            TestRunner.check("Top match score >= 70", matches[0].getScore() >= 70);

            // 3. Claim review & completion flow
            Claim claim = claimService.getClaimById("CLM-0001");
            TestRunner.assertEquals("Initial claim is PENDING", ClaimStatus.PENDING, claim.getStatus());

            claimService.approveClaim("CLM-0001", "Officer Dan", "Staff approved based on serial number & dragon sticker");
            TestRunner.assertEquals("Claim is now APPROVED", ClaimStatus.APPROVED, claim.getStatus());

            claimService.completeHandover("CLM-0001", "Officer Dan", "Physical verification completed at security office");
            TestRunner.assertEquals("Claim is now COMPLETED", ClaimStatus.COMPLETED, claim.getStatus());
            TestRunner.assertEquals("Lost item ITM-0001 is now RETURNED", ItemStatus.RETURNED, lostItem.getStatus());
            TestRunner.assertEquals("Found item ITM-0002 is now RETURNED", ItemStatus.RETURNED, itemService.getById("ITM-0002").getStatus());

        } catch (ItemNotFoundException | InvalidClaimException e) {
            TestRunner.check("Operational workflow failed: " + e.getMessage(), false);
        }
    }

    private static void deleteDirRecursive(Path path) {
        try {
            if (Files.exists(path)) {
                try (var stream = Files.walk(path)) {
                    stream.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.delete(p);
                                } catch (IOException ignored) {
                                }
                            });
                }
            }
        } catch (IOException ignored) {
        }
    }
}
