package com.vladiha.visualpanel.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Lightweight custom widgets (no dependency on vanilla widget classes). */
public final class Widgets {
    private Widgets() {}

    public abstract static class Widget {
        public int x, y, w, h;
        public float alpha = 1f;
        protected float hover;

        public abstract void render(GuiGraphicsExtractor g, Font font, int mx, int my);

        public boolean mouseDown(double mx, double my) {
            return false;
        }

        public void mouseDrag(double mx, double my) {}

        public void mouseUp() {}

        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        protected void animateHover(int mx, int my) {
            hover += ((contains(mx, my) ? 1f : 0f) - hover) * 0.3f;
        }
    }

    /** Section title. */
    public static final class Header extends Widget {
        private final Supplier<String> label;

        public Header(Supplier<String> label) {
            this.label = label;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, int mx, int my) {
            String s = Ui.fit(font, label.get().toUpperCase(java.util.Locale.ROOT), w);
            Ui.text(g, font, s, x, y + 3, Ui.argb(alpha, Ui.accent()));
            g.fill(x, y + h - 1, x + w, y + h, Ui.argb(alpha * 0.25f, Ui.accent()));
        }
    }

    public static final class Button extends Widget {
        private final Supplier<String> label;
        private final Runnable action;

        public Button(Supplier<String> label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, int mx, int my) {
            animateHover(mx, my);
            Ui.rounded(g, x, y, w, h, Ui.argb(alpha, Ui.lerp(Ui.SURFACE2, Ui.accent(), hover * 0.35f)));
            if (hover > 0.05f) Ui.outline(g, x, y, w, h, Ui.argb(alpha * hover * 0.8f, Ui.accent()));
            Ui.centered(g, font, Ui.fit(font, label.get(), w - 6), x + w / 2, y + (h - 8) / 2, Ui.argb(alpha, Ui.TEXT));
        }

        @Override
        public boolean mouseDown(double mx, double my) {
            action.run();
            return true;
        }
    }

    public static final class Toggle extends Widget {
        private final Supplier<String> label;
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private float knob = -1f;

        public Toggle(Supplier<String> label, BooleanSupplier get, Consumer<Boolean> set) {
            this.label = label;
            this.get = get;
            this.set = set;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, int mx, int my) {
            animateHover(mx, my);
            boolean on = get.getAsBoolean();
            if (knob < 0f) knob = on ? 1f : 0f;
            knob += ((on ? 1f : 0f) - knob) * 0.3f;
            Ui.rounded(g, x, y, w, h, Ui.argb(alpha, Ui.lerp(Ui.SURFACE, Ui.SURFACE2, hover)));
            Ui.text(g, font, Ui.fit(font, label.get(), w - 40), x + 6, y + (h - 8) / 2, Ui.argb(alpha, Ui.TEXT));
            int pw = 24, ph = 10;
            int px = x + w - pw - 6, py = y + (h - ph) / 2;
            Ui.rounded(g, px, py, pw, ph, Ui.argb(alpha, Ui.lerp(0x3A4654, Ui.accent(), knob)));
            int kx = px + 1 + Math.round((pw - ph) * knob);
            g.fill(kx, py + 1, kx + ph - 2, py + ph - 1, Ui.argb(alpha, 0xFFFFFF));
        }

        @Override
        public boolean mouseDown(double mx, double my) {
            set.accept(!get.getAsBoolean());
            return true;
        }
    }

    public static final class Slider extends Widget {
        private final Supplier<String> label;
        private final double min, max, step;
        private final DoubleSupplier get;
        private final DoubleConsumer set;
        private final DoubleFunction<String> format;
        private boolean dragging;

        public Slider(Supplier<String> label, double min, double max, double step,
                      DoubleSupplier get, DoubleConsumer set, DoubleFunction<String> format) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.get = get;
            this.set = set;
            this.format = format;
        }

