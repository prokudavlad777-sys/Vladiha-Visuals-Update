package com.vladiha.visualpanel.ui;

import com.vladiha.visualpanel.HudElement;
import com.vladiha.visualpanel.HudRenderer;
import com.vladiha.visualpanel.Lang;
import com.vladiha.visualpanel.Settings;
import com.vladiha.visualpanel.ui.Widgets.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** HUD editor: drag elements, scroll to scale, panel for opacity / colour / visibility. */
public final class HudEditorScreen extends Screen {
    private static final int ROW = 24;

    private final Screen parent;
    private HudElement selected = HudElement.COORDINATES;
    private HudElement dragging;
    private float dragOffX, dragOffY;

    private final List<Widget> always = new ArrayList<>();
    private final List<Widget> selectedOnly = new ArrayList<>();
    private Widget pressed;
    private int px, py, pw, ph;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    private Settings.ElementSettings sel() {
        return Settings.get().element(selected);
    }

    private Slider channel(String key, int shift) {
        return new Slider(() -> Lang.tr(key), 0, 255, 1,
            () -> (sel().color >> shift) & 255,
            v -> sel().color = (sel().color & ~(255 << shift)) | (((int) Math.round(v)) << shift),
            v -> String.valueOf(Math.round(v)));
    }

    @Override
    protected void init() {
        always.clear();
        selectedOnly.clear();
        pw = Math.min(400, width - 16);
        ph = 16 + ROW * 5 + 4;
        px = (width - pw) / 2;

        selectedOnly.add(new Toggle(() -> Lang.tr("editor.enabled"), () -> sel().enabled, v -> sel().enabled = v));
        selectedOnly.add(new Slider(() -> Lang.tr("editor.scale"), 0.5, 3.0, 0.05,
            () -> sel().scale, v -> sel().scale = (float) v, v -> String.format(java.util.Locale.ROOT, "%.2fx", v)));
        selectedOnly.add(new Slider(() -> Lang.tr("editor.opacity"), 10, 100, 1,
            () -> sel().opacity * 100.0, v -> sel().opacity = (float) (v / 100.0), v -> Math.round(v) + "%"));
        selectedOnly.add(new Swatches(() -> Lang.tr("editor.color"), () -> sel().color, c -> sel().color = c));
        selectedOnly.add(channel("editor.red", 16));
        selectedOnly.add(channel("editor.green", 8));
        selectedOnly.add(channel("editor.blue", 0));
        selectedOnly.add(new Button(() -> Lang.tr("editor.reset"), () -> {
            Settings.ElementSettings d = selected.defaults();
            Settings.ElementSettings s = sel();
            s.enabled = d.enabled; s.x = d.x; s.y = d.y; s.scale = d.scale; s.opacity = d.opacity; s.color = d.color;
        }));

        always.add(new Button(() -> Lang.tr("editor.resetAll"), () -> Settings.get().resetHud()));
        always.add(new Button(() -> Lang.tr("editor.done"), this::onClose));
        layout();
    }

    /** Places the panel at the bottom, or at the top if the selected element is in the lower half. */
    private void layout() {
        float[] r = HudRenderer.rect(minecraft, selected, sel(), width, height);
        py = (r[1] + r[3] / 2f > height / 2f) ? 8 : height - ph - 8;
        int gap = 8, colW = (pw - 16 - gap) / 2;
        int lx = px + 8, rx = lx + colW + gap;
        int y0 = py + 16;
        place(selectedOnly.get(0), lx, y0, colW);             // enabled
        place(selectedOnly.get(1), rx, y0, colW);             // scale (shares row w/ toggle)
        place(selectedOnly.get(2), lx, y0 + ROW, colW);       // opacity
        place(selectedOnly.get(3), rx, y0 + ROW, colW);       // swatches
        place(selectedOnly.get(4), lx, y0 + ROW * 2, colW);   // R
        place(selectedOnly.get(5), rx, y0 + ROW * 2, colW);   // G
        place(selectedOnly.get(6), lx, y0 + ROW * 3, colW);   // B
        place(selectedOnly.get(7), rx, y0 + ROW * 3, colW);   // reset element
        place(always.get(0), lx, y0 + ROW * 4, colW);         // reset all
        place(always.get(1), rx, y0 + ROW * 4, colW);         // done
    }

