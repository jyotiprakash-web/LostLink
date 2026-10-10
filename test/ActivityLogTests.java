import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import service.ClaimService;
import service.FileStorage;
import service.ItemService;
import service.MatchingService;
import util.InputHelper;

public class ActivityLogTests {

    public static void run() {
        System.out.println("--- Running F25 Activity Log Tests ---");
        try {
            Path testDir = Files.createTempDirectory("lostlink_f25_log_");

            // 1. Direct logging and formatting verification
            {
                FileStorage storage = new FileStorage(testDir.toString());
                storage.logActivity("REPORT_LOST", "Item: ITM-0001 | Name: Blue Jacket | Reporter: Alice (Student)");
                storage.logActivity("REPORT_FOUND", "Item: ITM-0002 | Name: Keys | Reporter: Bob (Staff)");
                storage.logActivity("CLAIM_SUBMITTED", "Claim: CLM-0001 | Item: ITM-0002 | Claimant: Charlie");

                TestRunner.check("activity.log exists", Files.exists(storage.getActivityLogPath()));

                List<String> lines = Files.readAllLines(storage.getActivityLogPath(), StandardCharsets.UTF_8);
                TestRunner.assertEquals("activity.log has 3 lines", 3, lines.size());
                TestRunner.check("Line 1 contains REPORT_LOST", lines.get(0).contains("[REPORT_LOST]"));
                TestRunner.check("Line 1 contains ITM-0001", lines.get(0).contains("ITM-0001"));
                TestRunner.check("Line 1 contains timestamp pattern", lines.get(0).matches("^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*"));
                TestRunner.check("Line 2 contains REPORT_FOUND", lines.get(1).contains("[REPORT_FOUND]"));
                TestRunner.check("Line 3 contains CLAIM_SUBMITTED", lines.get(2).contains("[CLAIM_SUBMITTED]"));

                // readRecentActivities test
                List<String> recent2 = storage.readRecentActivities(2);
                TestRunner.assertEquals("readRecentActivities returns last 2", 2, recent2.size());
                TestRunner.check("Recent 1 is REPORT_FOUND", recent2.get(0).contains("REPORT_FOUND"));
                TestRunner.check("Recent 2 is CLAIM_SUBMITTED", recent2.get(1).contains("CLAIM_SUBMITTED"));
            }

            // 2. Full Console workflow logging & privacy guarantee
            {
                Path consoleDir = testDir.resolve("console_log");
                FileStorage storage = new FileStorage(consoleDir.toString());
                ItemService itemService = new ItemService();
                ClaimService claimService = new ClaimService(itemService);

                String scripted =
                        // 1. Report lost
                        "1\nRed Backpack\n2\nRed\nLibrary\nDesk 3\n2026-03-10\nNike bag\n1\nDave\ndave@uni.edu\nS101\n" +
                        // 2. Report found with confidential private details
                        "2\nRed Backpack\n2\nRed\nLibrary\n2026-03-11\nNike bag found\n2\nOfficer Mary\nmary@uni.edu\nSTF05\nSecret badge #99 inside pocket\n" +
                        // 3. Submit claim
                        "6\n1\nITM-0002\nITM-0001\nDave\ndave@uni.edu\nBadge inside\n7\n" +
                        // 4. Request more info
                        "6\n3\nCLM-0001\nOfficer Mary\nSTF05\nConfirm badge number\n7\n" +
                        // 5. Approve claim
                        "6\n4\nCLM-0001\nOfficer Mary\nSTF05\nBadge verified\n7\n" +
                        // 6. Complete handover
                        "6\n6\nCLM-0001\nOfficer Mary\nSTF05\nItem returned to Dave\n7\n" +
                        // 7. Exit
                        "9\n";

                BufferedReader reader = new BufferedReader(new StringReader(scripted));
                ByteArrayOutputStream outCapture = new ByteArrayOutputStream();
                PrintStream capturePs = new PrintStream(outCapture);

                InputHelper inputHelper = new InputHelper(reader, capturePs);
                Main app = new Main(itemService, new MatchingService(), claimService, storage, inputHelper, capturePs, consoleDir.toString());
                app.run();

                TestRunner.check("Console activity log was created", Files.exists(storage.getActivityLogPath()));
                List<String> logLines = Files.readAllLines(storage.getActivityLogPath(), StandardCharsets.UTF_8);

                TestRunner.assertEquals("Total activity log events recorded", 6, logLines.size());
                TestRunner.check("Event 1 is REPORT_LOST", logLines.get(0).contains("[REPORT_LOST]"));
                TestRunner.check("Event 2 is REPORT_FOUND", logLines.get(1).contains("[REPORT_FOUND]"));
                TestRunner.check("Event 3 is CLAIM_SUBMITTED", logLines.get(2).contains("[CLAIM_SUBMITTED]"));
                TestRunner.check("Event 4 is CLAIM_INFO_REQUESTED", logLines.get(3).contains("[CLAIM_INFO_REQUESTED]"));
                TestRunner.check("Event 5 is CLAIM_APPROVED", logLines.get(4).contains("[CLAIM_APPROVED]"));
                TestRunner.check("Event 6 is HANDOVER_COMPLETED", logLines.get(5).contains("[HANDOVER_COMPLETED]"));

                // Critical Privacy Check: Secret badge #99 must NEVER appear in activity.log!
                for (String line : logLines) {
                    TestRunner.check("Log strictly hides private details", !line.contains("Secret badge #99"));
                }
            }

            // Cleanup temp dir
            deleteRecursively(testDir);

        } catch (Exception e) {
            TestRunner.check("ActivityLogTests threw unexpected exception: " + e.getMessage(), false);
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
