package com.mobilesales.model;

/**
 * Enumeration representing Inventory operational states.
 */
public enum InventoryStatus {
    IN_STOCK("In Stock", "#10b981"),
    LOW_STOCK("Low Stock", "#f59e0b"),
    REORDER_REQUIRED("Reorder Required", "#ef4444"),
    OVERSTOCK("Overstock", "#8b5cf6");

    private final String displayName;
    private final String colorHex;

    InventoryStatus(String displayName, String colorHex) {
        this.displayName = displayName;
        this.colorHex = colorHex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public static InventoryStatus fromString(String status) {
        if (status == null) return IN_STOCK;
        String clean = status.trim().toUpperCase().replace(" ", "_");
        for (InventoryStatus s : values()) {
            if (s.name().equalsIgnoreCase(clean) || s.displayName.equalsIgnoreCase(status)) {
                return s;
            }
        }
        return IN_STOCK;
    }
}
