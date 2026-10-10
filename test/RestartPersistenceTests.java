import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RestartPersistenceTests {

    public static void run() {
        System.out.println("--- Running F24 Restart Persistence Tests ---");
        try {
            Path tempDir = Files.createTempDirectory("lostlink_f24_restart_");
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

            // --- RUN 1: Separate JVM process creating items and claims ---
            String run1Input =
                    "1\n" +                     // Option 1: Report lost item
                    "ThinkPad Laptop\n" +       // Name
                    "1\n" +                     // Category: Electronics
                    "Black\n" +                 // Color
                    "Library 3rd Floor\n" +     // Location
                    "Near window desk\n" +      // Extra details
                    "2026-03-01\n" +            // Date
                    "Lenovo laptop with sticker\n" + // Description
                    "1\n" +                     // Student
                    "Frank\n" +                 // Reporter name
                    "frank@uni.edu\n" +         // Contact
                    "S888\n" +                  // Student ID
                    "2\n" +                     // Option 2: Report found item
                    "ThinkPad Charger\n" +      // Name
                    "1\n" +                     // Category: Electronics
                    "Black\n" +                 // Color
                    "Library 3rd Floor\n" +     // Location
                    "2026-03-02\n" +            // Date
                    "65W USB-C charger\n" +     // Description
                    "2\n" +                     // Staff
                    "Sarah Staff\n" +           // Reporter name
                    "sarah@uni.edu\n" +         // Contact
                    "STF01\n" +                 // Staff ID
                    "Serial number SN-9988\n" + // Confidential private details
                    "6\n" +                     // Option 6: Manage claims
                    "1\n" +                     // Submenu 1: Submit claim
                    "ITM-0002\n" +              // Found item ID
                    "ITM-0001\n" +              // Linked lost item ID
                    "Frank\n" +                 // Claimant name
                    "frank@uni.edu\n" +         // Contact
                    "Matches laptop voltage\n" +// Details
                    "7\n" +                     // Submenu 7: Back to main menu
                    "9\n";                      // Option 9: Save and exit

            ProcessBuilder pb1 = new ProcessBuilder(javaBin, "-cp", "out", "Main", "--data-dir", tempDir.toAbsolutePath().toString());
            pb1.redirectErrorStream(true);
            Process proc1 = pb1.start();

            try (OutputStreamWriter writer = new OutputStreamWriter(proc1.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(run1Input);
                writer.flush();
            }

            StringBuilder output1 = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc1.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output1.append(line).append("\n");
                }
            }

            int exitCode1 = proc1.waitFor();
            TestRunner.assertEquals("Run 1 JVM process exited with code 0", 0, exitCode1);
            TestRunner.check("Run 1 output created ITM-0001", output1.toString().contains("ITM-0001"));
            TestRunner.check("Run 1 output created ITM-0002", output1.toString().contains("ITM-0002"));
            TestRunner.check("Run 1 output created CLM-0001", output1.toString().contains("CLM-0001"));

            Path itemsPath = tempDir.resolve("items.csv");
            Path claimsPath = tempDir.resolve("claims.csv");
            TestRunner.check("Run 1 items.csv exists on disk", Files.exists(itemsPath));
            TestRunner.check("Run 1 claims.csv exists on disk", Files.exists(claimsPath));

            // --- RUN 2: Completely fresh JVM process loading same data directory ---
            String run2Input =
                    "1\n" +                     // Option 1: Report another lost item
                    "Water Bottle\n" +          // Name
                    "7\n" +                     // Category: Other
                    "Blue\n" +                  // Color
                    "Gym\n" +                   // Location
                    "\n" +                      // Extra details (optional blank)
                    "2026-03-03\n" +            // Date
                    "Metal flask\n" +           // Description
                    "1\n" +                     // Student
                    "Grace\n" +                 // Reporter name
                    "grace@uni.edu\n" +         // Contact
                    "S999\n" +                  // Student ID
                    "6\n" +                     // Option 6: Manage claims
                    "2\n" +                     // Submenu 2: View all claims
                    "4\n" +                     // Submenu 4: Approve claim
                    "CLM-0001\n" +              // Claim ID
                    "Sarah Staff\n" +           // Reviewer
                    "STF01\n" +                 // Staff ID
                    "Verified ownership\n" +    // Notes
                    "6\n" +                     // Submenu 6: Complete handover
                    "CLM-0001\n" +              // Claim ID
                    "Sarah Staff\n" +           // Staff name
                    "STF01\n" +                 // Staff ID
                    "Delivered to Frank\n" +    // Notes
                    "7\n" +                     // Submenu 7: Back to main menu
                    "8\n" +                     // Option 8: Statistics dashboard
                    "9\n";                      // Option 9: Save and exit

            ProcessBuilder pb2 = new ProcessBuilder(javaBin, "-cp", "out", "Main", "--data-dir", tempDir.toAbsolutePath().toString());
            pb2.redirectErrorStream(true);
            Process proc2 = pb2.start();

            try (OutputStreamWriter writer = new OutputStreamWriter(proc2.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(run2Input);
                writer.flush();
            }

            StringBuilder output2 = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc2.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output2.append(line).append("\n");
                }
            }

            int exitCode2 = proc2.waitFor();
            String out2 = output2.toString();

            TestRunner.assertEquals("Run 2 JVM process exited with code 0", 0, exitCode2);
            TestRunner.check("Run 2 startup loaded 2 items and 1 claim", out2.contains("Loaded 2 item(s) and 1 claim(s)"));
            TestRunner.check("Run 2 sequence continuity: Grace got ITM-0003", out2.contains("ITM-0003"));
            TestRunner.check("Run 2 viewed pre-existing CLM-0001", out2.contains("CLM-0001"));
            TestRunner.check("Run 2 approval succeeded", out2.contains("Claim CLM-0001 APPROVED"));
            TestRunner.check("Run 2 handover completed", out2.contains("Handover complete"));
            TestRunner.check("Run 2 statistics shows recovery rate 100.0%", out2.contains("Recovery Rate:         100.0%"));

            // Verify final disk state after Run 2
            List<String> finalItems = Files.readAllLines(itemsPath, StandardCharsets.UTF_8);
            TestRunner.assertEquals("Final items file has header + 3 items", 4, finalItems.size());
            TestRunner.check("ITM-0001 returned in file", finalItems.get(1).contains("RETURNED"));
            TestRunner.check("ITM-0002 returned in file", finalItems.get(2).contains("RETURNED"));
            TestRunner.check("ITM-0003 open in file", finalItems.get(3).contains("OPEN"));

            List<String> finalClaims = Files.readAllLines(claimsPath, StandardCharsets.UTF_8);
            TestRunner.assertEquals("Final claims file has header + 1 claim", 2, finalClaims.size());
            TestRunner.check("CLM-0001 completed in file", finalClaims.get(1).contains("COMPLETED"));

            // Cleanup temp dir
            deleteRecursively(tempDir);

        } catch (Exception e) {
            TestRunner.check("RestartPersistenceTests threw unexpected exception: " + e.getMessage(), false);
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
