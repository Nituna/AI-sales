package com.mobilesales.model;

/**
 * Enumeration representing product velocity classifications: 1. Fast Moving 2.
 * Medium Moving 3. Slow Moving 4. Potential Fast Moving (AI signal driven
 * alert)
 */
public enum ProductClassification {
    FAST_MOVING("Fast Moving", "High historical velocity and sustained market momentum", "#10b981", "fas fa-bolt"),
    MEDIUM_MOVING("Medium Moving", "Steady consistent turnover with balanced reorder cycles", "#3b82f6", "fas fa-chart-line"),
    SLOW_MOVING("Slow Moving", "Low turnover velocity; risk of capital lock-in", "#ef4444", "fas fa-hourglass-half"),
    POTENTIAL_FAST_MOVING("Potential Fast Moving", "Potential future demand increase detected. High search visibility & customer engagement signals.", "#f59e0b", "fas fa-fire");

    private final String label;
    private final String description;
    private final String color;
    private final String icon;

    ProductClassification(String label, String description, String color, String icon) {
        this.label = label;
        this.description = description;
        this.color = color;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public static ProductClassification fromString(String val) {
        if (val == null) {
            return MEDIUM_MOVING;
        }
        String clean = val.trim().toUpperCase().replace(" ", "_");
        for (ProductClassification c : values()) {
            if (c.name().equalsIgnoreCase(clean) || c.label.equalsIgnoreCase(val)) {
                return c;
            }
        }
        return MEDIUM_MOVING;
    }
}
