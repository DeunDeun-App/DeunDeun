package com.example.deundeun.ui.translate;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class PictogramEmoji {

    private static final String DEFAULT = "📢";

    private static final Map<String, String> EMOJI = new HashMap<>();

    static {
        EMOJI.put("RAIN", "☔");
        EMOJI.put("TYPHOON", "🌀");
        EMOJI.put("WATER", "🌊");
        EMOJI.put("EARTHQUAKE", "⚠️");
        EMOJI.put("FIRE", "🔥");
        EMOJI.put("SMOKE", "💨");
        EMOJI.put("HOT", "☀️");
        EMOJI.put("COLD", "🥶");
        EMOJI.put("SNOW", "☃️");
        EMOJI.put("WIND", "🌬️");
        EMOJI.put("MASK", "😷");
        EMOJI.put("MOUNTAIN", "⛰️");
        EMOJI.put("VIRUS", "🦠");
        EMOJI.put("HAND_WASH", "🧼");
        EMOJI.put("SEARCH", "🔍");
        EMOJI.put("NO_ENTRY", "⛔");
        EMOJI.put("RUN", "🏃");
        EMOJI.put("HOUSE", "🏠");
        EMOJI.put("UP", "⬆️");
        EMOJI.put("TABLE", "🙇");
        EMOJI.put("WINDOW", "🚪");
        EMOJI.put("DRINK", "💧");
        EMOJI.put("CLOTHES", "🧥");
        EMOJI.put("CAR", "🚗");
        EMOJI.put("PHONE", "📞");
        EMOJI.put("SHELTER", "🏫");
        EMOJI.put("INFO", DEFAULT);
    }

    private PictogramEmoji() {
    }

    public static String of(@Nullable String code) {
        String emoji = code == null ? null : EMOJI.get(code);
        return emoji == null ? DEFAULT : emoji;
    }
}
