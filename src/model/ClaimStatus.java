package model;

/**
 * Lifecycle statuses for ownership claims.
 */
public enum ClaimStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED;

    public static ClaimStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Claim status cannot be blank.");
        }
        String normalized = value.trim().toUpperCase();
        for (ClaimStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown claim status: '" + value + "'. Valid values: PENDING, APPROVED, REJECTED, COMPLETED.");
    }
}
