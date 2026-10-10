import java.io.PrintStream;
import service.ItemService;
import service.MatchingService;
import util.InputHelper;

/**
 * Main entry point and interactive console controller for LostLink.
 */
public class Main {
    public static final String DEFAULT_DATA_DIR = "data";

    public static final String[] MENU_LABELS = new String[]{
        "Report a lost item",
        "Report a found item",
        "Find potential matches",
        "Search all records",
        "Browse lost and found items",
        "Manage claims and returns",
        "My reports",
        "Statistics dashboard",
        "Save data and exit"
    };

    private final ItemService itemService;
    private final MatchingService matchingService;
    private final InputHelper inputHelper;
    private final PrintStream out;
    private final String dataDir;
    private boolean running = true;

    public Main(ItemService itemService, MatchingService matchingService, InputHelper inputHelper, PrintStream out, String dataDir) {
        this.itemService = (itemService != null) ? itemService : new ItemService();
        this.matchingService = (matchingService != null) ? matchingService : new MatchingService();
        this.inputHelper = (inputHelper != null) ? inputHelper : new InputHelper();
        this.out = (out != null) ? out : System.out;
        this.dataDir = (dataDir != null) ? dataDir : DEFAULT_DATA_DIR;
    }

    public Main(ItemService itemService, InputHelper inputHelper, PrintStream out, String dataDir) {
        this(itemService, new MatchingService(), inputHelper, out, dataDir);
    }

    public static void main(String[] args) {
        String dataDir = resolveDataDir(args);
        InputHelper inputHelper = new InputHelper();
        ItemService itemService = new ItemService();
        Main app = new Main(itemService, inputHelper, System.out, dataDir);
        app.run();
    }

