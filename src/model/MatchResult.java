package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the explainable similarity score between a lost item and a found item candidate.
 * Note: A match score indicates heuristic similarity, NEVER a probability of ownership.
 */
public class MatchResult {
    private final LostItem lostItem;
    private final FoundItem foundItem;
    private final int score;
    private final List<String> reasons;

    public MatchResult(LostItem lostItem, FoundItem foundItem, int score, List<String> reasons) {
        if (lostItem == null) {
            throw new IllegalArgumentException("Lost item cannot be null in MatchResult.");
        }
        if (foundItem == null) {
            throw new IllegalArgumentException("Found item cannot be null in MatchResult.");
        }
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Match score must be between 0 and 100, inclusive. Got: " + score);
        }

        this.lostItem = lostItem;
        this.foundItem = foundItem;
        this.score = score;
        this.reasons = (reasons != null) ? new ArrayList<>(reasons) : new ArrayList<>();
    }

    public LostItem getLostItem() {
        return lostItem;
    }

    public FoundItem getFoundItem() {
        return foundItem;
    }

    public int getScore() {
        return score;
    }

    /**
     * Returns an unmodifiable view of the match explanation reasons.
     */
    public List<String> getReasons() {
        return Collections.unmodifiableList(reasons);
    }

    public String getExplanation() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Match Score: %d/100 (Similarity only, not proof of ownership)%n", score));
        sb.append("Candidate Found Item: ").append(foundItem.getId()).append(" - ").append(foundItem.getName()).append("\n");
        sb.append("Reasons breakdown:\n");
        if (reasons.isEmpty()) {
            sb.append("  - No positive criteria matched.\n");
        } else {
            for (String reason : reasons) {
                sb.append("  - ").append(reason).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("MatchResult[Lost=%s, Found=%s, Score=%d/100]",
                lostItem.getId(), foundItem.getId(), score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchResult)) return false;
        MatchResult that = (MatchResult) o;
        return score == that.score &&
                Objects.equals(lostItem, that.lostItem) &&
                Objects.equals(foundItem, that.foundItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lostItem, foundItem, score);
    }
}
