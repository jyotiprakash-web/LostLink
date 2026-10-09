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
            case 2:
            case 3:
            case 4:
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
