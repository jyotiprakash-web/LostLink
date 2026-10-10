import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import model.Claim;
import model.ClaimStatus;
import model.FoundItem;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import service.ClaimService;
import service.FileStorage;
import service.ItemService;

public class FileStorageLoadTests {

    public static void run() {
        System.out.println("--- Running F22 FileStorage Load Tests ---");
        try {
            Path testDir = Files.createTempDirectory("lostlink_f22_test_");

            // 1. Missing directory and files handling
            {
                Path nonExistentDir = testDir.resolve("non_existent_subdir");
                FileStorage storage = new FileStorage(nonExistentDir.toString());
                ItemService itemService = new ItemService();
                ClaimService claimService = new ClaimService(itemService);

                int itemsLoaded = storage.loadItems(itemService);
                int claimsLoaded = storage.loadClaims(claimService, itemService);

                TestRunner.assertEquals("Items loaded from missing dir is 0", 0, itemsLoaded);
                TestRunner.assertEquals("Claims loaded from missing dir is 0", 0, claimsLoaded);
                TestRunner.assertEquals("ItemService is empty", 0, itemService.getAllItems().size());
            }

            // 2. Normal valid items and claims loading
            {
                Path validDir = testDir.resolve("valid_data");
                Files.createDirectories(validDir);
                FileStorage storage = new FileStorage(validDir.toString());

                String itemsCsv =
                        FileStorage.ITEMS_HEADER + "\n" +
                        "ITM-0001,LOST,MacBook Pro,ELECTRONICS,Silver,Library,2026-03-01,14-inch M2,OPEN,Student,Alice,alice@uni.edu,Quiet study area\n" +
                        "ITM-0002,FOUND,Blue Umbrella,ACCESSORIES,Blue,Dining Hall,2026-03-02,Foldable umbrella,CLAIM_PENDING,Staff,Officer Dave,dave@uni.edu,Wooden handle with BB carving\n";
                Files.write(storage.getItemsCsvPath(), itemsCsv.getBytes(StandardCharsets.UTF_8));

                String claimsCsv =
                        FileStorage.CLAIMS_HEADER + "\n" +
                        "CLM-0001,ITM-0002,ITM-0001,Alice,alice@uni.edu,It has BB carved on handle,2026-03-03,PENDING,Staff Officer,Under review\n";
                Files.write(storage.getClaimsCsvPath(), claimsCsv.getBytes(StandardCharsets.UTF_8));

                ItemService itemService = new ItemService();
                ClaimService claimService = new ClaimService(itemService);

                int itemsLoaded = storage.loadItems(itemService);
                int claimsLoaded = storage.loadClaims(claimService, itemService);

                TestRunner.assertEquals("Loaded 2 items", 2, itemsLoaded);
                TestRunner.assertEquals("Loaded 1 claim", 1, claimsLoaded);

                Item item1 = itemService.getById("ITM-0001");
                TestRunner.check("Item 1 is LostItem", item1 instanceof LostItem);
                TestRunner.assertEquals("Item 1 name", "MacBook Pro", item1.getName());
                TestRunner.assertEquals("Item 1 category", ItemCategory.ELECTRONICS, item1.getCategory());
                TestRunner.assertEquals("Item 1 last seen", "Quiet study area", ((LostItem) item1).getLastSeenLocationDetails());

                Item item2 = itemService.getById("ITM-0002");
                TestRunner.check("Item 2 is FoundItem", item2 instanceof FoundItem);
                TestRunner.assertEquals("Item 2 status", ItemStatus.CLAIM_PENDING, item2.getStatus());
                TestRunner.assertEquals("Item 2 private details preserved", "Wooden handle with BB carving", ((FoundItem) item2).getPrivateDetails());
                TestRunner.check("Item 2 public summary hides private details", !item2.getPublicSummary().contains("Wooden handle"));

                Claim claim1 = claimService.getClaimById("CLM-0001");
                TestRunner.assertEquals("Claim 1 ID", "CLM-0001", claim1.getId());
                TestRunner.assertEquals("Claim 1 found item", "ITM-0002", claim1.getFoundItemId());
                TestRunner.assertEquals("Claim 1 linked lost item", "ITM-0001", claim1.getLinkedLostItemId());
                TestRunner.assertEquals("Claim 1 status", ClaimStatus.PENDING, claim1.getStatus());

                // Sequence synchronizer check: next IDs must be ITM-0003 and CLM-0002
                TestRunner.assertEquals("Next item sequence synchronized", "ITM-0003", itemService.generateNextId());
                TestRunner.assertEquals("Next claim sequence synchronized", "CLM-0002", claimService.generateNextClaimId());
            }

            // 3. Resilient handling of malformed records & writing to *.rejected
            {
                Path corruptedDir = testDir.resolve("corrupted_data");
                Files.createDirectories(corruptedDir);
                FileStorage storage = new FileStorage(corruptedDir.toString());

                String itemsWithCorruptRows =
                        FileStorage.ITEMS_HEADER + "\n" +
                        "ITM-0010,LOST,Valid Item,BOOKS,Red,Room 101,2026-03-05,Math Book,OPEN,Student,Charlie,charlie@uni.edu,Desk 5\n" +
                        "MALFORMED_TOO_FEW_FIELDS,LOST,Only,Three\n" +
                        "ITM-0011,LOST,Bad Date,OTHER,Black,Hall,2026-99-99,Bad Date Item,OPEN,Student,Dan,dan@uni.edu,\n" +
                        "ITM-0012,LOST,Bad Category,WEAPONS,Black,Hall,2026-03-05,Description,OPEN,Student,Dan,dan@uni.edu,\n" +
                        "ITM-0010,FOUND,Duplicate ID,OTHER,Blue,Hall,2026-03-05,Duplicate,OPEN,Student,Eve,eve@uni.edu,\n" +
                        "ITM-0013,FOUND,Another Valid Item,OTHER,White,Desk,2026-03-06,White Scarf,OPEN,Student,Frank,frank@uni.edu,Has initials\n";
                Files.write(storage.getItemsCsvPath(), itemsWithCorruptRows.getBytes(StandardCharsets.UTF_8));

                String claimsWithCorruptRows =
                        FileStorage.CLAIMS_HEADER + "\n" +
                        "CLM-0010,ITM-0013,,Frank,frank@uni.edu,White Scarf,2026-03-07,PENDING,,\n" +
                        "CLM-0011,ITM-NON_EXISTENT,,Ghost,ghost@uni.edu,No such item,2026-03-07,PENDING,,\n" +
                        "TOO_FEW,FIELDS\n";
                Files.write(storage.getClaimsCsvPath(), claimsWithCorruptRows.getBytes(StandardCharsets.UTF_8));

                ItemService itemService = new ItemService();
                ClaimService claimService = new ClaimService(itemService);

                int itemsLoaded = storage.loadItems(itemService);
                int claimsLoaded = storage.loadClaims(claimService, itemService);

                TestRunner.assertEquals("Loaded 2 valid items out of 6 rows", 2, itemsLoaded);
                TestRunner.assertEquals("Loaded 1 valid claim out of 3 rows", 1, claimsLoaded);

                TestRunner.check("Items rejected file was created", Files.exists(storage.getItemsRejectedPath()));
                TestRunner.check("Claims rejected file was created", Files.exists(storage.getClaimsRejectedPath()));

                List<String> itemsRejected = Files.readAllLines(storage.getItemsRejectedPath(), StandardCharsets.UTF_8);
                TestRunner.assertEquals("4 corrupted item rows recorded in items.rejected", 4, itemsRejected.size());
                TestRunner.check("items.rejected has too few fields error", itemsRejected.get(0).contains("Expected at least 12 fields"));
                TestRunner.check("items.rejected has duplicate ID error", itemsRejected.get(3).contains("Duplicate item ID"));

                List<String> claimsRejected = Files.readAllLines(storage.getClaimsRejectedPath(), StandardCharsets.UTF_8);
                TestRunner.assertEquals("2 corrupted claim rows recorded in claims.rejected", 2, claimsRejected.size());
                TestRunner.check("claims.rejected has non-existent item error", claimsRejected.get(0).contains("Item not found with ID: ITM-NON_EXISTENT"));
            }

            // Clean up temp dir
            deleteRecursively(testDir);

        } catch (Exception e) {
            TestRunner.check("FileStorageLoadTests threw unexpected exception: " + e.getMessage(), false);
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path child : stream) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