    private static void place(Widget w, int x, int y, int width) {
        w.x = x;
        w.y = y;
        w.w = width;
        w.h = ROW - 4;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partialTick) {
        layout();
        int accent = Ui.accent();
        g.fill(0, 0, width, height, Ui.argb(0.45f, 0x03070B));
        g.fill(width / 2, 0, width / 2 + 1, height, Ui.argb(0.12f, accent));
        g.fill(0, height / 2, width, height / 2 + 1, Ui.argb(0.12f, accent));

        Settings settings = Settings.get();
        for (HudElement e : HudElement.values()) {
            HudRenderer.draw(g, minecraft, e, settings.element(e), true);
            float[] r = HudRenderer.rect(minecraft, e, settings.element(e), width, height);
            int x = Math.round(r[0]), y = Math.round(r[1]), w = Math.round(r[2]), h = Math.round(r[3]);
            Ui.outline(g, x - 1, y - 1, w + 2, h + 2, Ui.argb(e == selected ? 1f : 0.3f, e == selected ? accent : Ui.DIM));
        }

        Ui.rounded(g, px - 1, py - 1, pw + 2, ph + 2, Ui.argb(0.45f, accent));
        Ui.rounded(g, px, py, pw, ph, Ui.argb(0.97f, Ui.BG));
        Ui.text(g, font, Ui.fit(font, Lang.tr("editor.title") + ": " + Lang.tr(selected.nameKey()), pw - 16), px + 8, py + 5, Ui.argb(1f, accent));
        for (Widget w : selectedOnly) {
            w.alpha = 1f;
            w.render(g, font, mx, my);
        }
        for (Widget w : always) {
            w.alpha = 1f;
            w.render(g, font, mx, my);
        }
        Ui.text(g, font, Ui.fit(font, Lang.tr("editor.hint"), width - 16), 8, py < height / 2 ? py + ph + 6 : py - 12, Ui.argb(1f, Ui.DIM));
    }

    private boolean inPanel(double mx, double my) {
        return mx >= px && mx < px + pw && my >= py && my < py + ph;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x(), my = event.y();
            if (inPanel(mx, my)) {
                List<Widget> all = new ArrayList<>(selectedOnly);
                all.addAll(always);
                for (Widget w : all) {
                    if (w.contains(mx, my) && w.mouseDown(mx, my)) {
                        pressed = w;
                        return true;
                    }
                }
                return true;
            }
            HudElement[] all = HudElement.values();
            for (int i = all.length - 1; i >= 0; i--) {
                float[] r = HudRenderer.rect(minecraft, all[i], Settings.get().element(all[i]), width, height);
                if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                    selected = all[i];
                    dragging = all[i];
                    dragOffX = (float) mx - r[0];
                    dragOffY = (float) my - r[1];
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
        if (dragging != null) {
            Settings.ElementSettings s = Settings.get().element(dragging);
            float[] r = HudRenderer.rect(minecraft, dragging, s, width, height);
            float nx = Settings.clamp((float) event.x() - dragOffX, 0f, Math.max(0f, width - r[2]));
            float ny = Settings.clamp((float) event.y() - dragOffY, 0f, Math.max(0f, height - r[3]));
            s.x = nx / width;
            s.y = ny / height;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = pressed != null || dragging != null;
        if (pressed != null) pressed.mouseUp();
        pressed = null;
        dragging = null;
        if (handled) {
            Settings.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (!inPanel(mouseX, mouseY)) {
            Settings.ElementSettings s = sel();
            s.scale = Settings.clamp(Math.round((s.scale + (float) deltaY * 0.1f) * 20f) / 20f, 0.5f, 3f);
            Settings.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        Settings.save();
        minecraft.gui.setScreen(parent);
    }
}
