import exception.InvalidClaimException;
import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import model.FoundItem;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.MatchResult;
import model.Student;
import service.ClaimService;
import service.FileStorage;
import service.ItemService;
import service.MatchingService;
import util.InputHelper;

/**
 * End-to-end integration and edge-case test suite verifying all design.md Section 11
 * requirements, edge cases, and an unbroken "Demo Day" scenario touching all 9 menu options.
 */
public class IntegrationTests {

    public static void run() {
        System.out.println("--- Running F26 Integration & Edge Case Tests ---");
        try {
            // ==============================================================
            // PART 1: SECTION 11 EDGE CASES
            // ==============================================================

            // 1. Empty item list operations
            {
                ItemService emptyItems = new ItemService();
                ClaimService emptyClaims = new ClaimService(emptyItems);
                MatchingService emptyMatching = new MatchingService();

                TestRunner.check("Empty list search returns empty", emptyItems.search("", null, null, null, null).isEmpty());
                TestRunner.check("Empty list browse returns empty", emptyItems.browse(null, null, ItemService.SORT_BY_DATE_DESC).isEmpty());
                TestRunner.assertEquals("Empty list statistics returns 0", 0, emptyItems.getStatistics(emptyClaims).getTotalItems());

                // Unknown item ID lookup
                TestRunner.assertThrows("Unknown item ID throws ItemNotFoundException", ItemNotFoundException.class, () -> {
                    emptyItems.getById("ITM-9999");
                });

                // Unknown claim ID lookup
                TestRunner.assertThrows("Unknown claim ID throws InvalidClaimException", InvalidClaimException.class, () -> {
                    emptyClaims.getClaimById("CLM-9999");
                });
            }

            // 2. Illegal claim transitions rejected
            {
                ItemService itemService = new ItemService();
                ClaimService claimService = new ClaimService(itemService);
                Student user = new Student("Alice", "alice@uni.edu", "S101");

                LostItem lost = itemService.reportLostItem("Phone", ItemCategory.ELECTRONICS, "Black",
                        "Library", LocalDate.now(), "Pixel phone", user, "alice@uni.edu", "");
                FoundItem found = itemService.reportFoundItem("Phone", ItemCategory.ELECTRONICS, "Black",
                        "Library", LocalDate.now(), "Pixel found", user, "alice@uni.edu", "PIN 1234");

                // Cannot claim a lost item
                TestRunner.assertThrows("Claim against lost item throws InvalidClaimException", InvalidClaimException.class, () -> {
                    claimService.submitClaim(lost.getId(), "", "Bob", "bob@uni.edu", "Proof", LocalDate.now());
                });

                // Submit valid claim
                model.Claim claim = claimService.submitClaim(found.getId(), lost.getId(), "Bob", "bob@uni.edu", "Proof", LocalDate.now());

                // Cannot complete PENDING claim directly (must be APPROVED first)
                TestRunner.assertThrows("Completing PENDING claim directly throws", InvalidClaimException.class, () -> {
                    claimService.completeHandover(claim.getId(), "Staff", "Premature handover");
                });

                // Reject claim
                claimService.rejectClaim(claim.getId(), "Staff", "Insufficient proof");

                // Cannot complete REJECTED claim
                TestRunner.assertThrows("Completing REJECTED claim throws", InvalidClaimException.class, () -> {
                    claimService.completeHandover(claim.getId(), "Staff", "Handover after reject");
                });

                // Cannot approve REJECTED claim
                TestRunner.assertThrows("Approving REJECTED claim throws", InvalidClaimException.class, () -> {
                    claimService.approveClaim(claim.getId(), "Staff", "Approve after reject");
                });
            }

            // 3. Deterministic tie-breaking on identical match scores
            {
                MatchingService matchingService = new MatchingService();
                Student reporter = new Student("Test", "test@uni.edu");

                LostItem targetLost = new LostItem("ITM-0100", "Umbrella", ItemCategory.ACCESSORIES,
                        "Black", "Hall", LocalDate.of(2026, 3, 10), "Black umbrella",
                        ItemStatus.OPEN, reporter, "test@uni.edu", "");

                // Three candidate found items with identical attributes & dates
                FoundItem c1 = new FoundItem("ITM-0030", "Umbrella", ItemCategory.ACCESSORIES,
                        "Black", "Hall", LocalDate.of(2026, 3, 10), "Black umbrella",
                        ItemStatus.OPEN, reporter, "test@uni.edu", "P1");
                FoundItem c2 = new FoundItem("ITM-0010", "Umbrella", ItemCategory.ACCESSORIES,
                        "Black", "Hall", LocalDate.of(2026, 3, 10), "Black umbrella",
                        ItemStatus.OPEN, reporter, "test@uni.edu", "P2");
                FoundItem c3 = new FoundItem("ITM-0020", "Umbrella", ItemCategory.ACCESSORIES,
                        "Black", "Hall", LocalDate.of(2026, 3, 10), "Black umbrella",
                        ItemStatus.OPEN, reporter, "test@uni.edu", "P3");

                MatchResult[] matches = matchingService.findTopMatches(targetLost, java.util.Arrays.asList(c1, c2, c3));
                TestRunner.assertEquals("Matches size is 3", 3, matches.length);
                // Tie breaker must sort by ID ascending: ITM-0010, ITM-0020, ITM-0030
                TestRunner.assertEquals("1st tie broken candidate is ITM-0010", "ITM-0010", matches[0].getFoundItem().getId());
                TestRunner.assertEquals("2nd tie broken candidate is ITM-0020", "ITM-0020", matches[1].getFoundItem().getId());
                TestRunner.assertEquals("3rd tie broken candidate is ITM-0030", "ITM-0030", matches[2].getFoundItem().getId());
            }

            // ==============================================================
            // PART 2: COMPREHENSIVE "DEMO DAY" SCENARIO (ALL 9 OPTIONS)
            // ==============================================================
            {
                Path demoDir = Files.createTempDirectory("lostlink_demo_day_");
                FileStorage storage = new FileStorage(demoDir.toString());
                ItemService itemService = new ItemService();
                ClaimService claimService = new ClaimService(itemService);
                MatchingService matchingService = new MatchingService();

                // Single script driving all 9 console menu options in sequence:
                // Option 1: Report Lost Item (MacBook Pro)
                // Option 2: Report Found Item (MacBook Pro with secret sticker)
                // Option 3: Find Potential Matches -> 100/100 -> initiate claim -> submit claim CLM-0001
                // Option 4: Search Records (keyword 'MacBook')
                // Option 5: Browse Items (sort by date descending)
                // Option 6: Manage claims -> approve CLM-0001 -> complete handover
                // Option 7: My Reports -> lookup by reporter name 'Alice'
                // Option 8: Statistics dashboard
                // Option 9: Save data and exit
                String demoScript =
                        // 1. Report lost item
                        "1\n" +
                        "MacBook Pro\n" +               // Item name
                        "1\n" +                         // Category: Electronics
                        "Silver\n" +                     // Color
                        "Library Quiet Floor\n" +       // Location
                        "Table 14 near outlet\n" +      // Extra details
                        "2026-03-01\n" +                // Date
                        "14-inch Space Silver M2\n" +   // Description
                        "1\n" +                         // Role: Student
                        "Alice Smith\n" +               // Name
                        "alice@uni.edu\n" +             // Contact
                        "STU-1001\n" +                  // Student ID

                        // 2. Report found item
                        "2\n" +
                        "MacBook Pro Laptop\n" +        // Item name
                        "1\n" +                         // Category: Electronics
                        "Silver\n" +                     // Color
                        "Library Quiet Floor\n" +       // Found location
                        "2026-03-01\n" +                // Date found
                        "14-inch laptop found on desk\n" + // Description
                        "2\n" +                         // Role: Staff
                        "Officer Bob\n" +               // Name
                        "bob@uni.edu\n" +               // Contact
                        "STF-2002\n" +                  // Staff ID
                        "Secret sticker 'GOLANG' on bottom case\n" + // Private details

                        // 3. Find potential matches
                        "3\n" +
                        "1\n" +                         // Select 1st lost report (ITM-0001)
                        "y\n" +                         // Initiate claim? Yes
                        "Alice Smith\n" +               // Claimant name
                        "alice@uni.edu\n" +             // Contact
                        "It has a GOLANG sticker underneath\n" + // Identifying info

                        // 4. Search all records
                        "4\n" +
                        "MacBook\n" +                   // Keyword
                        "\n" +                          // Category (blank = all)
                        "\n" +                          // Color (blank = all)
                        "\n" +                          // Location (blank = all)
                        "\n" +                          // Status (blank = all)

                        // 5. Browse records
                        "5\n" +
                        "1\n" +                         // Scope: All items (choice 1)
                        "1\n" +                         // Status: All statuses (choice 1)
                        "1\n" +                         // Sort: Date newest first (choice 1)

                        // 6. Manage claims and returns
                        "6\n" +
                        "4\n" +                         // Submenu: Approve claim
                        "CLM-0001\n" +                  // Claim ID
                        "Officer Bob\n" +               // Reviewer name
                        "STF-2002\n" +                  // Staff ID
                        "Sticker matches reported private details\n" + // Notes
                        "6\n" +                         // Submenu: Complete handover
                        "CLM-0001\n" +                  // Claim ID
                        "Officer Bob\n" +               // Operator name
                        "STF-2002\n" +                  // Staff ID
                        "Handed over to Alice in person\n" + // Confirmation notes
                        "7\n" +                         // Submenu: Back to main menu

                        // 7. My reports
                        "7\n" +
                        "1\n" +                         // Lookup by reporter name
                        "Alice\n" +                     // Name
                        "n\n" +                         // Update open items? n

                        // 8. Statistics dashboard
                        "8\n" +

                        // 9. Save and exit
                        "9\n";

                BufferedReader reader = new BufferedReader(new StringReader(demoScript));
                ByteArrayOutputStream outCapture = new ByteArrayOutputStream();
                PrintStream capturePs = new PrintStream(outCapture);

                InputHelper inputHelper = new InputHelper(reader, capturePs);
                Main app = new Main(itemService, matchingService, claimService, storage, inputHelper, capturePs, demoDir.toString());
                app.run();

                String output = outCapture.toString();

                // Option 1 assertions
                TestRunner.check("Demo: Option 1 reported lost item ITM-0001", output.contains("Generated Item ID: ITM-0001"));

                // Option 2 assertions
                TestRunner.check("Demo: Option 2 reported found item ITM-0002", output.contains("Generated Item ID: ITM-0002"));

                // Option 3 assertions (Matches and claim hand-off)
                TestRunner.check("Demo: Option 3 found top matches", output.contains("Match Analysis for ITM-0001"));
                TestRunner.check("Demo: Option 3 high match candidate", output.contains("Rank #1 Candidate Found Item: ITM-0002"));
                TestRunner.check("Demo: Option 3 claim submitted via handoff", output.contains("Generated Claim ID: CLM-0001"));

                // Option 4 assertions (Search)
                TestRunner.check("Demo: Option 4 search found records", output.contains("[Search Results] Total found: 2"));

                // Option 5 assertions (Browse)
                TestRunner.check("Demo: Option 5 browse records executed", output.contains("[Browse Results] Total records: 2"));

                // Option 6 assertions (Claims management)
                TestRunner.check("Demo: Option 6 claim approved", output.contains("Claim CLM-0001 APPROVED!"));
                TestRunner.check("Demo: Option 6 handover complete", output.contains("Handover complete! Claim is COMPLETED and Item is marked RETURNED."));

                // Option 7 assertions (My reports)
                TestRunner.check("Demo: Option 7 found reports for Alice", output.contains("[My Reports for 'Alice'] Found: 1"));

                // Option 8 assertions (Statistics dashboard)
                TestRunner.check("Demo: Option 8 statistics dashboard displayed", output.contains("STATISTICS DASHBOARD"));
                TestRunner.check("Demo: Option 8 recovery rate 100.0%", output.contains("Recovery Rate:         100.0%"));
                TestRunner.check("Demo: Option 8 returned count 2", output.contains("Returned Items:        2 (Lost: 1, Found: 1)"));

                // Option 9 assertions (Save & exit)
                TestRunner.check("Demo: Option 9 clean goodbye exit", output.contains("Saving data and exiting LostLink. Goodbye!"));

                // Privacy assertion: Confidential sticker details NEVER leaked in console output or activity log
                TestRunner.check("Demo: Confidential private details strictly hidden from console",
                        !output.contains("Secret sticker 'GOLANG' on bottom case"));

                List<String> activityLog = Files.readAllLines(storage.getActivityLogPath(), StandardCharsets.UTF_8);
                for (String logLine : activityLog) {
                    TestRunner.check("Demo: Confidential details strictly hidden from activity log",
                            !logLine.contains("Secret sticker 'GOLANG'"));
                }

                // Cleanup demo dir
                deleteRecursively(demoDir);
            }

        } catch (Exception e) {
            TestRunner.check("IntegrationTests threw unexpected exception: " + e.getMessage(), false);
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
