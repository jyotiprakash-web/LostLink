import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import model.Claim;
import model.ClaimStatus;
import model.FoundItem;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.Student;
import service.ClaimService;
import service.FileStorage;
import service.ItemService;
import service.MatchingService;
import util.InputHelper;

public class FileStorageSaveTests {

    public static void run() {
        System.out.println("--- Running F23 FileStorage Save Tests ---");
        try {
            Path testDir = Files.createTempDirectory("lostlink_f23_test_");

            // 1. Atomic saving of items (saveItems) and temporary file cleanup
            {
                Path subDir = testDir.resolve("save_items");
                FileStorage storage = new FileStorage(subDir.toString());
                ItemService itemService = new ItemService();

                Student user = new Student("Alice", "alice@uni.edu", "S100");
                LostItem lost = itemService.reportLostItem("AirPods Pro", ItemCategory.ELECTRONICS, "White",
                        "Library", LocalDate.of(2026, 3, 10), "In charging case", user, "alice@uni.edu", "Floor 1");
                FoundItem found = itemService.reportFoundItem("Watch", ItemCategory.ACCESSORIES, "Black",
                        "Gym", LocalDate.of(2026, 3, 11), "Digital watch", user, "alice@uni.edu", "Engraved initials 'AW'");

                storage.saveItems(itemService);

                TestRunner.check("items.csv was created", Files.exists(storage.getItemsCsvPath()));
                List<String> lines = Files.readAllLines(storage.getItemsCsvPath(), StandardCharsets.UTF_8);
                TestRunner.assertEquals("items.csv has header + 2 items", 3, lines.size());
                TestRunner.assertEquals("Line 0 is header", FileStorage.ITEMS_HEADER, lines.get(0));
                TestRunner.check("Line 1 contains AirPods", lines.get(1).contains("AirPods Pro"));
                TestRunner.check("Line 2 contains Watch and private details", lines.get(2).contains("Engraved initials 'AW'"));

                // Verify no leftover .tmp files
                try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(subDir, "*.tmp")) {
                    TestRunner.check("No leftover temp files after saveItems", !stream.iterator().hasNext());
                }
            }

            // 2. Atomic saving of claims (saveClaims)
            {
                Path subDir = testDir.resolve("save_claims");
                FileStorage storage = new FileStorage(subDir.toString());
                ItemService itemService = new ItemService();
                ClaimService claimService = new ClaimService(itemService);

                Student user = new Student("Bob", "bob@uni.edu", "S200");
                FoundItem found = itemService.reportFoundItem("Wallet", ItemCategory.BAGS, "Brown",
                        "Hall", LocalDate.of(2026, 3, 12), "Leather wallet", user, "bob@uni.edu", "Contains credit card");

                Claim claim = claimService.submitClaim(found.getId(), "", "Charlie", "charlie@uni.edu",
                        "Chase Visa card inside", LocalDate.of(2026, 3, 13));

                storage.saveClaims(claimService);

                TestRunner.check("claims.csv was created", Files.exists(storage.getClaimsCsvPath()));
                List<String> lines = Files.readAllLines(storage.getClaimsCsvPath(), StandardCharsets.UTF_8);
                TestRunner.assertEquals("claims.csv has header + 1 claim", 2, lines.size());
                TestRunner.assertEquals("Line 0 is header", FileStorage.CLAIMS_HEADER, lines.get(0));
                TestRunner.check("Line 1 contains Charlie", lines.get(1).contains("Charlie"));
                TestRunner.check("Line 1 contains Chase Visa card", lines.get(1).contains("Chase Visa card inside"));

                // Verify no leftover .tmp files
                try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(subDir, "*.tmp")) {
                    TestRunner.check("No leftover temp files after saveClaims", !stream.iterator().hasNext());
                }
            }

