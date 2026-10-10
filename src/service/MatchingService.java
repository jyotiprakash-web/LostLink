package service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.FoundItem;
import model.ItemStatus;
import model.LostItem;
import model.MatchResult;

/**
 * Service for calculating explainable heuristic similarity scores and ranking candidates.
 * Note: Scores reflect heuristic attribute similarity, NEVER probability of ownership.
 */
public class MatchingService {
    public static final int MATCH_DATE_WINDOW_DAYS = 14;
    public static final int MIN_MATCH_SCORE = 30;

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "and", "or", "in", "on", "at", "to", "for", "of", "with",
            "is", "it", "my", "this", "that", "was", "has", "have", "by", "from"
    ));

    /**
     * Compares a lost item and a found item, computing an explainable score out of 100.
     */
    public MatchResult compare(LostItem lost, FoundItem found) {
        if (lost == null || found == null) {
            throw new IllegalArgumentException("Items compared in MatchingService cannot be null.");
        }

        List<String> reasons = new ArrayList<>();
        int totalScore = 0;

        // 1. Category match (max 30)
        if (lost.getCategory() == found.getCategory()) {
            totalScore += 30;
            reasons.add("Category matched (+30)");
        }

        // 2. Keyword overlap (max 25)
        Set<String> lostTokens = extractKeywords(lost.getName() + " " + lost.getDescription());
        Set<String> foundTokens = extractKeywords(found.getName() + " " + found.getDescription());

        if (!lostTokens.isEmpty() && !foundTokens.isEmpty()) {
            int smallerSize = Math.min(lostTokens.size(), foundTokens.size());
            int sharedCount = 0;
            for (String token : lostTokens) {
                if (foundTokens.contains(token)) {
                    sharedCount++;
                }
            }
            if (sharedCount > 0 && smallerSize > 0) {
                int keywordPoints = (int) Math.round(25.0 * sharedCount / smallerSize);
                keywordPoints = Math.min(25, Math.max(1, keywordPoints));
                totalScore += keywordPoints;
                reasons.add("Keyword overlap (" + sharedCount + " shared keywords) (+" + keywordPoints + ")");
            }
        }

        // 3. Color match (max 15)
        String lostColor = lost.getColor().trim().toLowerCase();
        String foundColor = found.getColor().trim().toLowerCase();
        if (lostColor.equals(foundColor)) {
            totalScore += 15;
            reasons.add("Color matched (+15)");
        } else if (lostColor.contains(foundColor) || foundColor.contains(lostColor)) {
            totalScore += 10;
            reasons.add("Partial color match (+10)");
        }

        // 4. Location match / proximity (max 20)
        String lostLoc = lost.getLocation().trim().toLowerCase();
        String foundLoc = found.getLocation().trim().toLowerCase();
        if (lostLoc.equals(foundLoc)) {
            totalScore += 20;
            reasons.add("Location matched (+20)");
        } else if (lostLoc.contains(foundLoc) || foundLoc.contains(lostLoc)) {
            totalScore += 15;
            reasons.add("Partial location match (+15)");
        } else {
            Set<String> lostLocTokens = extractKeywords(lostLoc);
            Set<String> foundLocTokens = extractKeywords(foundLoc);
            if (!lostLocTokens.isEmpty() && !foundLocTokens.isEmpty()) {
                int minLocSize = Math.min(lostLocTokens.size(), foundLocTokens.size());
                int sharedLoc = 0;
                for (String t : lostLocTokens) {
                    if (foundLocTokens.contains(t)) {
                        sharedLoc++;
                    }
                }
                if (sharedLoc > 0 && minLocSize > 0) {
                    int locPoints = (int) Math.round(20.0 * sharedLoc / minLocSize);
                    locPoints = Math.min(20, Math.max(1, locPoints));
                    totalScore += locPoints;
                    reasons.add("Location proximity (+" + locPoints + ")");
                }
            }
        }

        // 5. Date proximity (max 10)
        long diffDays = Math.abs(ChronoUnit.DAYS.between(lost.getDate(), found.getDate()));
        if (diffDays < MATCH_DATE_WINDOW_DAYS) {
            int datePoints = (int) Math.round(10.0 * (MATCH_DATE_WINDOW_DAYS - diffDays) / MATCH_DATE_WINDOW_DAYS);
            datePoints = Math.min(10, Math.max(1, datePoints));
            totalScore += datePoints;
            reasons.add("Date proximity (" + diffDays + " days apart) (+" + datePoints + ")");
        }

        totalScore = Math.min(100, Math.max(0, totalScore));
        return new MatchResult(lost, found, totalScore, reasons);
    }

    public static final java.util.Comparator<MatchResult> MATCH_COMPARATOR = (a, b) -> {
        int scoreCmp = Integer.compare(b.getScore(), a.getScore());
        if (scoreCmp != 0) return scoreCmp;
        int dateCmp = b.getFoundItem().getDate().compareTo(a.getFoundItem().getDate());
        if (dateCmp != 0) return dateCmp;
        return a.getFoundItem().getId().compareTo(b.getFoundItem().getId());
    };

    /**
     * Finds, ranks, and returns a bounded MatchResult[] array (max 3) of top matches.
     * Enforces eligibility (OPEN for lost, OPEN or CLAIM_PENDING for found) and MIN_MATCH_SCORE.
     * Traversed with a loop.
     */
    public MatchResult[] findTopMatches(LostItem lost, List<FoundItem> candidateFoundItems) {
        if (lost == null || !isLostItemEligible(lost) || candidateFoundItems == null || candidateFoundItems.isEmpty()) {
            return new MatchResult[0];
        }

        List<MatchResult> eligibleResults = new ArrayList<>();
        for (FoundItem found : candidateFoundItems) {
            if (isFoundItemEligible(found)) {
                MatchResult result = compare(lost, found);
                if (result.getScore() >= MIN_MATCH_SCORE) {
                    eligibleResults.add(result);
                }
            }
        }

        eligibleResults.sort(MATCH_COMPARATOR);

        int count = Math.min(3, eligibleResults.size());
        MatchResult[] topMatches = new MatchResult[count];
        for (int i = 0; i < count; i++) {
            topMatches[i] = eligibleResults.get(i);
        }
        return topMatches;
    }

    /**
     * Checks if a found item is eligible for matching (OPEN or CLAIM_PENDING).
     */
    public boolean isFoundItemEligible(FoundItem item) {
        if (item == null) return false;
        ItemStatus s = item.getStatus();
        return s == ItemStatus.OPEN || s == ItemStatus.CLAIM_PENDING;
    }

    /**
     * Checks if a lost item is eligible for matching (OPEN).
     */
    public boolean isLostItemEligible(LostItem item) {
        if (item == null) return false;
        return item.getStatus() == ItemStatus.OPEN;
    }

    /**
     * Extracts normalized, punctuation-stripped keywords without stop words.
     */
    public static Set<String> extractKeywords(String text) {
        if (text == null) {
            return Collections.emptySet();
        }
        String clean = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] rawTokens = clean.split("\\s+");
        Set<String> result = new HashSet<>();
        for (String token : rawTokens) {
            String trimmed = token.trim();
            if (trimmed.length() > 1 && !STOP_WORDS.contains(trimmed)) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
