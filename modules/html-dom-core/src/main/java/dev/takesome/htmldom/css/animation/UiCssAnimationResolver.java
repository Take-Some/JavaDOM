package dev.takesome.htmldom.css.animation;




import static dev.takesome.htmldom.support.validation.HtmlDomValidator.lowerTrimToEmpty;
import java.util.ArrayList;

import java.util.List;

import java.util.Locale;

import java.util.Map;



public final class UiCssAnimationResolver {

    public List<UiCssAnimationDescriptor> resolveAll(Map<String, String> style) {

        List<String> names = list(value(style, "animation-name", "none"));

        List<String> durations = list(value(style, "animation-duration", "0s"));

        List<String> delays = list(value(style, "animation-delay", "0s"));

        List<String> timing = list(value(style, "animation-timing-function", "ease"));

        List<String> iterations = list(value(style, "animation-iteration-count", "1"));

        List<String> directions = list(value(style, "animation-direction", "normal"));

        List<String> fills = list(value(style, "animation-fill-mode", "none"));

        List<String> plays = list(value(style, "animation-play-state", "running"));

        int count = Math.max(1, names.size());

        ArrayList<UiCssAnimationDescriptor> out = new ArrayList<>();

        for (int i = 0; i < count; i++) {

            out.add(new UiCssAnimationDescriptor(

                    item(names, i, "none"),

                    time(item(durations, i, item(durations, 0, "0s"))),

                    time(item(delays, i, item(delays, 0, "0s"))),

                    item(timing, i, item(timing, 0, "ease")),

                    iteration(item(iterations, i, item(iterations, 0, "1"))),

                    item(directions, i, item(directions, 0, "normal")),

                    item(fills, i, item(fills, 0, "none")),

                    item(plays, i, item(plays, 0, "running"))

            ));

        }

        return List.copyOf(out);

    }

    private String value(Map<String,String> style, String key, String fallback) {

        if (style == null) return fallback;

        String v = style.getOrDefault(key, "");

        return v == null || v.isBlank() ? fallback : v.trim();

    }

    private List<String> list(String raw) {

        ArrayList<String> out = new ArrayList<>();

        if (raw == null || raw.isBlank()) return out;

        StringBuilder cur = new StringBuilder(); int depth = 0; char quote = 0;

        for (int i = 0; i < raw.length(); i++) {

            char ch = raw.charAt(i);

            if (quote != 0) { cur.append(ch); if (ch == quote) quote = 0; continue; }

            if (ch == '\'' || ch == '"') { quote = ch; cur.append(ch); continue; }

            if (ch == '(') depth++; if (ch == ')' && depth > 0) depth--;

            if (ch == ',' && depth == 0) { add(out, cur); continue; }

            cur.append(ch);

        }

        add(out, cur); return out;

    }

    private void add(List<String> out, StringBuilder cur) { String v = cur.toString().trim(); if (!v.isBlank()) out.add(v); cur.setLength(0); }

    private String item(List<String> values, int index, String fallback) { return values == null || values.isEmpty() ? fallback : index < values.size() ? values.get(index) : fallback; }

    private long time(String raw) {

        String v = lowerTrimToEmpty(raw, Locale.ROOT);

        try { if (v.endsWith("ms")) return Math.round(Double.parseDouble(v.substring(0, v.length()-2).trim()));

            if (v.endsWith("s")) return Math.round(Double.parseDouble(v.substring(0, v.length()-1).trim()) * 1000.0);

            return Math.round(Double.parseDouble(v)); } catch (RuntimeException ignored) { return 0L; }

    }

    private double iteration(String raw) {

        String v = lowerTrimToEmpty(raw, Locale.ROOT);

        if ("infinite".equals(v)) return -1.0;

        try { return Math.max(0.0, Double.parseDouble(v)); } catch (RuntimeException ignored) { return 1.0; }

    }

}
