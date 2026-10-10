package service;

import exception.InvalidClaimException;
import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import model.Claim;
import model.ClaimStatus;
import model.FoundItem;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.Staff;
import model.Student;
import model.User;
import util.CsvCodec;

/**
 * Handles persistent storage of items and claims using RFC 4180 CSV files.
 * Provides resilient loading, bad-row rejection tracking, and atomic saves.
 */
public class FileStorage {

    public static final String ITEMS_HEADER =
            "id,type,name,category,color,location,date,description,status,reporterType,reporterName,reporterContact,extraDetails";

    public static final String CLAIMS_HEADER =
            "claimId,foundItemId,linkedLostItemId,claimantName,claimantContact,identifyingDetails,claimDate,status,reviewerName,reviewNotes";

    private final Path dataDirPath;
    private final Path itemsCsvPath;
    private final Path claimsCsvPath;
    private final Path itemsRejectedPath;
    private final Path claimsRejectedPath;

    public FileStorage(String dataDir) {
        String dir = (dataDir != null && !dataDir.trim().isEmpty()) ? dataDir.trim() : "data";
        this.dataDirPath = Paths.get(dir);
        this.itemsCsvPath = dataDirPath.resolve("items.csv");
        this.claimsCsvPath = dataDirPath.resolve("claims.csv");
        this.itemsRejectedPath = dataDirPath.resolve("items.rejected");
        this.claimsRejectedPath = dataDirPath.resolve("claims.rejected");
    }

    public Path getDataDirPath() {
        return dataDirPath;
    }

    public Path getItemsCsvPath() {
        return itemsCsvPath;
    }

    public Path getClaimsCsvPath() {
        return claimsCsvPath;
    }

    public Path getItemsRejectedPath() {
        return itemsRejectedPath;
    }

    public Path getClaimsRejectedPath() {
        return claimsRejectedPath;
    }

    /**
     * Resiliently loads all items from items.csv into ItemService.
     * Skips header row. Corrupted/malformed rows are logged and saved to items.rejected.
     * Returns count of successfully loaded items.
     */
    public synchronized int loadItems(ItemService itemService) throws IOException {
        if (itemService == null) {
            throw new IllegalArgumentException("ItemService cannot be null.");
        }
        if (!Files.exists(itemsCsvPath)) {
            return 0;
        }

        int loadedCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(itemsCsvPath, StandardCharsets.UTF_8)) {
            List<String> record;
            boolean isFirstRow = true;

            while ((record = CsvCodec.readNextRecord(reader)) != null) {
                if (record.isEmpty() || (record.size() == 1 && record.get(0).trim().isEmpty())) {
                    continue; // Skip blank lines
                }

                // Check and skip header
                if (isFirstRow) {
                    isFirstRow = false;
                    if ("id".equalsIgnoreCase(record.get(0).trim()) ||
                            (record.size() > 1 && "type".equalsIgnoreCase(record.get(1).trim()))) {
                        continue;
                    }
                }

                try {
                    Item item = parseItemRecord(record);
                    itemService.addItem(item);
                    loadedCount++;
                } catch (Exception e) {
                    System.err.println("[Warning] Skipping malformed item record in " + itemsCsvPath + ": " + e.getMessage());
                    recordRejection(itemsRejectedPath, record, e.getMessage());
                }
            }
        }

