package model;

import exception.InvalidItemException;
import java.time.LocalDate;

/**
 * Represents a found item turned in by a student or staff member.
 * May include sensitive identifying details kept hidden from public listings.
 */
public class FoundItem extends Item {
    private final String privateDetails;

    public FoundItem(String id,
                     String name,
                     ItemCategory category,
                     String color,
                     String location,
                     LocalDate date,
                     String description,
                     ItemStatus status,
                     User reporter,
                     String contact,
                     String privateDetails) throws InvalidItemException {

        super(id, name, category, color, location, date, description, status, reporter, contact);
        this.privateDetails = (privateDetails != null) ? privateDetails.trim() : "";
    }

    public FoundItem(String id,
                     String name,
                     ItemCategory category,
                     String color,
                     String location,
                     LocalDate date,
                     String description,
                     User reporter) throws InvalidItemException {

        this(id, name, category, color, location, date, description, ItemStatus.OPEN, reporter, (reporter != null ? reporter.getContact() : ""), "");
    }

    /**
     * Returns sensitive identifying details. Accessible only during claim verification.
     */
    public String getPrivateDetails() {
        return privateDetails;
    }

    @Override
    public String getItemType() {
        return "FOUND";
    }

    // Note: getPublicSummary and toString deliberately inherit from Item
    // and do NOT expose privateDetails to public search, browse, or matches.
}
