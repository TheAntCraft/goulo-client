package dev.nova.client.module;

public enum Category {
    HUD("HUD"),
    RENDER("Grafik"),
    PERFORMANCE("Performance"),
    UTILITY("Nützliches");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
