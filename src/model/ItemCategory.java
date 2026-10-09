package model;

/**
 * Standard item categories.
 */
public enum ItemCategory {
    ELECTRONICS,
    BAGS,
    IDENTIFICATION,
    BOOKS,
    CLOTHING,
    ACCESSORIES,
    OTHER;

    public static ItemCategory fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Item category cannot be blank.");
        }
        String normalized = value.trim().toUpperCase();
        for (ItemCategory cat : values()) {
            if (cat.name().equals(normalized)) {
                return cat;
            }
        }
        throw new IllegalArgumentException("Unknown item category: '" + value + "'. Valid values: ELECTRONICS, BAGS, IDENTIFICATION, BOOKS, CLOTHING, ACCESSORIES, OTHER.");
    }

    /**
     * Returns an array of category names suitable for menu choices.
     */
    public static String[] getNames() {
        ItemCategory[] categories = values();
        String[] names = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            names[i] = categories[i].name();
        }
        return names;
    }
}
