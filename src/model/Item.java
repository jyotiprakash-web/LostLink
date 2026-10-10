package model;

import exception.InvalidItemException;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Abstract base class for all lost and found cataloged items.
 */
public abstract class Item {
    private final String id;
    private final String name;
    private final ItemCategory category;
    private String color;
    private String location;
    private final LocalDate date;
    private String description;
    private ItemStatus status;
    private final User reporter;
    private String contact;

    public Item(String id,
                String name,
                ItemCategory category,
                String color,
                String location,
                LocalDate date,
                String description,
                ItemStatus status,
                User reporter,
                String contact) throws InvalidItemException {

        if (id == null || id.trim().isEmpty()) {
            throw new InvalidItemException("Item ID cannot be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidItemException("Item name cannot be blank.");
        }
        if (category == null) {
            throw new InvalidItemException("Item category cannot be null.");
        }
        if (color == null || color.trim().isEmpty()) {
            throw new InvalidItemException("Item color cannot be blank.");
        }
        if (location == null || location.trim().isEmpty()) {
            throw new InvalidItemException("Item location cannot be blank.");
        }
        if (date == null) {
            throw new InvalidItemException("Item date cannot be null.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidItemException("Item event date cannot be in the future: " + date);
        }
        if (description == null || description.trim().isEmpty()) {
            throw new InvalidItemException("Item description cannot be blank.");
        }
        if (reporter == null) {
            throw new InvalidItemException("Item reporter cannot be null.");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.category = category;
        this.color = color.trim();
        this.location = location.trim();
        this.date = date;
        this.description = description.trim();
        this.status = (status != null) ? status : ItemStatus.OPEN;
        this.reporter = reporter;
        this.contact = (contact != null && !contact.trim().isEmpty()) ? contact.trim() : reporter.getContact();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public String getColor() {
        return color;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Item status cannot be set to null.");
        }
        this.status = status;
    }

    /**
     * Updates editable fields on an item only if it remains OPEN.
     */
    public void updateDetails(String newColor, String newLocation, String newDescription, String newContact)
            throws InvalidItemException {

        if (status != ItemStatus.OPEN) {
            throw new InvalidItemException("Cannot update item " + id + ". Only OPEN items can be edited (current status: " + status + ").");
        }
        if (newColor != null && !newColor.trim().isEmpty()) {
            this.color = newColor.trim();
        }
        if (newLocation != null && !newLocation.trim().isEmpty()) {
            this.location = newLocation.trim();
        }
        if (newDescription != null && !newDescription.trim().isEmpty()) {
            this.description = newDescription.trim();
        }
        if (newContact != null && !newContact.trim().isEmpty()) {
            this.contact = newContact.trim();
        }
    }

    public User getReporter() {
        return reporter;
    }

    public String getContact() {
        return contact;
    }

    /**
     * Polymorphic item type discriminator ("LOST" or "FOUND").
     */
    public abstract String getItemType();

    /**
     * Publicly safe string summary, guaranteed never to expose private identifying details.
     */
    public String getPublicSummary() {
        return String.format("[%s] %s - %s (%s, %s) at %s on %s | Status: %s | Reported by: %s [%s]",
                getItemType(), id, name, category, color, location, date, status,
                reporter.getName(), reporter.displayRole());
    }

    @Override
    public String toString() {
        return getPublicSummary();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
