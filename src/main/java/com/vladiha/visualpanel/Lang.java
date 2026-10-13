package com.vladiha.visualpanel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** In-mod language switching (independent of the game language). */
public final class Lang {
    public static final String[] CODES = {"en", "ru", "uk", "pl", "de"};
    public static final String[] LABELS = {"EN", "RU", "UA", "PL", "DE"};

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    private Lang() {}

    public static int indexOf(String code) {
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) return i;
        return 0;
    }

    private static Map<String, String> load(String code) {
        return CACHE.computeIfAbsent(code, c -> {
            try (InputStream in = Lang.class.getResourceAsStream("/assets/visualpanel/panel_lang/" + c + ".json")) {
                if (in == null) return Map.of();
                Map<String, String> map = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), MAP_TYPE);
                return map == null ? Map.of() : map;
            } catch (Exception e) {
                VisualPanelMod.LOGGER.error("Failed to load language {}", c, e);
                return Map.of();
            }
        });
    }

    public static String tr(String key) {
        String value = load(Settings.get().language).get(key);
        if (value == null) value = load("en").get(key);
        return value == null ? key : value;
    }

    public static String tr(String key, Object... args) {
        return String.format(Locale.ROOT, tr(key), args);
    }
}
