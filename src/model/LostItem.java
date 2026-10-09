package model;

import exception.InvalidItemException;
import java.time.LocalDate;

/**
 * Represents a lost item reported by a student or staff member.
 */
public class LostItem extends Item {
    private final String lastSeenLocationDetails;

    public LostItem(String id,
                    String name,
                    ItemCategory category,
                    String color,
                    String location,
                    LocalDate date,
                    String description,
                    ItemStatus status,
                    User reporter,
                    String contact,
                    String lastSeenLocationDetails) throws InvalidItemException {

        super(id, name, category, color, location, date, description, status, reporter, contact);
        this.lastSeenLocationDetails = (lastSeenLocationDetails != null) ? lastSeenLocationDetails.trim() : "";
    }

    public LostItem(String id,
                    String name,
                    ItemCategory category,
                    String color,
                    String location,
                    LocalDate date,
                    String description,
                    User reporter) throws InvalidItemException {

        this(id, name, category, color, location, date, description, ItemStatus.OPEN, reporter, (reporter != null ? reporter.getContact() : ""), "");
    }

    public String getLastSeenLocationDetails() {
        return lastSeenLocationDetails;
    }

    @Override
    public String getItemType() {
        return "LOST";
    }

    @Override
    public String getPublicSummary() {
        String base = super.getPublicSummary();
        if (!lastSeenLocationDetails.isEmpty()) {
            return base + " | Last seen details: " + lastSeenLocationDetails;
        }
        return base;
    }
}
