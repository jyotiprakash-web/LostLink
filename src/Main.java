import java.io.PrintStream;
import service.ItemService;
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
    private final InputHelper inputHelper;
    private final PrintStream out;
    private final String dataDir;
    private boolean running = true;

    public Main(ItemService itemService, InputHelper inputHelper, PrintStream out, String dataDir) {
        this.itemService = (itemService != null) ? itemService : new ItemService();
        this.inputHelper = (inputHelper != null) ? inputHelper : new InputHelper();
        this.out = (out != null) ? out : System.out;
        this.dataDir = (dataDir != null) ? dataDir : DEFAULT_DATA_DIR;
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
                out.println("[Coming Soon] " + MENU_LABELS[choice - 1] + " is not yet implemented.");
                break;
            case 4:
                handleSearchRecords();
                break;
            case 5:
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
