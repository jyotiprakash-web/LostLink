package service;

import exception.InvalidItemException;
import exception.ItemNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.FoundItem;
import model.Item;
import model.ItemCategory;
import model.ItemStatus;
import model.LostItem;
import model.User;

/**
 * Service managing item records, lifecycle states, ID generation,
 * and maintaining synchronized ArrayList and HashMap collections.
 */
public class ItemService {
    private final List<Item> itemList = new ArrayList<>();
    private final Map<String, Item> itemMap = new HashMap<>();
    private int nextIdSequence = 1;

    public ItemService() {
    }

    /**
     * Generates the next sequential unique ID in format ITM-0001.
     * Skips any IDs already present in the map.
     */
    public synchronized String generateNextId() {
        while (true) {
            String candidate = String.format("ITM-%04d", nextIdSequence++);
            if (!itemMap.containsKey(candidate)) {
                return candidate;
            }
        }
    }

    /**
     * Updates the sequence counter to be strictly higher than loaded IDs.
     */
    public synchronized void syncNextIdSequence(String existingId) {
        if (existingId == null) return;
        if (existingId.startsWith("ITM-")) {
            try {
                int suffix = Integer.parseInt(existingId.substring(4));
                if (suffix >= nextIdSequence) {
                    nextIdSequence = suffix + 1;
                }
            } catch (NumberFormatException ignored) {
                // Non-standard numeric suffix; leave sequence as is
            }
        }
    }

    /**
     * Creates and registers a new LostItem report.
     */
    public synchronized LostItem reportLostItem(String name,
                                                ItemCategory category,
                                                String color,
                                                String location,
                                                LocalDate date,
                                                String description,
                                                User reporter,
                                                String contact,
                                                String lastSeenLocationDetails) throws InvalidItemException {

        String id = generateNextId();
        LostItem item = new LostItem(id, name, category, color, location, date, description,
                ItemStatus.OPEN, reporter, contact, lastSeenLocationDetails);
        addItemInternal(item);
        return item;
    }

    /**
     * Creates and registers a new FoundItem report.
     */
    public synchronized FoundItem reportFoundItem(String name,
                                                  ItemCategory category,
                                                  String color,
                                                  String location,
                                                  LocalDate date,
                                                  String description,
                                                  User reporter,
                                                  String contact,
                                                  String privateDetails) throws InvalidItemException {

        String id = generateNextId();
        FoundItem item = new FoundItem(id, name, category, color, location, date, description,
                ItemStatus.OPEN, reporter, contact, privateDetails);
        addItemInternal(item);
        return item;
    }

    /**
     * Adds an item with pre-existing ID (e.g. from persistence), validating uniqueness.
     */
    public synchronized void addItem(Item item) throws InvalidItemException {
        if (item == null) {
            throw new InvalidItemException("Item cannot be null.");
        }
        if (itemMap.containsKey(item.getId())) {
            throw new InvalidItemException("Duplicate item ID: " + item.getId());
        }
        addItemInternal(item);
        syncNextIdSequence(item.getId());
    }

    private void addItemInternal(Item item) {
        itemList.add(item);
        itemMap.put(item.getId(), item);
    }

    /**
     * Fast lookup by ID via HashMap. Throws ItemNotFoundException if ID is unknown.
     */
    public Item getById(String id) throws ItemNotFoundException {
        if (id == null || id.trim().isEmpty()) {
            throw new ItemNotFoundException("Item ID cannot be blank.");
        }
        Item item = itemMap.get(id.trim());
        if (item == null) {
            throw new ItemNotFoundException("Item not found with ID: " + id.trim());
        }
        return item;
    }

    /**
     * Returns an unmodifiable snapshot of all items. Safe if list is empty.
     */
    public List<Item> getAllItems() {
        return Collections.unmodifiableList(new ArrayList<>(itemList));
    }

    /**
     * Returns an unmodifiable list of all lost items.
     */
    public List<LostItem> getLostItems() {
        List<LostItem> result = new ArrayList<>();
        for (Item item : itemList) {
            if (item instanceof LostItem) {
                result.add((LostItem) item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns an unmodifiable list of all found items.
     */
    public List<FoundItem> getFoundItems() {
        List<FoundItem> result = new ArrayList<>();
        for (Item item : itemList) {
            if (item instanceof FoundItem) {
                result.add((FoundItem) item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public int getItemCount() {
        return itemList.size();
    }

    /**
     * Updates an item's status safely.
     */
    public synchronized void updateItemStatus(String id, ItemStatus newStatus) throws ItemNotFoundException {
        Item item = getById(id);
        item.setStatus(newStatus);
    }

    /**
     * Clears all items and resets sequence.
     */
    public synchronized void clear() {
        itemList.clear();
        itemMap.clear();
        nextIdSequence = 1;
    }
}
