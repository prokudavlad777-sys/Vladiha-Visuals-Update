package com.vladiha.visualpanel.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.vladiha.visualpanel.HudElement;
import com.vladiha.visualpanel.Lang;
import com.vladiha.visualpanel.Settings;
import com.vladiha.visualpanel.ui.Widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Main panel (opened with V): Visuals / HUD / Settings tabs. */
public final class PanelScreen extends Screen {
    private enum Tab {
        VISUALS("tab.visuals"), HUD("tab.hud"), SETTINGS("tab.settings");
        final String key;
        Tab(String key) { this.key = key; }
    }

    private static final int ROW = 26;
    private static Tab lastTab = Tab.VISUALS;

    private Tab tab = lastTab;
    private final List<Widget> widgets = new ArrayList<>();
    private Widget pressed;
    private Button closeButton;
    private final long openedAt = System.nanoTime();

    private int wx, wy, ww, wh, viewTop, viewBottom, cx, cw;
    private int scroll, maxScroll, curCol, curRowY, cols;
    private float indicatorX = -1f;

    public PanelScreen() {
        super(Component.literal("Visual Panel"));
    }

    @Override
    protected void init() {
        ww = Math.min(440, width - 16);
        wh = Math.min(260, height - 16);
        wx = (width - ww) / 2;
        wy = (height - wh) / 2;
        viewTop = wy + 52;
        viewBottom = wy + wh - 20;
        cx = wx + 12;
        cw = ww - 24;
        cols = ww >= 330 ? 2 : 1;
        closeButton = new Button(() -> "x", this::onClose);
        closeButton.x = wx + ww - 24;
        closeButton.y = wy + 7;
        closeButton.w = 16;
        closeButton.h = 14;
        rebuild();
    }

    private void add(Widget w, boolean full) {
        int gap = 8;
        int colW = (cw - gap) / 2;
        if (full || cols == 1) {
            if (curCol != 0) { curRowY += ROW; curCol = 0; }
            w.x = cx;
            w.w = cw;
            w.y = viewTop + curRowY - scroll;
            curRowY += ROW;
        } else {
            w.x = cx + curCol * (colW + gap);
            w.w = colW;
            w.y = viewTop + curRowY - scroll;
            if (++curCol == 2) { curCol = 0; curRowY += ROW; }
        }
        w.h = ROW - 4;
        widgets.add(w);
    }

    private void rebuild() {
        rebuild(true);
    }

    private void rebuild(boolean retry) {
        widgets.clear();
        pressed = null;
        curCol = 0;
        curRowY = 0;
        Settings s = Settings.get();
        Options o = Minecraft.getInstance().options;

        switch (tab) {
            case VISUALS -> {
                add(new Header(() -> Lang.tr("section.global")), true);
                add(new Slider(() -> Lang.tr("visuals.fov"), 30, 110, 1,
                    () -> o.fov().get(), v -> o.fov().set((int) Math.round(v)), v -> String.valueOf(Math.round(v))), false);
                add(new Slider(() -> Lang.tr("visuals.brightness"), 0, 1, 0.01,
                    () -> o.gamma().get(), v -> o.gamma().set(v), v -> Math.round(v * 100) + "%"), false);
                add(new Slider(() -> Lang.tr("visuals.render"), 2, 32, 1,
                    () -> o.renderDistance().get(), v -> o.renderDistance().set((int) Math.round(v)), v -> String.valueOf(Math.round(v))), false);
                add(new Button(() -> Lang.tr("visuals.reset"), () -> {
                    o.fov().set(70);
                    o.gamma().set(0.5);
                    o.renderDistance().set(12);
                }), false);
            }
            case HUD -> {
                add(new Header(() -> Lang.tr("tab.hud")), true);
                add(new Toggle(() -> Lang.tr("hud.show"), () -> s.hudEnabled, v -> { s.hudEnabled = v; Settings.save(); }), true);
                for (HudElement e : HudElement.values()) {
                    Settings.ElementSettings es = s.element(e);
                    add(new Toggle(() -> Lang.tr(e.nameKey()), () -> es.enabled, v -> { es.enabled = v; Settings.save(); }), false);
                }
                add(new Button(() -> Lang.tr("hud.editor"), () -> {
                    Settings.save();
                    minecraft.gui.setScreen(new HudEditorScreen(this));
                }), false);
                add(new Button(() -> Lang.tr("hud.reset"), () -> { s.resetHud(); Settings.save(); rebuild(); }), false);
            }
            case SETTINGS -> {
                add(new Header(() -> Lang.tr("tab.settings")), true);
                add(new Segmented(() -> Lang.tr("settings.language"), Lang.LABELS,
                    () -> Lang.indexOf(s.language), i -> { s.language = Lang.CODES[i]; Settings.save(); }), true);
                add(new Swatches(() -> Lang.tr("settings.accent"), () -> s.accent, c -> { s.accent = c; Settings.save(); }), true);
                add(new Toggle(() -> Lang.tr("settings.animations"), () -> s.animations, v -> { s.animations = v; Settings.save(); }), false);
                add(new Button(() -> Lang.tr("settings.reset"), () -> { Settings.resetAll(); rebuild(); }), false);
            }
        }
        if (curCol != 0) curRowY += ROW;
        maxScroll = Math.max(0, curRowY - (viewBottom - viewTop) - 4);
        if (scroll > maxScroll && retry) {
            scroll = maxScroll;
            rebuild(false);
        }
    }

