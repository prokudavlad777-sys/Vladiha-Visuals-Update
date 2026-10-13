package com.vladiha.visualpanel;

import com.vladiha.visualpanel.ui.HudEditorScreen;
import com.vladiha.visualpanel.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Draws the HUD elements (health, armor, coordinates, biome). Purely visual, client side. */
public final class HudRenderer {
    private static final int PAD = 4;
    private static final int ACCENT_W = 2;
    private static final int TEXT_H = 9;

    private HudRenderer() {}

    private record Content(String text, float fill) {}

    private static Content content(HudElement element, Minecraft mc) {
        Player p = mc.player;
        switch (element) {
            case COORDINATES:
                return new Content(String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ()), -1f);
            case HEALTH: {
                float hp = p.getHealth();
                float max = Math.max(1f, p.getMaxHealth());
                return new Content(String.format(Locale.ROOT, "%s: %.1f / %.1f", Lang.tr("hud.health"), hp, max),
                    Settings.clamp(hp / max, 0f, 1f));
            }
            case ARMOR: {
                int armor = p.getArmorValue();
                return new Content(Lang.tr("hud.armor") + ": " + armor, Settings.clamp(armor / 20f, 0f, 1f));
            }
            default: {
                String name = mc.level.getBiome(p.blockPosition()).unwrapKey().map(key -> {
                    var id = key.identifier();
                    return Component.translatable("biome." + id.getNamespace() + "." + id.getPath()).getString();
                }).orElse(Lang.tr("hud.unknown"));
                return new Content(Lang.tr("hud.biome") + ": " + name, -1f);
            }
        }
    }

    /** @return {x, y, width, height} in GUI pixels, scaled and clamped to the screen. */
    public static float[] rect(Minecraft mc, HudElement element, Settings.ElementSettings s, int guiW, int guiH) {
        Content c = content(element, mc);
        float w = (mc.font.width(c.text()) + PAD * 2 + ACCENT_W) * s.scale;
        float h = (TEXT_H + PAD * 2 + (c.fill() >= 0 ? 4 : 0)) * s.scale;
        float x = Settings.clamp(s.x * guiW, 0f, Math.max(0f, guiW - w));
        float y = Settings.clamp(s.y * guiH, 0f, Math.max(0f, guiH - h));
        return new float[] {x, y, w, h};
    }

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, HudElement element, Settings.ElementSettings s, boolean editing) {
        Font font = mc.font;
        Content c = content(element, mc);
        float[] r = rect(mc, element, s, g.guiWidth(), g.guiHeight());
        float inv = 1f / s.scale;
        int w = Math.round(r[2] * inv);
        int h = Math.round(r[3] * inv);
        float a = s.opacity * (editing && !s.enabled ? 0.4f : 1f);
        int accent = Settings.get().accent;

        g.pose().pushMatrix();
        g.pose().translate(r[0], r[1]);
        g.pose().scale(s.scale, s.scale);
        g.fill(0, 0, w, h, Ui.argb(a * 0.8f, Ui.BG));
        g.fill(0, 0, ACCENT_W, h, Ui.argb(a, accent));
        g.text(font, Component.literal(c.text()), ACCENT_W + PAD, PAD, Ui.argb(Math.max(a, 0.15f), s.color));
        if (c.fill() >= 0f) {
            int x0 = ACCENT_W + PAD;
            int x1 = w - PAD;
            g.fill(x0, h - 5, x1, h - 3, Ui.argb(a * 0.5f, Ui.SURFACE2));
            g.fill(x0, h - 5, x0 + Math.round((x1 - x0) * c.fill()), h - 3, Ui.argb(a, s.color));
        }
        g.pose().popMatrix();
    }

    public static void renderLayer(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) return;
        Settings settings = Settings.get();
        if (!settings.hudEnabled || mc.gui.screen() instanceof HudEditorScreen) return;
        for (HudElement e : HudElement.values()) {
            Settings.ElementSettings es = settings.element(e);
            if (es.enabled) draw(g, mc, e, es, false);
        }
    }
}
