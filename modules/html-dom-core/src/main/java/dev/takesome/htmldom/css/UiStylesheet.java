package dev.takesome.htmldom.css;



import java.util.ArrayList;

import java.util.Collections;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;



/** Parsed stylesheet containing normal rules, @font-face metadata and @keyframes. */

public final class UiStylesheet {

    private final List<UiCssRule> rules;

    private final List<UiFontFaceRule> fontFaces;

    private final Map<String, UiCssKeyframesRule> keyframes;



    public UiStylesheet(List<UiCssRule> rules, List<UiFontFaceRule> fontFaces) {

        this(rules, fontFaces, Map.of());

    }



    public UiStylesheet(List<UiCssRule> rules, List<UiFontFaceRule> fontFaces, Map<String, UiCssKeyframesRule> keyframes) {

        this.rules = Collections.unmodifiableList(rules == null ? List.of() : List.copyOf(rules));

        this.fontFaces = Collections.unmodifiableList(fontFaces == null ? List.of() : List.copyOf(fontFaces));

        LinkedHashMap<String, UiCssKeyframesRule> frames = new LinkedHashMap<>();

        if (keyframes != null) {

            keyframes.forEach((name, rule) -> {

                if (name != null && rule != null && !name.isBlank() && !rule.empty()) frames.put(name.trim(), rule);

            });

        }

        this.keyframes = Collections.unmodifiableMap(frames);

    }



    public static UiStylesheet empty() {

        return new UiStylesheet(List.of(), List.of(), Map.of());

    }



    public List<UiCssRule> rules() { return rules; }

    public List<UiFontFaceRule> fontFaces() { return fontFaces; }

    public Map<String, UiCssKeyframesRule> keyframes() { return keyframes; }



    public UiStylesheet plus(UiStylesheet other) {

        if (other == null) return this;

        ArrayList<UiCssRule> nextRules = new ArrayList<>(rules);

        nextRules.addAll(other.rules);

        ArrayList<UiFontFaceRule> nextFonts = new ArrayList<>(fontFaces);

        nextFonts.addAll(other.fontFaces);

        LinkedHashMap<String, UiCssKeyframesRule> nextFrames = new LinkedHashMap<>(keyframes);

        nextFrames.putAll(other.keyframes);

        return new UiStylesheet(nextRules, nextFonts, nextFrames);

    }

}