    private boolean visible(Widget w) {
        return w.y >= viewTop && w.y + w.h <= viewBottom;
    }

    private float progress() {
        if (!Settings.get().animations) return 1f;
        return Ui.easeOut((System.nanoTime() - openedAt) / 220_000_000f);
    }

    private int tabX(int i) {
        return wx + 12 + i * ((ww - 24) / Tab.values().length);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        float t = progress();
        int accent = Ui.accent();
        g.fill(0, 0, width, height, Ui.argb(0.55f * t, 0x03070B));

        g.pose().pushMatrix();
        g.pose().translate(0f, (1f - t) * 14f);
        Ui.rounded(g, wx - 1, wy - 1, ww + 2, wh + 2, Ui.argb(t * 0.45f, accent));
        Ui.rounded(g, wx, wy, ww, wh, Ui.argb(t * 0.97f, Ui.BG));

        Ui.text(g, font, "VISUAL PANEL", wx + 12, wy + 10, Ui.argb(t, accent));
        closeButton.alpha = t;
        closeButton.render(g, font, mx, my);

        int tabW = (ww - 24) / Tab.values().length;
        float target = tabX(tab.ordinal());
        if (indicatorX < 0f) indicatorX = target;
        indicatorX += (target - indicatorX) * 0.3f;
        g.fill(wx + 12, wy + 44, wx + ww - 12, wy + 45, Ui.argb(t * 0.2f, Ui.DIM));
        for (Tab tb : Tab.values()) {
            int x = tabX(tb.ordinal());
            boolean over = mx >= x && mx < x + tabW && my >= wy + 26 && my < wy + 46;
            int col = tb == tab ? Ui.TEXT : (over ? Ui.lerp(Ui.DIM, Ui.TEXT, 0.6f) : Ui.DIM);
            Ui.centered(g, font, Ui.fit(font, Lang.tr(tb.key), tabW - 4), x + tabW / 2, wy + 30, Ui.argb(t, col));
        }
        g.fill(Math.round(indicatorX) + 6, wy + 43, Math.round(indicatorX) + tabW - 6, wy + 45, Ui.argb(t, accent));

        for (Widget w : widgets) {
            if (!visible(w)) continue;
            w.alpha = t;
            w.render(g, font, mx, my);
        }
        if (maxScroll > 0) {
            int trackH = viewBottom - viewTop;
            int barH = Math.max(12, trackH * trackH / (trackH + maxScroll));
            int barY = viewTop + (trackH - barH) * scroll / maxScroll;
            g.fill(wx + ww - 6, barY, wx + ww - 4, barY + barH, Ui.argb(t * 0.6f, accent));
        }

        g.fill(wx + 12, wy + wh - 18, wx + ww - 12, wy + wh - 17, Ui.argb(t * 0.2f, Ui.DIM));
        Ui.text(g, font, Ui.fit(font, Lang.tr("panel.hint"), ww - 24), wx + 12, wy + wh - 13, Ui.argb(t, Ui.DIM));
        g.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x(), my = event.y();
            if (closeButton.contains(mx, my)) {
                onClose();
                return true;
            }
            int tabW = (ww - 24) / Tab.values().length;
            for (Tab tb : Tab.values()) {
                int x = tabX(tb.ordinal());
                if (mx >= x && mx < x + tabW && my >= wy + 26 && my < wy + 46) {
                    tab = tb;
                    lastTab = tb;
                    scroll = 0;
                    rebuild();
                    return true;
                }
            }
            for (Widget w : new ArrayList<>(widgets)) {
                if (visible(w) && w.contains(mx, my) && w.mouseDown(mx, my)) {
                    pressed = w;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (pressed != null) {
            pressed.mouseDrag(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (pressed != null) {
            pressed.mouseUp();
            pressed = null;
            Settings.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (maxScroll > 0) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (deltaY * 18)));
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_V) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Settings.save();
        minecraft.options.save();
        minecraft.gui.setScreen(null);
    }
}