    public static String resolveDataDir(String[] args) {
        String dir = System.getProperty("lostlink.data");
        if (dir != null && !dir.trim().isEmpty()) {
            return dir.trim();
        }
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if ("--data-dir".equals(args[i]) && i + 1 < args.length) {
                    return args[i + 1].trim();
                }
            }
        }
        return DEFAULT_DATA_DIR;
    }

    public void run() {
        while (running) {
            displayMenu();
            int choice = inputHelper.readMenuChoice(1, MENU_LABELS.length);
            if (choice == -1) {
                // EOF encountered
                out.println("\n[EOF] End of input detected. Exiting cleanly.");
                handleExit();
                break;
            }
            handleMenuChoice(choice);
        }
    }

    public void displayMenu() {
        out.println("====================================================");
        out.println("                    LOSTLINK");
        out.println("          Intelligent Lost & Found System");
        out.println("====================================================");
        for (int i = 0; i < MENU_LABELS.length; i++) {
            out.printf("%d. %s%n", (i + 1), MENU_LABELS[i]);
        }
        out.println("====================================================");
    }

    public void handleMenuChoice(int choice) {
        switch (choice) {
            case 1:
                handleReportLostItem();
                break;
            case 2:
                handleReportFoundItem();
                break;
            case 3:
                handleFindPotentialMatches();
                break;
            case 4:
                handleSearchRecords();
                break;
            case 5:
                handleBrowseItems();
                break;
            case 6:
            case 7:
            case 8:
                out.println("[Coming Soon] " + MENU_LABELS[choice - 1] + " is not yet implemented.");
                break;
            case 9:
                handleExit();
                break;
            default:
                out.println("Unknown option: " + choice);
                break;
        }
    }

    public void handleReportLostItem() {
        out.println("\n--- Report a Lost Item ---");
        try {
            String name = inputHelper.readNonBlank("Item name");
            String categoryStr = inputHelper.readChoice("Select Category", model.ItemCategory.getNames());
            model.ItemCategory category = model.ItemCategory.fromString(categoryStr);
            String color = inputHelper.readNonBlank("Color");
            String location = inputHelper.readNonBlank("Last-seen location");
            String lastSeenDetails = inputHelper.readOptional("Additional last-seen details");
            java.time.LocalDate date = inputHelper.readDate("Date lost");
            String description = inputHelper.readNonBlank("Description");

            String roleChoice = inputHelper.readChoice("Reporter role", new String[]{"Student", "Staff"});
            String reporterName = inputHelper.readNonBlank("Reporter name");
            String contact = inputHelper.readNonBlank("Contact email or phone");

            model.User reporter;
            if ("Student".equalsIgnoreCase(roleChoice)) {
                String studentId = inputHelper.readOptional("Student ID");
                reporter = new model.Student(reporterName, contact, studentId);
            } else {
                String staffId = inputHelper.readOptional("Staff ID");
                reporter = new model.Staff(reporterName, contact, staffId);
            }

            model.LostItem item = itemService.reportLostItem(name, category, color, location, date,
                    description, reporter, contact, lastSeenDetails);

            out.println("\n[Success] Lost item report created successfully!");
            out.println("Generated Item ID: " + item.getId());
            out.println("Item Summary: " + item.getPublicSummary());
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        } catch (exception.InvalidItemException e) {
            out.println("[Error] Failed to create lost item report: " + e.getMessage());
        }
    }

    public void handleFindPotentialMatches() {
        out.println("\n--- Find Potential Matches ---");
        try {
            java.util.List<model.LostItem> openLostItems = new java.util.ArrayList<>();
            for (model.Item item : itemService.getLostItems()) {
                if (item.getStatus() == model.ItemStatus.OPEN) {
                    openLostItems.add((model.LostItem) item);
                }
            }

            if (openLostItems.isEmpty()) {
                out.println("[Notice] No open lost items available to match against.");
                return;
            }

            out.println("Select a lost item to find matches for:");
            for (int i = 0; i < openLostItems.size(); i++) {
                out.printf("  %d. %s - %s (%s, %s)%n", (i + 1), openLostItems.get(i).getId(),
                        openLostItems.get(i).getName(), openLostItems.get(i).getCategory(), openLostItems.get(i).getLocation());
            }

            int sel = inputHelper.readMenuChoice(1, openLostItems.size());
            if (sel == -1) {
                out.println("\n[EOF] End of input detected. Exiting.");
                handleExit();
                return;
            }

            model.LostItem selectedLost = openLostItems.get(sel - 1);
            model.MatchResult[] topMatches = matchingService.findTopMatches(selectedLost, itemService.getFoundItems());

            out.println("\n[Match Analysis for " + selectedLost.getId() + " - " + selectedLost.getName() + "]");
            if (topMatches.length == 0) {
                out.println("[Notice] No eligible found items met the minimum match score ("
                        + service.MatchingService.MIN_MATCH_SCORE + "/100).");
                return;
            }

            out.println("Displaying Top " + topMatches.length + " Match Candidate(s):");
            // Traversed with a loop over bounded MatchResult[] array
            for (int i = 0; i < topMatches.length; i++) {
                model.MatchResult match = topMatches[i];
                out.println("----------------------------------------------------");
                out.printf("Rank #%d Candidate Found Item: %s%n", (i + 1), match.getFoundItem().getId());
                out.printf("Similarity Score: %d/100 (Heuristic similarity only, NEVER proof of ownership)%n", match.getScore());
                out.println("Item Summary: " + match.getFoundItem().getPublicSummary());
                out.println("Reasons breakdown:");
                for (String reason : match.getReasons()) {
                    out.println("  - " + reason);
                }
            }
            out.println("----------------------------------------------------");

            String startClaim = inputHelper.readOptional("Would you like to initiate a claim for one of these found items? (y/n)");
            if ("y".equalsIgnoreCase(startClaim)) {
                out.println("[Notice] Ownership claims are verified manually by staff. Please note the Candidate Found Item ID and use Menu Option 6 ('Manage claims and returns') to submit a claim.");
            }
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        }
    }

    public void handleReportFoundItem() {
        out.println("\n--- Report a Found Item ---");
        try {
            String name = inputHelper.readNonBlank("Item name");
            String categoryStr = inputHelper.readChoice("Select Category", model.ItemCategory.getNames());
            model.ItemCategory category = model.ItemCategory.fromString(categoryStr);
            String color = inputHelper.readNonBlank("Color");
            String location = inputHelper.readNonBlank("Found location");
            java.time.LocalDate date = inputHelper.readDate("Date found");
            String description = inputHelper.readNonBlank("Description");

            String roleChoice = inputHelper.readChoice("Reporter role", new String[]{"Student", "Staff"});
            String reporterName = inputHelper.readNonBlank("Reporter name");
            String contact = inputHelper.readNonBlank("Contact email or phone");

            model.User reporter;
            if ("Student".equalsIgnoreCase(roleChoice)) {
                String studentId = inputHelper.readOptional("Student ID");
                reporter = new model.Student(reporterName, contact, studentId);
            } else {
                String staffId = inputHelper.readOptional("Staff ID");
                reporter = new model.Staff(reporterName, contact, staffId);
            }

            String privateDetails = inputHelper.readOptional("Private identifying details (confidential verification info)");

            model.FoundItem item = itemService.reportFoundItem(name, category, color, location, date,
                    description, reporter, contact, privateDetails);

            out.println("\n[Success] Found item report registered successfully!");
            out.println("Generated Item ID: " + item.getId());
            out.println("Item Public Summary: " + item.getPublicSummary());
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        } catch (exception.InvalidItemException e) {
            out.println("[Error] Failed to create found item report: " + e.getMessage());
        }
    }

    public void handleSearchRecords() {
        out.println("\n--- Search Records ---");
        try {
            String keyword = inputHelper.readOptional("Search keyword (matches name, description, color, location)");

            String catInput = inputHelper.readOptional("Category filter (leave blank for ALL, or enter category name)");
            model.ItemCategory catFilter = null;
            if (!catInput.isEmpty()) {
                try {
                    catFilter = model.ItemCategory.fromString(catInput);
                } catch (IllegalArgumentException e) {
                    out.println("Unknown category '" + catInput + "'. Ignoring category filter.");
                }
            }

            String colorFilter = inputHelper.readOptional("Color filter (leave blank for ALL)");
            String locationFilter = inputHelper.readOptional("Location filter (leave blank for ALL)");

            String statusInput = inputHelper.readOptional("Status filter (OPEN, CLAIM_PENDING, RETURNED, CLOSED, or leave blank for ALL)");
            model.ItemStatus statusFilter = null;
            if (!statusInput.isEmpty()) {
                try {
                    statusFilter = model.ItemStatus.fromString(statusInput);
                } catch (IllegalArgumentException e) {
                    out.println("Unknown status '" + statusInput + "'. Ignoring status filter.");
                }
            }

            java.util.List<model.Item> results = itemService.search(keyword, catFilter, colorFilter, locationFilter, statusFilter);
            out.println("\n[Search Results] Total found: " + results.size());
            if (results.isEmpty()) {
                out.println("No records matched your search criteria.");
            } else {
                for (int i = 0; i < results.size(); i++) {
                    out.println((i + 1) + ". " + results.get(i).getPublicSummary());
                }
            }
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        }
    }

    public void handleBrowseItems() {
        out.println("\n--- Browse Lost and Found Items ---");
        try {
            String typeChoice = inputHelper.readChoice("Filter by item type", new String[]{"All items", "Lost items only", "Found items only"});
            String typeFilter = "ALL";
            if ("Lost items only".equals(typeChoice)) {
                typeFilter = "LOST";
            } else if ("Found items only".equals(typeChoice)) {
                typeFilter = "FOUND";
            }

            String statusChoice = inputHelper.readChoice("Filter by status", new String[]{"All statuses", "OPEN only", "CLAIM_PENDING only", "RETURNED only", "CLOSED only"});
            model.ItemStatus statusFilter = null;
            if ("OPEN only".equals(statusChoice)) {
                statusFilter = model.ItemStatus.OPEN;
            } else if ("CLAIM_PENDING only".equals(statusChoice)) {
                statusFilter = model.ItemStatus.CLAIM_PENDING;
            } else if ("RETURNED only".equals(statusChoice)) {
                statusFilter = model.ItemStatus.RETURNED;
            } else if ("CLOSED only".equals(statusChoice)) {
                statusFilter = model.ItemStatus.CLOSED;
            }

            String sortChoice = inputHelper.readChoice("Sort items by", new String[]{"Date (newest first)", "Date (oldest first)", "Name (A-Z)"});
            java.util.Comparator<model.Item> comparator = service.ItemService.SORT_BY_DATE_DESC;
            if ("Date (oldest first)".equals(sortChoice)) {
                comparator = service.ItemService.SORT_BY_DATE_ASC;
            } else if ("Name (A-Z)".equals(sortChoice)) {
                comparator = service.ItemService.SORT_BY_NAME_ASC;
            }

            java.util.List<model.Item> results = itemService.browse(typeFilter, statusFilter, comparator);
            out.println("\n[Browse Results] Total records: " + results.size());
            if (results.isEmpty()) {
                out.println("No records found matching the browse criteria.");
            } else {
                for (int i = 0; i < results.size(); i++) {
                    out.println((i + 1) + ". " + results.get(i).getPublicSummary());
                }
            }
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        }
    }

    public void handleExit() {
        out.println("Saving data and exiting LostLink. Goodbye!");
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public String getDataDir() {
        return dataDir;
    }
}
