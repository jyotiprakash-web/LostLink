import java.io.PrintStream;
import service.ClaimService;
import service.FileStorage;
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
    private final ClaimService claimService;
    private final FileStorage fileStorage;
    private final InputHelper inputHelper;
    private final PrintStream out;
    private final String dataDir;
    private boolean running = true;

    public Main(ItemService itemService, MatchingService matchingService, ClaimService claimService, FileStorage fileStorage, InputHelper inputHelper, PrintStream out, String dataDir) {
        this.itemService = (itemService != null) ? itemService : new ItemService();
        this.matchingService = (matchingService != null) ? matchingService : new MatchingService();
        this.claimService = (claimService != null) ? claimService : new ClaimService(this.itemService);
        this.inputHelper = (inputHelper != null) ? inputHelper : new InputHelper();
        this.out = (out != null) ? out : System.out;
        this.dataDir = (dataDir != null) ? dataDir : DEFAULT_DATA_DIR;
        this.fileStorage = fileStorage;
    }

    public Main(ItemService itemService, MatchingService matchingService, ClaimService claimService, InputHelper inputHelper, PrintStream out, String dataDir) {
        this(itemService, matchingService, claimService,
                Boolean.getBoolean("lostlink.no_storage") ? null : new FileStorage((dataDir != null) ? dataDir : DEFAULT_DATA_DIR),
                inputHelper, out, dataDir);
    }

    public Main(ItemService itemService, MatchingService matchingService, InputHelper inputHelper, PrintStream out, String dataDir) {
        this(itemService, matchingService, new ClaimService(itemService), inputHelper, out, dataDir);
    }

    public Main(ItemService itemService, InputHelper inputHelper, PrintStream out, String dataDir) {
        this(itemService, new MatchingService(), new ClaimService(itemService), inputHelper, out, dataDir);
    }

    public static void main(String[] args) {
        String dataDir = resolveDataDir(args);
        InputHelper inputHelper = new InputHelper();
        ItemService itemService = new ItemService();
        ClaimService claimService = new ClaimService(itemService);
        FileStorage fileStorage = new FileStorage(dataDir);
        Main app = new Main(itemService, new MatchingService(), claimService, fileStorage, inputHelper, System.out, dataDir);
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
        loadData();
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

    public void loadData() {
        if (fileStorage == null) return;
        try {
            int loadedItems = fileStorage.loadItems(itemService);
            int loadedClaims = fileStorage.loadClaims(claimService, itemService);
            if (loadedItems > 0 || loadedClaims > 0) {
                out.printf("Loaded %d item(s) and %d claim(s) from %s.%n", loadedItems, loadedClaims, dataDir);
            }
        } catch (java.io.IOException e) {
            out.println("[Warning] Failed to load data from " + dataDir + ": " + e.getMessage());
        }
    }

    private void persistItemAppendSafe(model.Item item) {
        if (fileStorage != null && item != null) {
            try {
                fileStorage.appendItem(item);
            } catch (java.io.IOException e) {
                out.println("[Warning] Failed to save item to file: " + e.getMessage());
            }
        }
    }

    private void persistAllSafe(String context) {
        if (fileStorage != null) {
            try {
                fileStorage.saveAll(itemService, claimService);
            } catch (java.io.IOException e) {
                out.println("[Warning] Failed to save " + context + ": " + e.getMessage());
            }
        }
    }

    private void logActivitySafe(String action, String details) {
        if (fileStorage != null) {
            fileStorage.logActivity(action, details);
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
                handleManageClaims();
                break;
            case 7:
                handleMyReports();
                break;
            case 8:
                handleStatisticsDashboard();
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
            persistItemAppendSafe(item);
            logActivitySafe("REPORT_LOST", "Item: " + item.getId() + " | Name: " + item.getName() + " | Category: " + item.getCategory() + " | Reporter: " + item.getReporter().getName() + " (" + item.getReporter().displayRole() + ")");
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
                if (topMatches.length == 1) {
                    handleSubmitClaim(topMatches[0].getFoundItem().getId(), selectedLost.getId());
                } else {
                    out.println("Select candidate to claim (1-" + topMatches.length + "):");
                    int candNum = inputHelper.readMenuChoice(1, topMatches.length);
                    if (candNum != -1) {
                        handleSubmitClaim(topMatches[candNum - 1].getFoundItem().getId(), selectedLost.getId());
                    }
                }
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
            persistItemAppendSafe(item);
            logActivitySafe("REPORT_FOUND", "Item: " + item.getId() + " | Name: " + item.getName() + " | Category: " + item.getCategory() + " | Reporter: " + item.getReporter().getName() + " (" + item.getReporter().displayRole() + ")");
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

    public void handleManageClaims() {
        boolean inClaimsMenu = true;
        while (inClaimsMenu && running) {
            out.println("\n====================================================");
            out.println("            Manage Claims and Returns");
            out.println("====================================================");
            out.println("1. Submit a new ownership claim");
            out.println("2. View all claims");
            out.println("3. Request more information on a pending claim");
            out.println("4. Approve a pending claim");
            out.println("5. Reject a pending claim");
            out.println("6. Complete item handover (mark as returned)");
            out.println("7. Return to main menu");
            out.println("====================================================");

            int choice = inputHelper.readMenuChoice(1, 7);
            if (choice == -1) {
                out.println("\n[EOF] End of input detected. Exiting.");
                handleExit();
                return;
            }

            switch (choice) {
                case 1:
                    handleSubmitClaim(null, null);
                    break;
                case 2:
                    handleViewAllClaims();
                    break;
                case 3:
                    handleRequestMoreInfo();
                    break;
                case 4:
                    handleApproveClaim();
                    break;
                case 5:
                    handleRejectClaim();
                    break;
                case 6:
                    handleCompleteHandover();
                    break;
                case 7:
                    inClaimsMenu = false;
                    break;
            }
        }
    }

    public void handleSubmitClaim(String preFilledFoundId, String preFilledLostId) {
        out.println("\n--- Submit an Ownership Claim ---");
        try {
            String foundId = (preFilledFoundId != null && !preFilledFoundId.isEmpty())
                    ? preFilledFoundId
                    : inputHelper.readNonBlank("Found item ID to claim (e.g. ITM-0001)");

            String lostId = (preFilledLostId != null && !preFilledLostId.isEmpty())
                    ? preFilledLostId
                    : inputHelper.readOptional("Linked lost item ID (if any)");

            String claimantName = inputHelper.readNonBlank("Claimant full name");
            String contact = inputHelper.readNonBlank("Claimant contact phone/email");
            String details = inputHelper.readNonBlank("Identifying information / proof of ownership");

            model.Claim claim = claimService.submitClaim(foundId, lostId, claimantName, contact, details, java.time.LocalDate.now());
            out.println("\n[Success] Claim submitted successfully!");
            out.println("Generated Claim ID: " + claim.getId());
            out.println("Claim Summary: " + claim.getSummary());
            persistAllSafe("claim submission");
            logActivitySafe("CLAIM_SUBMITTED", "Claim: " + claim.getId() + " | Found Item: " + claim.getFoundItemId() + " | Claimant: " + claim.getClaimantName());
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        } catch (exception.ItemNotFoundException e) {
            out.println("[Error] Item not found: " + e.getMessage());
        } catch (exception.InvalidClaimException e) {
            out.println("[Error] Invalid claim: " + e.getMessage());
        }
    }

    public void handleViewAllClaims() {
        out.println("\n--- All Registered Claims ---");
        java.util.List<model.Claim> claims = claimService.getAllClaims();
        if (claims.isEmpty()) {
            out.println("[Notice] No claims have been submitted yet.");
            return;
        }
        for (int i = 0; i < claims.size(); i++) {
            out.println((i + 1) + ". " + claims.get(i).getSummary());
        }
    }

    public void handleRequestMoreInfo() {
        out.println("\n--- Request More Information ---");
        try {
            String claimId = inputHelper.readNonBlank("Claim ID");
            String staffName = inputHelper.readNonBlank("Reviewing staff name");
            String staffId = inputHelper.readOptional("Staff ID");
            model.Staff staff = new model.Staff(staffName, "", staffId);
            out.println("Operating as: " + staff.getName() + " [" + staff.displayRole() + "]");

            String notes = inputHelper.readNonBlank("Questions / information needed");
            claimService.requestMoreInformation(claimId, staff.getName(), notes);
            out.println("[Success] Claim " + claimId + " updated with notes. Status remains PENDING.");
            persistAllSafe("claim notes update");
            logActivitySafe("CLAIM_INFO_REQUESTED", "Claim: " + claimId + " | Reviewer: " + staff.getName() + " | Notes: " + notes);
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        } catch (exception.InvalidClaimException e) {
            out.println("[Error] " + e.getMessage());
        }
    }

    public void handleApproveClaim() {
        out.println("\n--- Approve Ownership Claim ---");
        try {
            String claimId = inputHelper.readNonBlank("Claim ID to approve");
            String staffName = inputHelper.readNonBlank("Reviewing staff name");
            String staffId = inputHelper.readOptional("Staff ID");
            model.Staff staff = new model.Staff(staffName, "", staffId);
            out.println("Operating as: " + staff.getName() + " [" + staff.displayRole() + "]");

            String notes = inputHelper.readNonBlank("Approval justification notes");
            claimService.approveClaim(claimId, staff.getName(), notes);
            out.println("[Success] Claim " + claimId + " APPROVED! Item is awaiting handover completion.");
            persistAllSafe("claim approval");
            logActivitySafe("CLAIM_APPROVED", "Claim: " + claimId + " | Reviewer: " + staff.getName() + " | Notes: " + notes);
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        } catch (exception.InvalidClaimException | exception.ItemNotFoundException e) {
            out.println("[Error] " + e.getMessage());
        }
    }

    public void handleRejectClaim() {
        out.println("\n--- Reject Ownership Claim ---");
        try {
            String claimId = inputHelper.readNonBlank("Claim ID to reject");
            String staffName = inputHelper.readNonBlank("Reviewing staff name");
            String staffId = inputHelper.readOptional("Staff ID");
            model.Staff staff = new model.Staff(staffName, "", staffId);
            out.println("Operating as: " + staff.getName() + " [" + staff.displayRole() + "]");

            String notes = inputHelper.readNonBlank("Rejection reason notes");
            claimService.rejectClaim(claimId, staff.getName(), notes);
            out.println("[Success] Claim " + claimId + " REJECTED.");
            persistAllSafe("claim rejection");
            logActivitySafe("CLAIM_REJECTED", "Claim: " + claimId + " | Reviewer: " + staff.getName() + " | Notes: " + notes);
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        } catch (exception.InvalidClaimException | exception.ItemNotFoundException e) {
            out.println("[Error] " + e.getMessage());
        }
    }

    public void handleCompleteHandover() {
        out.println("\n--- Complete Item Handover ---");
        try {
            String claimId = inputHelper.readNonBlank("APPROVED Claim ID to complete");
            String staffName = inputHelper.readNonBlank("Staff operator name");
            String staffId = inputHelper.readOptional("Staff ID");
            model.Staff staff = new model.Staff(staffName, "", staffId);
            out.println("Operating as: " + staff.getName() + " [" + staff.displayRole() + "]");

            String notes = inputHelper.readNonBlank("Handover confirmation notes");
            claimService.completeHandover(claimId, staff.getName(), notes);
            out.println("[Success] Handover complete! Claim is COMPLETED and Item is marked RETURNED.");
            persistAllSafe("handover completion");
            logActivitySafe("HANDOVER_COMPLETED", "Claim: " + claimId + " | Operator: " + staff.getName() + " | Notes: " + notes);
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        } catch (exception.InvalidClaimException | exception.ItemNotFoundException e) {
            out.println("[Error] " + e.getMessage());
        }
    }

    public void handleMyReports() {
        out.println("\n--- My Reports ---");
        try {
            out.println("1. Lookup reports by reporter name");
            out.println("2. Lookup report by item ID");
            out.println("3. Return to main menu");
            int choice = inputHelper.readMenuChoice(1, 3);
            if (choice == -1 || choice == 3) {
                return;
            }

            java.util.List<model.Item> reports = new java.util.ArrayList<>();
            if (choice == 1) {
                String name = inputHelper.readNonBlank("Enter your reporter name (case-insensitive)");
                reports = itemService.getReportsByReporterName(name);
                out.println("\n[My Reports for '" + name + "'] Found: " + reports.size());
                if (reports.isEmpty()) {
                    out.println("No reports found under that reporter name.");
                    return;
                }
                for (int i = 0; i < reports.size(); i++) {
                    out.println((i + 1) + ". " + reports.get(i).getPublicSummary());
                }
            } else if (choice == 2) {
                String id = inputHelper.readNonBlank("Enter Item ID (e.g. ITM-0001)");
                try {
                    model.Item item = itemService.getById(id);
                    reports.add(item);
                    out.println("\n[Report Found]");
                    out.println(item.getPublicSummary());
                } catch (exception.ItemNotFoundException e) {
                    out.println("[Error] Item not found with ID: " + id);
                    return;
                }
            }

            String updateChoice = inputHelper.readOptional("Would you like to update details for an OPEN item? (y/n)");
            if ("y".equalsIgnoreCase(updateChoice)) {
                String targetId = inputHelper.readNonBlank("Enter Item ID to update");
                try {
                    model.Item item = itemService.getById(targetId);
                    if (item.getStatus() != model.ItemStatus.OPEN) {
                        out.println("[Notice] Only items with status OPEN can be edited. Current status is " + item.getStatus() + ".");
                        return;
                    }
                    out.println("Editing item " + targetId + " (press Enter to keep current value):");
                    String newColor = inputHelper.readOptional("New color (current: " + item.getColor() + ")");
                    String newLoc = inputHelper.readOptional("New location (current: " + item.getLocation() + ")");
                    String newDesc = inputHelper.readOptional("New description (current: " + item.getDescription() + ")");
                    String newContact = inputHelper.readOptional("New contact (current: " + item.getContact() + ")");
                    String newExtra = "";
                    if (item instanceof model.LostItem) {
                        newExtra = inputHelper.readOptional("New last seen details (current: " + ((model.LostItem) item).getLastSeenLocationDetails() + ")");
                    } else if (item instanceof model.FoundItem) {
                        newExtra = inputHelper.readOptional("New private details");
                    }

                    itemService.updateOpenItemDetails(targetId, newColor, newLoc, newDesc, newContact, newExtra);
                    out.println("[Success] Item " + targetId + " details updated successfully!");
                    out.println("Updated summary: " + item.getPublicSummary());
                    persistAllSafe("item update");
                    logActivitySafe("ITEM_UPDATED", "Item: " + targetId + " | Color: " + item.getColor() + " | Location: " + item.getLocation());
                } catch (exception.ItemNotFoundException e) {
                    out.println("[Error] Item not found with ID: " + targetId);
                } catch (exception.InvalidItemException e) {
                    out.println("[Error] Failed to update item: " + e.getMessage());
                }
            }
        } catch (java.util.NoSuchElementException e) {
            out.println("\n[EOF] End of input detected. Exiting.");
            handleExit();
        }
    }

    public void handleStatisticsDashboard() {
        out.println("\n====================================================");
        out.println("               STATISTICS DASHBOARD");
        out.println("====================================================");
        model.SystemStatistics stats = itemService.getStatistics(claimService);
        out.printf("Total Lost Reports:    %d%n", stats.getTotalLost());
        out.printf("Total Found Reports:   %d%n", stats.getTotalFound());
        out.printf("Total Reports:         %d%n", stats.getTotalItems());
        out.printf("Unresolved Items:      %d (OPEN or CLAIM_PENDING)%n", stats.getUnresolvedItems());
        out.printf("Pending Claims:        %d%n", stats.getPendingClaims());
        out.printf("Returned Items:        %d (Lost: %d, Found: %d)%n",
                stats.getReturnedItems(), stats.getReturnedLost(), stats.getReturnedFound());
        out.printf("Recovery Rate:         %s%n", stats.getFormattedRecoveryRate());
        out.println("====================================================");
    }

    public void handleExit() {
        persistAllSafe("all data on exit");
        out.println("Saving data and exiting LostLink. Goodbye!");
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public FileStorage getFileStorage() {
        return fileStorage;
    }

    public String getDataDir() {
        return dataDir;
    }
}