            // 3. Appending items without rewriting whole file
            {
                Path subDir = testDir.resolve("append_items");
                FileStorage storage = new FileStorage(subDir.toString());
                ItemService itemService = new ItemService();

                Student user = new Student("Dan", "dan@uni.edu", "S300");
                LostItem item1 = itemService.reportLostItem("Calculator", ItemCategory.ELECTRONICS, "Black",
                        "Science Hall", LocalDate.of(2026, 3, 14), "TI-84", user, "dan@uni.edu", "Room 202");
                storage.appendItem(item1);

                List<String> linesAfter1 = Files.readAllLines(storage.getItemsCsvPath(), StandardCharsets.UTF_8);
                TestRunner.assertEquals("Header + 1 item after first append", 2, linesAfter1.size());

                LostItem item2 = itemService.reportLostItem("Jacket", ItemCategory.CLOTHING, "Blue",
                        "Cafeteria", LocalDate.of(2026, 3, 14), "Denim jacket", user, "dan@uni.edu", "Back chair");
                storage.appendItem(item2);

                List<String> linesAfter2 = Files.readAllLines(storage.getItemsCsvPath(), StandardCharsets.UTF_8);
                TestRunner.assertEquals("Header + 2 items after second append", 3, linesAfter2.size());
                TestRunner.assertEquals("Header unchanged", FileStorage.ITEMS_HEADER, linesAfter2.get(0));
                TestRunner.check("Item 1 present", linesAfter2.get(1).contains("Calculator"));
                TestRunner.check("Item 2 present", linesAfter2.get(2).contains("Jacket"));
            }

            // 4. End-to-end console wiring: reporting item, exiting, and verifying persistence
            {
                Path subDir = testDir.resolve("e2e_console_save");
                FileStorage storage = new FileStorage(subDir.toString());
                ItemService itemService = new ItemService();
                ClaimService claimService = new ClaimService(itemService);

                // Run 1: Report lost item through console and exit (menu 1, then menu 9)
                String scriptedInputRun1 =
                        "1\n" +             // Option 1: Report lost item
                        "HydroFlask\n" +    // Item name
                        "7\n" +             // Category: Other (choice 7)
                        "Green\n" +         // Color
                        "Gym\n" +           // Last-seen location
                        "Locker room\n" +   // Extra details
                        "2026-03-15\n" +    // Date
                        "32oz bottle\n" +   // Description
                        "1\n" +             // Role: Student (choice 1)
                        "Eve\n" +           // Reporter name
                        "eve@uni.edu\n" +   // Contact
                        "S400\n" +          // Student ID
                        "9\n";              // Option 9: Save and exit

                BufferedReader reader1 = new BufferedReader(new StringReader(scriptedInputRun1));
                ByteArrayOutputStream outCapture1 = new ByteArrayOutputStream();
                PrintStream capturePs1 = new PrintStream(outCapture1);

                InputHelper inputHelper1 = new InputHelper(reader1, capturePs1);
                Main app1 = new Main(itemService, new MatchingService(), claimService, storage, inputHelper1, capturePs1, subDir.toString());
                app1.run();

                TestRunner.check("items.csv was created by console report & exit", Files.exists(storage.getItemsCsvPath()));
                List<String> savedLines = Files.readAllLines(storage.getItemsCsvPath(), StandardCharsets.UTF_8);
                TestRunner.check("Saved item contains HydroFlask", savedLines.toString().contains("HydroFlask"));

                // Run 2: Boot fresh Main against same subDir, verify startup loaded HydroFlask
                ItemService itemService2 = new ItemService();
                ClaimService claimService2 = new ClaimService(itemService2);

                String scriptedInputRun2 =
                        "8\n" +             // Option 8: Statistics dashboard
                        "9\n";              // Option 9: Save and exit

                BufferedReader reader2 = new BufferedReader(new StringReader(scriptedInputRun2));
                ByteArrayOutputStream outCapture2 = new ByteArrayOutputStream();
                PrintStream capturePs2 = new PrintStream(outCapture2);

                InputHelper inputHelper2 = new InputHelper(reader2, capturePs2);
                Main app2 = new Main(itemService2, new MatchingService(), claimService2, storage, inputHelper2, capturePs2, subDir.toString());
                app2.run();

                String outputRun2 = outCapture2.toString();
                TestRunner.check("Run 2 loaded item from disk at startup", outputRun2.contains("Loaded 1 item(s)"));
                TestRunner.check("Run 2 statistics shows Total Lost Reports: 1", outputRun2.contains("Total Lost Reports:    1"));
            }

            // Cleanup temp dir
            deleteRecursively(testDir);

        } catch (Exception e) {
            TestRunner.check("FileStorageSaveTests threw unexpected exception: " + e.getMessage(), false);
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
