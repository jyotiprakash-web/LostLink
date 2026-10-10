package model;

import java.util.Locale;

/**
 * Immutable snapshot of system statistics for LostLink.
 * Aggregates lost and found counts, unresolved lifecycle states,
 * claim states, returned counts, and recovery percentage.
 */
public class SystemStatistics {
    private final int totalLost;
    private final int totalFound;
    private final int totalItems;
    private final int unresolvedItems;
    private final int pendingClaims;
    private final int totalClaims;
    private final int returnedItems;
    private final int returnedFound;
    private final int returnedLost;
    private final double recoveryRate;

    public SystemStatistics(int totalLost,
                            int totalFound,
                            int totalItems,
                            int unresolvedItems,
                            int pendingClaims,
                            int totalClaims,
                            int returnedItems,
                            int returnedFound,
                            int returnedLost,
                            double recoveryRate) {
        this.totalLost = totalLost;
        this.totalFound = totalFound;
        this.totalItems = totalItems;
        this.unresolvedItems = unresolvedItems;
        this.pendingClaims = pendingClaims;
        this.totalClaims = totalClaims;
        this.returnedItems = returnedItems;
        this.returnedFound = returnedFound;
        this.returnedLost = returnedLost;
        this.recoveryRate = recoveryRate;
    }

    public int getTotalLost() {
        return totalLost;
    }

    public int getTotalFound() {
        return totalFound;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getUnresolvedItems() {
        return unresolvedItems;
    }

    public int getPendingClaims() {
        return pendingClaims;
    }

    public int getTotalClaims() {
        return totalClaims;
    }

    public int getReturnedItems() {
        return returnedItems;
    }

    public int getReturnedFound() {
        return returnedFound;
    }

    public int getReturnedLost() {
        return returnedLost;
    }

    public double getRecoveryRate() {
        return recoveryRate;
    }

    /**
     * Formats recovery rate safely. If no found items are reported, returns
     * a safe string containing both "N/A" and "0.0%" to prevent divide-by-zero.
     */
    public String getFormattedRecoveryRate() {
        if (totalFound == 0) {
            return "N/A (0.0% - no found items registered)";
        }
        return String.format(Locale.US, "%.1f%%", recoveryRate);
    }

    @Override
    public String toString() {
        return "SystemStatistics{" +
                "totalLost=" + totalLost +
                ", totalFound=" + totalFound +
                ", totalItems=" + totalItems +
                ", unresolvedItems=" + unresolvedItems +
                ", pendingClaims=" + pendingClaims +
                ", totalClaims=" + totalClaims +
                ", returnedItems=" + returnedItems +
                ", returnedFound=" + returnedFound +
                ", returnedLost=" + returnedLost +
                ", recoveryRate=" + getFormattedRecoveryRate() +
                '}';
    }
}