        return loadedCount;
    }

    /**
     * Resiliently loads all claims from claims.csv into ClaimService.
     * Enforces that referenced foundItemId exists in ItemService.
     * Skips header row. Corrupted/malformed rows are logged and saved to claims.rejected.
     * Returns count of successfully loaded claims.
     */
    public synchronized int loadClaims(ClaimService claimService, ItemService itemService) throws IOException {
        if (claimService == null || itemService == null) {
            throw new IllegalArgumentException("ClaimService and ItemService cannot be null.");
        }
        if (!Files.exists(claimsCsvPath)) {
            return 0;
        }

        int loadedCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(claimsCsvPath, StandardCharsets.UTF_8)) {
            List<String> record;
            boolean isFirstRow = true;

            while ((record = CsvCodec.readNextRecord(reader)) != null) {
                if (record.isEmpty() || (record.size() == 1 && record.get(0).trim().isEmpty())) {
                    continue;
                }

                if (isFirstRow) {
                    isFirstRow = false;
                    if ("claimid".equalsIgnoreCase(record.get(0).trim()) ||
                            "id".equalsIgnoreCase(record.get(0).trim()) ||
                            (record.size() > 1 && "founditemid".equalsIgnoreCase(record.get(1).trim()))) {
                        continue;
                    }
                }

                try {
                    Claim claim = parseClaimRecord(record, itemService);
                    claimService.addClaim(claim);
                    loadedCount++;
                } catch (Exception e) {
                    System.err.println("[Warning] Skipping malformed claim record in " + claimsCsvPath + ": " + e.getMessage());
                    recordRejection(claimsRejectedPath, record, e.getMessage());
                }
            }
        }

        return loadedCount;
    }

    /**
     * Loads both items and claims, ensuring items are loaded first.
     */
    public synchronized void loadAll(ItemService itemService, ClaimService claimService) throws IOException {
        loadItems(itemService);
        loadClaims(claimService, itemService);
    }

    /**
     * Atomically saves all items from ItemService to items.csv using a temp file.
     */
    public synchronized void saveItems(ItemService itemService) throws IOException {
        if (itemService == null) {
            throw new IllegalArgumentException("ItemService cannot be null.");
        }
        ensureDataDir();

        Path tempFile = Files.createTempFile(dataDirPath, "items_", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write(ITEMS_HEADER);
                writer.newLine();
                for (Item item : itemService.getAllItems()) {
                    writer.write(serializeItem(item));
                    writer.newLine();
                }
            }

            moveAtomically(tempFile, itemsCsvPath);
        } finally {
            if (Files.exists(tempFile)) {
                try {
                    Files.delete(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Atomically saves all claims from ClaimService to claims.csv using a temp file.
     */
    public synchronized void saveClaims(ClaimService claimService) throws IOException {
        if (claimService == null) {
            throw new IllegalArgumentException("ClaimService cannot be null.");
        }
        ensureDataDir();

        Path tempFile = Files.createTempFile(dataDirPath, "claims_", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                writer.write(CLAIMS_HEADER);
                writer.newLine();
                for (Claim claim : claimService.getAllClaims()) {
                    writer.write(serializeClaim(claim));
                    writer.newLine();
                }
            }

            moveAtomically(tempFile, claimsCsvPath);
        } finally {
            if (Files.exists(tempFile)) {
                try {
                    Files.delete(tempFile);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Atomically saves both items and claims.
     */
    public synchronized void saveAll(ItemService itemService, ClaimService claimService) throws IOException {
        saveItems(itemService);
        saveClaims(claimService);
    }

    // --- Parsing & Serialization Helpers ---

    private Item parseItemRecord(List<String> record)
            throws InvalidItemException, DateTimeParseException {

        if (record.size() < 12) {
            throw new InvalidItemException("Expected at least 12 fields, found " + record.size());
        }

        String id = record.get(0).trim();
        String typeStr = record.get(1).trim();
        String name = record.get(2).trim();
        ItemCategory category = ItemCategory.fromString(record.get(3).trim());
        String color = record.get(4).trim();
        String location = record.get(5).trim();
        LocalDate date = LocalDate.parse(record.get(6).trim());
        String description = record.get(7).trim();

        ItemStatus status;
        try {
            status = ItemStatus.valueOf(record.get(8).trim().toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            throw new InvalidItemException("Invalid item status: " + record.get(8));
        }

        String reporterType = record.get(9).trim();
        String reporterName = record.get(10).trim();
        String reporterContact = record.get(11).trim();
        String extraDetails = (record.size() > 12) ? record.get(12) : "";

        User reporter;
        if ("Staff".equalsIgnoreCase(reporterType)) {
            reporter = new Staff(reporterName, reporterContact);
        } else {
            reporter = new Student(reporterName, reporterContact);
        }

        if ("LOST".equalsIgnoreCase(typeStr)) {
            return new LostItem(id, name, category, color, location, date, description,
                    status, reporter, reporterContact, extraDetails);
        } else if ("FOUND".equalsIgnoreCase(typeStr)) {
            return new FoundItem(id, name, category, color, location, date, description,
                    status, reporter, reporterContact, extraDetails);
        } else {
            throw new InvalidItemException("Unknown item type: " + typeStr);
        }
    }

    private Claim parseClaimRecord(List<String> record, ItemService itemService)
            throws InvalidClaimException, ItemNotFoundException, DateTimeParseException {

        if (record.size() < 8) {
            throw new InvalidClaimException("Expected at least 8 fields, found " + record.size());
        }

        String claimId = record.get(0).trim();
        String foundItemId = record.get(1).trim();
        String linkedLostItemId = record.get(2).trim();
        String claimantName = record.get(3).trim();
        String claimantContact = record.get(4).trim();
        String identifyingDetails = record.get(5).trim();
        LocalDate claimDate = LocalDate.parse(record.get(6).trim());

        ClaimStatus status;
        try {
            status = ClaimStatus.valueOf(record.get(7).trim().toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            throw new InvalidClaimException("Invalid claim status: " + record.get(7));
        }

        String reviewerName = (record.size() > 8) ? record.get(8).trim() : "";
        String reviewNotes = (record.size() > 9) ? record.get(9).trim() : "";

        // Verify found item exists
        itemService.getById(foundItemId);

        return new Claim(claimId, foundItemId, linkedLostItemId, claimantName,
                claimantContact, identifyingDetails, claimDate, status, reviewerName, reviewNotes);
    }

    private String serializeItem(Item item) {
        List<String> fields = new ArrayList<>(13);
        fields.add(item.getId());
        fields.add(item.getItemType().toUpperCase(Locale.US));
        fields.add(item.getName());
        fields.add(item.getCategory().name());
        fields.add(item.getColor());
        fields.add(item.getLocation());
        fields.add(item.getDate().toString());
        fields.add(item.getDescription());
        fields.add(item.getStatus().name());

        User reporter = item.getReporter();
        if (reporter instanceof Staff) {
            fields.add("Staff");
        } else {
            fields.add("Student");
        }
        fields.add(reporter != null ? reporter.getName() : "");
        fields.add(item.getContact());

        if (item instanceof LostItem) {
            fields.add(((LostItem) item).getLastSeenLocationDetails());
        } else if (item instanceof FoundItem) {
            fields.add(((FoundItem) item).getPrivateDetails());
        } else {
            fields.add("");
        }

        return CsvCodec.formatRow(fields);
    }

    private String serializeClaim(Claim claim) {
        List<String> fields = new ArrayList<>(10);
        fields.add(claim.getId());
        fields.add(claim.getFoundItemId());
        fields.add(claim.getLinkedLostItemId());
        fields.add(claim.getClaimantName());
        fields.add(claim.getClaimantContact());
        fields.add(claim.getIdentifyingDetails());
        fields.add(claim.getClaimDate().toString());
        fields.add(claim.getStatus().name());
        fields.add(claim.getReviewerName());
        fields.add(claim.getReviewNotes());

        return CsvCodec.formatRow(fields);
    }

    private void ensureDataDir() throws IOException {
        if (!Files.exists(dataDirPath)) {
            Files.createDirectories(dataDirPath);
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void recordRejection(Path rejectedFile, List<String> record, String reason) {
        try {
            ensureDataDir();
            try (BufferedWriter bw = Files.newBufferedWriter(rejectedFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                bw.write(CsvCodec.formatRow(record) + " | REJECTED: " + reason);
                bw.newLine();
            }
        } catch (IOException ignored) {
        }
    }
}
