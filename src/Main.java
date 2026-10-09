import java.io.File;

/**
 * Main entry point for LostLink Console Application.
 */
public class Main {
    public static final String DEFAULT_DATA_DIR = "data";

    public static void main(String[] args) {
        String dataDir = resolveDataDir(args);
        printBanner();
        System.out.println("Data directory: " + dataDir);
        System.out.println("LostLink console skeleton initialized. Ready.");
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

    public static void printBanner() {
        System.out.println("====================================================");
        System.out.println("                    LOSTLINK");
        System.out.println("          Intelligent Lost & Found System");
        System.out.println("====================================================");
    }
}