        private void apply(double mx) {
            double t = Math.max(0, Math.min(1, (mx - (x + 4)) / (double) (w - 8)));
            double v = min + (max - min) * t;
            if (step > 0) v = Math.round(v / step) * step;
            set.accept(Math.max(min, Math.min(max, v)));
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, int mx, int my) {
            animateHover(mx, my);
            double v = get.getAsDouble();
            String val = format.apply(v);
            int vw = font.width(val);
            Ui.text(g, font, Ui.fit(font, label.get(), w - vw - 10), x + 2, y + 1, Ui.argb(alpha, Ui.TEXT));
            Ui.text(g, font, val, x + w - vw - 2, y + 1, Ui.argb(alpha, Ui.accent()));
            int ty = y + h - 7;
            g.fill(x + 2, ty, x + w - 2, ty + 3, Ui.argb(alpha, Ui.SURFACE2));
            float t = (float) ((v - min) / (max - min));
            int fx = x + 4 + Math.round((w - 8) * Math.max(0f, Math.min(1f, t)));
            g.fill(x + 2, ty, fx, ty + 3, Ui.argb(alpha, Ui.accent()));
            int kh = dragging ? 9 : 7 + Math.round(hover * 2);
            g.fill(fx - 2, ty + 1 - kh / 2, fx + 2, ty + 2 + kh / 2, Ui.argb(alpha, 0xFFFFFF));
        }

        @Override
        public boolean mouseDown(double mx, double my) {
            dragging = true;
            apply(mx);
            return true;
        }

        @Override
        public void mouseDrag(double mx, double my) {
            if (dragging) apply(mx);
        }

        @Override
        public void mouseUp() {
            dragging = false;
        }
    }

    /** Row of colour swatches with a label. */
    public static final class Swatches extends Widget {
        public static final int[] PRESETS = {
            0x2DD4BF, 0x22D3EE, 0x60A5FA, 0xA78BFA, 0xF472B6,
            0xF87171, 0xFB923C, 0xFACC15, 0x4ADE80, 0xFFFFFF
        };
        private final Supplier<String> label;
        private final IntSupplier get;
        private final IntConsumer set;

        public Swatches(Supplier<String> label, IntSupplier get, IntConsumer set) {
            this.label = label;
            this.get = get;
            this.set = set;
        }

        private int size() {
            return Math.max(8, Math.min(12, (w - 4 * (PRESETS.length - 1)) / PRESETS.length));
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, int mx, int my) {
            Ui.text(g, font, Ui.fit(font, label.get(), w), x + 2, y, Ui.argb(alpha, Ui.TEXT));
            int s = size();
            for (int i = 0; i < PRESETS.length; i++) {
                int sx = x + 2 + i * (s + 4);
                int sy = y + 11;
                g.fill(sx, sy, sx + s, sy + s, Ui.argb(alpha, PRESETS[i]));
                if ((get.getAsInt() & 0xFFFFFF) == PRESETS[i]) Ui.outline(g, sx - 2, sy - 2, s + 4, s + 4, Ui.argb(alpha, 0xFFFFFF));
            }
        }

        @Override
        public boolean mouseDown(double mx, double my) {
            int s = size();
            for (int i = 0; i < PRESETS.length; i++) {
                int sx = x + 2 + i * (s + 4);
                if (mx >= sx - 2 && mx < sx + s + 2 && my >= y + 9) {
                    set.accept(PRESETS[i]);
                    return true;
                }
            }
            return false;
        }
    }

    /** Segmented choice (used for the language). */
    public static final class Segmented extends Widget {
        private final Supplier<String> label;
        private final String[] options;
        private final IntSupplier get;
        private final IntConsumer set;

        public Segmented(Supplier<String> label, String[] options, IntSupplier get, IntConsumer set) {
            this.label = label;
            this.options = options;
            this.get = get;
            this.set = set;
        }

        private int segW() {
            return (w - 4) / options.length;
        }

        @Override
        public void render(GuiGraphicsExtractor g, Font font, int mx, int my) {
            Ui.text(g, font, Ui.fit(font, label.get(), w), x + 2, y, Ui.argb(alpha, Ui.TEXT));
            int sw = segW();
            for (int i = 0; i < options.length; i++) {
                int sx = x + 2 + i * sw;
                int sy = y + 10;
                boolean active = get.getAsInt() == i;
                boolean over = mx >= sx && mx < sx + sw && my >= sy && my < sy + 12;
                int bg = active ? Ui.accent() : Ui.lerp(Ui.SURFACE2, Ui.accent(), over ? 0.3f : 0f);
                g.fill(sx, sy, sx + sw - 2, sy + 12, Ui.argb(alpha, bg));
                Ui.centered(g, font, options[i], sx + (sw - 2) / 2, sy + 2, Ui.argb(alpha, active ? Ui.BG : Ui.TEXT));
            }
        }

        @Override
        public boolean mouseDown(double mx, double my) {
            int sw = segW();
            int i = (int) ((mx - (x + 2)) / sw);
            if (i >= 0 && i < options.length && my >= y + 10) {
                set.accept(i);
                return true;
            }
            return false;
        }
    }
}
