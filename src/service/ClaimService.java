package service;

import exception.InvalidClaimException;
import exception.ItemNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Claim;
import model.ClaimStatus;
import model.FoundItem;
import model.Item;
import model.ItemStatus;
import model.LostItem;

/**
 * Service managing ownership claims and enforcing claim and item lifecycle state transitions.
 */
public class ClaimService {
    private final ItemService itemService;
    private final List<Claim> claimList = new ArrayList<>();
    private final Map<String, Claim> claimMap = new HashMap<>();
    private int nextClaimSequence = 1;

    public ClaimService(ItemService itemService) {
        if (itemService == null) {
            throw new IllegalArgumentException("ItemService cannot be null in ClaimService.");
        }
        this.itemService = itemService;
    }

    /**
     * Generates unique claim ID formatted as CLM-0001.
     */
    public synchronized String generateNextClaimId() {
        while (true) {
            String candidate = String.format("CLM-%04d", nextClaimSequence++);
            if (!claimMap.containsKey(candidate)) {
                return candidate;
            }
        }
    }

    /**
     * Synchronizes the sequence counter with existing loaded claim IDs.
     */
    public synchronized void syncNextClaimSequence(String existingId) {
        if (existingId == null) return;
        if (existingId.startsWith("CLM-")) {
            try {
                int suffix = Integer.parseInt(existingId.substring(4));
                if (suffix >= nextClaimSequence) {
                    nextClaimSequence = suffix + 1;
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /**
     * Submits a new ownership claim for an eligible found item.
     * Enforces that the item is a FoundItem with status OPEN or CLAIM_PENDING.
     * Upon submission, the claim is stored as PENDING and the found item moves to CLAIM_PENDING.
     */
    public synchronized Claim submitClaim(String foundItemId,
                                          String linkedLostItemId,
                                          String claimantName,
                                          String claimantContact,
                                          String identifyingDetails,
                                          LocalDate claimDate)
            throws ItemNotFoundException, InvalidClaimException {

        if (foundItemId == null || foundItemId.trim().isEmpty()) {
            throw new InvalidClaimException("Found item ID cannot be blank.");
        }

        Item item = itemService.getById(foundItemId.trim());
        if (!(item instanceof FoundItem)) {
            throw new InvalidClaimException("Claims can only be filed against found items. Item "
                    + foundItemId + " is a " + item.getItemType() + " report.");
        }
        FoundItem foundItem = (FoundItem) item;

        if (foundItem.getStatus() == ItemStatus.RETURNED || foundItem.getStatus() == ItemStatus.CLOSED) {
            throw new InvalidClaimException("Cannot submit a claim for an item with status " + foundItem.getStatus() + ".");
        }

        if (linkedLostItemId != null && !linkedLostItemId.trim().isEmpty()) {
            Item lostItem = itemService.getById(linkedLostItemId.trim());
            if (!(lostItem instanceof LostItem)) {
                throw new InvalidClaimException("Linked item " + linkedLostItemId + " is not a lost item report.");
            }
        }

        String claimId = generateNextClaimId();
        Claim claim = new Claim(claimId, foundItem.getId(), linkedLostItemId, claimantName,
                claimantContact, identifyingDetails, claimDate);

        claimList.add(claim);
        claimMap.put(claim.getId(), claim);

        // Found item status transitions to CLAIM_PENDING
        foundItem.setStatus(ItemStatus.CLAIM_PENDING);

        return claim;
    }

    /**
     * Registers a pre-existing claim (e.g. loaded from persistence).
     */
    public synchronized void addClaim(Claim claim) throws InvalidClaimException {
        if (claim == null) {
            throw new InvalidClaimException("Claim cannot be null.");
        }
        if (claimMap.containsKey(claim.getId())) {
            throw new InvalidClaimException("Duplicate claim ID: " + claim.getId());
        }
        claimList.add(claim);
        claimMap.put(claim.getId(), claim);
        syncNextClaimSequence(claim.getId());
    }

    /**
     * Retrieves a claim by ID, throwing InvalidClaimException if not found.
     */
    public Claim getClaimById(String claimId) throws InvalidClaimException {
        if (claimId == null || claimId.trim().isEmpty()) {
            throw new InvalidClaimException("Claim ID cannot be blank.");
        }
        Claim claim = claimMap.get(claimId.trim());
        if (claim == null) {
            throw new InvalidClaimException("Claim not found with ID: " + claimId.trim());
        }
        return claim;
    }

    /**
     * Records "More information needed" notes, keeping the claim in PENDING status.
     */
    public synchronized void requestMoreInformation(String claimId, String reviewerName, String notes)
            throws InvalidClaimException {

        Claim claim = getClaimById(claimId);
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidClaimException("Can only request more information on PENDING claims. Current status: " + claim.getStatus());
        }
        claim.setReviewerName(reviewerName);
        claim.setReviewNotes(notes);
    }

    /**
     * Approves a PENDING claim. Enforces that only one claim per item may be APPROVED at a time.
     */
    public synchronized void approveClaim(String claimId, String reviewerName, String notes)
            throws InvalidClaimException, ItemNotFoundException {

        Claim claim = getClaimById(claimId);
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidClaimException("Only PENDING claims can be approved. Current status: " + claim.getStatus());
        }

        for (Claim other : claimList) {
            if (!other.getId().equals(claim.getId()) &&
                    other.getFoundItemId().equalsIgnoreCase(claim.getFoundItemId()) &&
                    other.getStatus() == ClaimStatus.APPROVED) {
                throw new InvalidClaimException("Cannot approve claim " + claimId + ". Another claim ("
                        + other.getId() + ") for item " + claim.getFoundItemId() + " is already APPROVED.");
            }
        }

        claim.setStatus(ClaimStatus.APPROVED);
        claim.setReviewerName(reviewerName);
        claim.setReviewNotes(notes);

        Item item = itemService.getById(claim.getFoundItemId());
        item.setStatus(ItemStatus.CLAIM_PENDING);
    }

    /**
     * Rejects a PENDING claim. Reverts the found item to OPEN unless another PENDING or APPROVED claim exists.
     */
    public synchronized void rejectClaim(String claimId, String reviewerName, String notes)
            throws InvalidClaimException, ItemNotFoundException {

        Claim claim = getClaimById(claimId);
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidClaimException("Only PENDING claims can be rejected. Current status: " + claim.getStatus());
        }

        claim.setStatus(ClaimStatus.REJECTED);
        claim.setReviewerName(reviewerName);
        claim.setReviewNotes(notes);

        boolean hasOtherActive = false;
        for (Claim other : claimList) {
            if (!other.getId().equals(claim.getId()) &&
                    other.getFoundItemId().equalsIgnoreCase(claim.getFoundItemId())) {
                if (other.getStatus() == ClaimStatus.PENDING || other.getStatus() == ClaimStatus.APPROVED) {
                    hasOtherActive = true;
                    break;
                }
            }
        }

        Item item = itemService.getById(claim.getFoundItemId());
        if (!hasOtherActive) {
            item.setStatus(ItemStatus.OPEN);
        } else {
            item.setStatus(ItemStatus.CLAIM_PENDING);
        }
    }

    public List<Claim> getAllClaims() {
        return Collections.unmodifiableList(new ArrayList<>(claimList));
    }

    public List<Claim> getClaimsForFoundItem(String foundItemId) {
        if (foundItemId == null) return Collections.emptyList();
        List<Claim> result = new ArrayList<>();
        for (Claim c : claimList) {
            if (c.getFoundItemId().equalsIgnoreCase(foundItemId.trim())) {
                result.add(c);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public int getClaimCount() {
        return claimList.size();
    }

    public synchronized void clear() {
        claimList.clear();
        claimMap.clear();
        nextClaimSequence = 1;
    }
}
