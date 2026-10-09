package model;

import exception.InvalidClaimException;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents an ownership claim submitted for a cataloged found item.
 */
public class Claim {
    private final String id;
    private final String foundItemId;
    private final String linkedLostItemId;
    private final String claimantName;
    private final String claimantContact;
    private final String identifyingDetails;
    private final LocalDate claimDate;
    private ClaimStatus status;
    private String reviewerName;
    private String reviewNotes;

    public Claim(String id,
                 String foundItemId,
                 String linkedLostItemId,
                 String claimantName,
                 String claimantContact,
                 String identifyingDetails,
                 LocalDate claimDate,
                 ClaimStatus status,
                 String reviewerName,
                 String reviewNotes) throws InvalidClaimException {

        if (id == null || id.trim().isEmpty()) {
            throw new InvalidClaimException("Claim ID cannot be blank.");
        }
        if (foundItemId == null || foundItemId.trim().isEmpty()) {
            throw new InvalidClaimException("Found item ID cannot be blank.");
        }
        if (claimantName == null || claimantName.trim().isEmpty()) {
            throw new InvalidClaimException("Claimant name cannot be blank.");
        }
        if (claimantContact == null || claimantContact.trim().isEmpty()) {
            throw new InvalidClaimException("Claimant contact cannot be blank.");
        }
        if (identifyingDetails == null || identifyingDetails.trim().isEmpty()) {
            throw new InvalidClaimException("Identifying details cannot be blank.");
        }
        if (claimDate == null) {
            throw new InvalidClaimException("Claim date cannot be null.");
        }
        if (claimDate.isAfter(LocalDate.now())) {
            throw new InvalidClaimException("Claim date cannot be in the future: " + claimDate);
        }

        this.id = id.trim();
        this.foundItemId = foundItemId.trim();
        this.linkedLostItemId = (linkedLostItemId != null) ? linkedLostItemId.trim() : "";
        this.claimantName = claimantName.trim();
        this.claimantContact = claimantContact.trim();
        this.identifyingDetails = identifyingDetails.trim();
        this.claimDate = claimDate;
        this.status = (status != null) ? status : ClaimStatus.PENDING;
        this.reviewerName = (reviewerName != null) ? reviewerName.trim() : "";
        this.reviewNotes = (reviewNotes != null) ? reviewNotes.trim() : "";
    }

    public Claim(String id,
                 String foundItemId,
                 String linkedLostItemId,
                 String claimantName,
                 String claimantContact,
                 String identifyingDetails,
                 LocalDate claimDate) throws InvalidClaimException {

        this(id, foundItemId, linkedLostItemId, claimantName, claimantContact,
             identifyingDetails, claimDate, ClaimStatus.PENDING, "", "");
    }

    public String getId() {
        return id;
    }

    public String getFoundItemId() {
        return foundItemId;
    }

    public String getLinkedLostItemId() {
        return linkedLostItemId;
    }

    public String getClaimantName() {
        return claimantName;
    }

    public String getClaimantContact() {
        return claimantContact;
    }

    public String getIdentifyingDetails() {
        return identifyingDetails;
    }

    public LocalDate getClaimDate() {
        return claimDate;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Claim status cannot be null.");
        }
        this.status = status;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = (reviewerName != null) ? reviewerName.trim() : "";
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = (reviewNotes != null) ? reviewNotes.trim() : "";
    }

    public String getSummary() {
        return String.format("[CLAIM] %s on Item %s | By: %s (%s) | Date: %s | Status: %s%s",
                id, foundItemId, claimantName, claimantContact, claimDate, status,
                reviewerName.isEmpty() ? "" : (" | Reviewed by: " + reviewerName));
    }

    @Override
    public String toString() {
        return getSummary();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Claim)) return false;
        Claim claim = (Claim) o;
        return Objects.equals(id, claim.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
