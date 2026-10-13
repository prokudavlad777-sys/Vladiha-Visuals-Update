package com.vladiha.visualpanel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local settings stored in config/visualpanel.json. */
public final class Settings {
    public static final class ElementSettings {
        public boolean enabled = true;
        public float x = 0.01f;
        public float y = 0.01f;
        public float scale = 1.0f;
        public float opacity = 0.85f;
        public int color = 0xFFFFFF;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Settings instance;

    public String language = "en";
    public int accent = 0x2DD4BF;
    public boolean animations = true;
    public boolean hudEnabled = true;
    public Map<String, ElementSettings> elements = new LinkedHashMap<>();

    public static Settings get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("visualpanel.json");
    }

    private static Settings load() {
        Settings s = null;
        try {
            Path p = file();
            if (Files.exists(p)) s = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), Settings.class);
        } catch (Exception e) {
            VisualPanelMod.LOGGER.error("Could not read visualpanel.json, using defaults", e);
        }
        if (s == null) s = new Settings();
        s.sanitize();
        return s;
    }

    private void sanitize() {
        if (language == null || !java.util.Arrays.asList(Lang.CODES).contains(language)) language = "en";
        accent &= 0xFFFFFF;
        if (elements == null) elements = new LinkedHashMap<>();
        for (HudElement e : HudElement.values()) {
            ElementSettings es = elements.get(e.id);
            if (es == null) {
                elements.put(e.id, e.defaults());
                continue;
            }
            es.x = clamp(es.x, 0f, 1f);
            es.y = clamp(es.y, 0f, 1f);
            es.scale = clamp(es.scale, 0.5f, 3f);
            es.opacity = clamp(es.opacity, 0.1f, 1f);
            es.color &= 0xFFFFFF;
        }
    }

    public ElementSettings element(HudElement e) {
        return elements.computeIfAbsent(e.id, k -> e.defaults());
    }

    public static void resetAll() {
        instance = new Settings();
        instance.sanitize();
        save();
    }

    public void resetHud() {
        hudEnabled = true;
        elements.clear();
        sanitize();
    }

    public static void save() {
        try {
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, GSON.toJson(get()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            VisualPanelMod.LOGGER.error("Could not save visualpanel.json", e);
        }
    }

    public static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
