package com.vladiha.visualpanel.ui;

import com.vladiha.visualpanel.Settings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Drawing helpers and the dark / turquoise palette. */
public final class Ui {
    public static final int BG = 0x0B1117;
    public static final int SURFACE = 0x141C24;
    public static final int SURFACE2 = 0x1E2A36;
    public static final int TEXT = 0xE6EDF3;
    public static final int DIM = 0x8B98A5;

    private Ui() {}

    public static int accent() {
        return Settings.get().accent;
    }

    public static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 255) + ((((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t));
        int g = (int) (((c1 >> 8) & 255) + ((((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t));
        int b = (int) ((c1 & 255) + (((c2 & 255) - (c1 & 255)) * t));
        return (r << 16) | (g << 8) | b;
    }

    public static float easeOut(float t) {
        float u = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - u * u * u;
    }

    /** Rectangle with cut corners (rounded look). */
    public static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static String fit(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String t = text;
        while (t.length() > 1 && font.width(t + "...") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    public static void text(GuiGraphicsExtractor g, Font font, String s, int x, int y, int color) {
        g.text(font, Component.literal(s), x, y, color);
    }

    public static void centered(GuiGraphicsExtractor g, Font font, String s, int cx, int y, int color) {
        g.text(font, Component.literal(s), cx - font.width(s) / 2, y, color);
    }
}
