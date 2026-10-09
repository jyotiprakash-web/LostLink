import exception.InvalidItemException;
import java.time.LocalDate;
import model.FoundItem;
import model.ItemCategory;
import model.LostItem;
import model.MatchResult;
import model.Student;
import model.User;
import service.MatchingService;

/**
 * Acceptance tests for F13: Scoring engine with explanations.
 */
public class ScoringEngineTests {

    public static void run() {
        System.out.println("--- Running F13 Scoring Engine Tests ---");

        MatchingService matcher = new MatchingService();
        User reporter = new Student("Bob", "bob@campus.edu", "S110");

        try {
            // 1. Category-only match gives exactly 30
            LostItem lostLaptop = new LostItem("ITM-0001", "Laptop", ItemCategory.ELECTRONICS,
                    "Silver", "Library", LocalDate.of(2026, 1, 1), "Computer device", reporter);
            FoundItem foundPhone = new FoundItem("ITM-0002", "Phone", ItemCategory.ELECTRONICS,
                    "Black", "Gym", LocalDate.of(2026, 3, 1), "Mobile phone", reporter);

            MatchResult catOnly = matcher.compare(lostLaptop, foundPhone);
            TestRunner.assertEquals("Category-only match gives exactly 30", 30, catOnly.getScore());
            TestRunner.assertEquals("Category-only has 1 reason", 1, catOnly.getReasons().size());
            TestRunner.assertEquals("Reason text", "Category matched (+30)", catOnly.getReasons().get(0));

            // 2. No match gives 0
            LostItem lostBag = new LostItem("ITM-0003", "Backpack", ItemCategory.BAGS,
                    "Blue", "Hallway", LocalDate.of(2026, 1, 1), "School bag", reporter);
            FoundItem foundBook = new FoundItem("ITM-0004", "Novel", ItemCategory.BOOKS,
                    "Red", "Cafeteria", LocalDate.of(2026, 3, 1), "Fiction book", reporter);

            MatchResult noMatch = matcher.compare(lostBag, foundBook);
            TestRunner.assertEquals("Zero match score is 0", 0, noMatch.getScore());
            TestRunner.assertEquals("Zero match has 0 reasons", 0, noMatch.getReasons().size());

            // 3. Perfect hand-built pair gives exactly 100
            LocalDate sameDate = LocalDate.of(2026, 3, 1);
            LostItem lostPerfect = new LostItem("ITM-0005", "MacBook Pro", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", sameDate, "Laptop computer device", reporter);
            FoundItem foundPerfect = new FoundItem("ITM-0006", "MacBook Pro", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", sameDate, "Laptop computer device", reporter);

            MatchResult perfectMatch = matcher.compare(lostPerfect, foundPerfect);
            TestRunner.assertEquals("Perfect pair score is 100", 100, perfectMatch.getScore());
            TestRunner.assertEquals("Perfect pair has all 5 reasons", 5, perfectMatch.getReasons().size());
            TestRunner.check("Reasons contains category", perfectMatch.getReasons().contains("Category matched (+30)"));
            TestRunner.check("Reasons contains color", perfectMatch.getReasons().contains("Color matched (+15)"));
            TestRunner.check("Reasons contains location", perfectMatch.getReasons().contains("Location matched (+20)"));
            TestRunner.check("Reasons contains date", perfectMatch.getReasons().contains("Date proximity (0 days apart) (+10)"));
            TestRunner.check("Reasons contains keywords", perfectMatch.getReasons().contains("Keyword overlap (5 shared keywords) (+25)"));

            // 4. Whitespace and case variations do not alter the score
            FoundItem foundVariedCase = new FoundItem("ITM-0007", "  macbook pro  ", ItemCategory.ELECTRONICS,
                    "  SILVER  ", "  library 2nd floor  ", sameDate, "  LAPTOP COMPUTER DEVICE  ", reporter);

            MatchResult variedMatch = matcher.compare(lostPerfect, foundVariedCase);
            TestRunner.assertEquals("Case and whitespace variation yields identical score 100", 100, variedMatch.getScore());

            // 5. Date decay verification
            // 7 days apart: datePoints = round(10 * (14 - 7) / 14) = 5
            FoundItem found7Days = new FoundItem("ITM-0008", "MacBook Pro", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", sameDate.minusDays(7), "Laptop computer device", reporter);
            MatchResult match7Days = matcher.compare(lostPerfect, found7Days);
            TestRunner.assertEquals("Score with 7 days apart drops by 5", 95, match7Days.getScore());
            TestRunner.check("Contains 7 days reason", match7Days.getReasons().contains("Date proximity (7 days apart) (+5)"));

            // 14 days apart: datePoints = 0
            FoundItem found14Days = new FoundItem("ITM-0009", "MacBook Pro", ItemCategory.ELECTRONICS,
                    "Silver", "Library 2nd Floor", sameDate.minusDays(14), "Laptop computer device", reporter);
            MatchResult match14Days = matcher.compare(lostPerfect, found14Days);
            TestRunner.assertEquals("Score with 14 days apart has 0 date points (total 90)", 90, match14Days.getScore());

            // 6. Total score never exceeds 100
            TestRunner.check("Score never exceeds 100", perfectMatch.getScore() <= 100);

        } catch (InvalidItemException e) {
            TestRunner.check("Scoring engine test setup failed: " + e.getMessage(), false);
        }
    }
}
