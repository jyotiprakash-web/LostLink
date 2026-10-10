import java.util.Objects;

/**
 * Dependency-free test runner and assertion helper for LostLink.
 */
public class TestRunner {
    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.setProperty("lostlink.no_storage", "true");
        System.out.println("====================================================");
        System.out.println("           LostLink Test Suite Runner");
        System.out.println("====================================================");

        runF1SelfTests();
        EnumAndExceptionTests.run();
        UserTests.run();
        ItemTests.run();
        ClaimAndMatchTests.run();
        InputHelperTests.run();
        ItemServiceTests.run();
        ConsoleMenuShellTests.run();
        ReportLostItemTests.run();
        ReportFoundItemTests.run();
        SearchTests.run();
        BrowseAndSortTests.run();
        ScoringEngineTests.run();
        MatchRankingTests.run();
        ClaimSubmissionTests.run();
        ClaimReviewTests.run();
        HandoverCompletionTests.run();
        ClaimsConsoleWorkflowTests.run();
        MyReportsTests.run();
        StatisticsTests.run();
        CsvCodecTests.run();
        FileStorageLoadTests.run();
        FileStorageSaveTests.run();
        RestartPersistenceTests.run();
        ActivityLogTests.run();

        System.out.println("====================================================");
        System.out.printf("Test Summary: Total=%d, Passed=%d, Failed=%d%n", testsRun, testsPassed, testsFailed);
        System.out.println("====================================================");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    public static void runF1SelfTests() {
        System.out.println("--- Running F1 Self Tests ---");
        check("Trivial true assertion", true);
        assertEquals("Trivial integer equality", 42, 42);
        assertEquals("Trivial string equality", "LostLink", "LostLink");
        assertThrows("assertThrows self-test with expected exception", IllegalArgumentException.class, () -> {
            throw new IllegalArgumentException("Expected failure");
        });
        check("Main resolve default data dir", "data".equals(Main.resolveDataDir(new String[0])));
        check("Main resolve custom data dir flag", "custom_dir".equals(Main.resolveDataDir(new String[]{"--data-dir", "custom_dir"})));
    }

    public static void check(String testName, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("[PASS] " + testName);
        } else {
            testsFailed++;
            System.out.println("[FAIL] " + testName + " (expected true, got false)");
        }
    }

    public static void assertEquals(String testName, Object expected, Object actual) {
        testsRun++;
        if (Objects.equals(expected, actual)) {
            testsPassed++;
            System.out.println("[PASS] " + testName);
        } else {
            testsFailed++;
            System.out.println("[FAIL] " + testName + " (expected: <" + expected + "> but was: <" + actual + ">)");
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }

    public static void assertThrows(String testName, Class<? extends Throwable> expectedType, ThrowingRunnable action) {
        testsRun++;
        try {
            action.run();
            testsFailed++;
            System.out.println("[FAIL] " + testName + " (expected exception " + expectedType.getName() + " was not thrown)");
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                testsPassed++;
                System.out.println("[PASS] " + testName);
            } else {
                testsFailed++;
                System.out.println("[FAIL] " + testName + " (expected " + expectedType.getName() + " but caught " + t.getClass().getName() + ": " + t.getMessage() + ")");
            }
        }
    }

    public static int getTestsRun() {
        return testsRun;
    }

    public static int getTestsPassed() {
        return testsPassed;
    }

    public static int getTestsFailed() {
        return testsFailed;
    }

    public static void reset() {
        testsRun = 0;
        testsPassed = 0;
        testsFailed = 0;
    }
}
