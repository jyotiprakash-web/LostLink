import exception.InvalidItemException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.FoundItem;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.MatchResult;
import model.Student;
import model.User;
import service.ItemService;
import service.MatchingService;
import util.InputHelper;

/**
 * Acceptance tests for F14: Ranking and top-three display (menu 3).
 */
public class MatchRankingTests {

    public static void run() {
        System.out.println("--- Running F14 Match Ranking and Top-Three Tests ---");

        MatchingService matcher = new MatchingService();
        User reporter = new Student("Alice", "alice@campus.edu", "S101");
        LocalDate baseDate = LocalDate.of(2026, 3, 10);

        try {
            LostItem targetLost = new LostItem("ITM-0001", "MacBook Pro Laptop", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", baseDate, "Apple laptop", reporter);

            // Create 5 found items with different scores and statuses
            // Found 1: High match (95+ score)
            FoundItem foundHigh = new FoundItem("ITM-0002", "MacBook Pro Laptop", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", baseDate, "Apple laptop computer", reporter);

            // Found 2: Good match (category + color + location = 65)
            FoundItem foundMed = new FoundItem("ITM-0003", "Dell Inspiron", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", baseDate.minusDays(20), "Dell machine", reporter);

            // Found 3: Low-qualifying match (category only = 30)
            FoundItem foundLow = new FoundItem("ITM-0004", "Sony Headphones", ItemCategory.ELECTRONICS,
                    "Black", "Cafeteria", baseDate.minusDays(20), "Audio headset", reporter);

            // Found 4: Ineligible status RETURNED (even if high match)
            FoundItem foundReturned = new FoundItem("ITM-0005", "MacBook Pro Laptop", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", baseDate, "Apple laptop", reporter);
            foundReturned.setStatus(ItemStatus.RETURNED);

            // Found 5: Ineligible status CLOSED
            FoundItem foundClosed = new FoundItem("ITM-0006", "MacBook Pro Laptop", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", baseDate, "Apple laptop", reporter);
            foundClosed.setStatus(ItemStatus.CLOSED);

            // Found 6: Below MIN_MATCH_SCORE (< 30, e.g. BOOKS)
            FoundItem foundBelowMin = new FoundItem("ITM-0007", "Calculus Book", ItemCategory.BOOKS,
                    "Red", "Gym", baseDate.minusDays(25), "Textbook", reporter);

            List<FoundItem> allCandidates = new ArrayList<>();
            allCandidates.add(foundHigh);
            allCandidates.add(foundMed);
            allCandidates.add(foundLow);
            allCandidates.add(foundReturned);
            allCandidates.add(foundClosed);
            allCandidates.add(foundBelowMin);

            // 1. Ineligible found items are excluded (RETURNED and CLOSED excluded)
            // 2. Below-minimum candidates (< 30) are excluded
            MatchResult[] topMatches = matcher.findTopMatches(targetLost, allCandidates);

            // Should contain exactly 3: foundHigh, foundMed, foundLow
            TestRunner.assertEquals("Top matches bounded to at most 3", 3, topMatches.length);
            TestRunner.assertEquals("1st candidate is foundHigh", "ITM-0002", topMatches[0].getFoundItem().getId());
            TestRunner.assertEquals("2nd candidate is foundMed", "ITM-0003", topMatches[1].getFoundItem().getId());
            TestRunner.assertEquals("3rd candidate is foundLow", "ITM-0004", topMatches[2].getFoundItem().getId());

            // 3. Descending order verified
            TestRunner.check("1st score >= 2nd score", topMatches[0].getScore() >= topMatches[1].getScore());
            TestRunner.check("2nd score >= 3rd score", topMatches[1].getScore() >= topMatches[2].getScore());

            // 4. Ineligible lost item (RETURNED) returns 0 candidates
            targetLost.setStatus(ItemStatus.RETURNED);
            MatchResult[] returnedLostMatches = matcher.findTopMatches(targetLost, allCandidates);
            TestRunner.assertEquals("Ineligible lost item yields 0 matches", 0, returnedLostMatches.length);
            targetLost.setStatus(ItemStatus.OPEN); // restore

            // 5. More than 3 eligible candidates returns exactly 3
            FoundItem foundMed2 = new FoundItem("ITM-0008", "HP Laptop", ItemCategory.ELECTRONICS,
                    "Silver", "Auditorium", baseDate.minusDays(20), "HP computer", reporter);
            allCandidates.add(foundMed2);
            MatchResult[] bounded3 = matcher.findTopMatches(targetLost, allCandidates);
            TestRunner.assertEquals("More than 3 candidates strictly bounded to 3", 3, bounded3.length);

            // 6. Fewer than 3 candidates handled gracefully
            List<FoundItem> onlyOneList = new ArrayList<>();
            onlyOneList.add(foundHigh);
            MatchResult[] singleMatch = matcher.findTopMatches(targetLost, onlyOneList);
            TestRunner.assertEquals("Single candidate yields array length 1", 1, singleMatch.length);

            // 7. Deterministic tie-breaking (equal scores break on date desc, then ID asc)
            FoundItem tieA = new FoundItem("ITM-0010", "Phone", ItemCategory.ELECTRONICS,
                    "Black", "Gym", LocalDate.of(2026, 3, 5), "Smartphone", reporter);
            FoundItem tieB = new FoundItem("ITM-0020", "Phone", ItemCategory.ELECTRONICS,
                    "Black", "Gym", LocalDate.of(2026, 3, 8), "Smartphone", reporter); // more recent

            List<FoundItem> tieList = new ArrayList<>();
            tieList.add(tieA);
            tieList.add(tieB);

            MatchResult[] run1 = matcher.findTopMatches(targetLost, tieList);
            MatchResult[] run2 = matcher.findTopMatches(targetLost, tieList);

            TestRunner.assertEquals("Tie run 1 first element is more recent date (ITM-0020)", "ITM-0020", run1[0].getFoundItem().getId());
            TestRunner.assertEquals("Tie run 2 first element is identical (deterministic)", run1[0].getFoundItem().getId(), run2[0].getFoundItem().getId());

            // 8. Console display check via Main
            ItemService service = new ItemService();
            service.addItem(targetLost);
            service.addItem(foundHigh);
            service.addItem(foundMed);

            // Scripted input: 3 (Find matches) -> 1 (select targetLost) -> n (do not start claim) -> 9 (exit)
            String scripted = "3\n1\nn\n9\n";
            BufferedReader reader = new BufferedReader(new StringReader(scripted));
            ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
            PrintStream captureOut = new PrintStream(outputCapture);

            InputHelper inputHelper = new InputHelper(reader, captureOut);
            Main app = new Main(service, matcher, inputHelper, captureOut, "data");
            app.run();

            String outStr = outputCapture.toString();
            TestRunner.check("Console output shows match analysis header", outStr.contains("Match Analysis for ITM-0001"));
            TestRunner.check("Console output contains similarity phrasing", outStr.contains("Heuristic similarity only, NEVER proof of ownership"));
            TestRunner.check("Console output never calls score probability of ownership", !outStr.contains("probability of ownership"));

        } catch (InvalidItemException e) {
            TestRunner.check("Match ranking test setup threw: " + e.getMessage(), false);
        }
    }
}
