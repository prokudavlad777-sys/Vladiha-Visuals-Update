package com.vladiha.visualpanel;

/** HUD elements. Positions are stored as fractions of the GUI size so they survive any resolution. */
public enum HudElement {
    COORDINATES("coordinates", 0.012f, 0.02f),
    HEALTH("health", 0.012f, 0.11f),
    ARMOR("armor", 0.012f, 0.20f),
    BIOME("biome", 0.012f, 0.29f);

    public final String id;
    private final float defaultX;
    private final float defaultY;

    HudElement(String id, float x, float y) {
        this.id = id;
        this.defaultX = x;
        this.defaultY = y;
    }

    public String nameKey() {
        return "hud.element." + id;
    }

    public Settings.ElementSettings defaults() {
        Settings.ElementSettings s = new Settings.ElementSettings();
        s.x = defaultX;
        s.y = defaultY;
        return s;
    }
}
