package model;

/**
 * Lifecycle statuses for lost and found items.
 */
public enum ItemStatus {
    OPEN,
    CLAIM_PENDING,
    RETURNED,
    CLOSED;

    public static ItemStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Item status cannot be blank.");
        }
        String normalized = value.trim().toUpperCase();
        for (ItemStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown item status: '" + value + "'. Valid values: OPEN, CLAIM_PENDING, RETURNED, CLOSED.");
    }
}
