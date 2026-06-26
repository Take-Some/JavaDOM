package dev.takesome.htmldom.css;

import dev.takesome.htmldom.dom.UiDomElement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Result of resolving CSS bounds/layout against a viewport. */
public final class UiCssLayoutResult {
    private final LinkedHashMap<Integer, UiCssBox> boxes = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, java.util.List<UiCssLineBox>> lineBoxes = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, java.util.List<UiCssInlineBox>> inlineBoxes = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, UiCssScrollBox> scrollBoxes = new LinkedHashMap<>();

    void put(UiDomElement element, UiCssBox box) {
        boxes.put(element.nodeId(), box);
    }

    void putLineBoxes(UiDomElement element, java.util.List<UiCssLineBox> lines) {
        if (element == null || lines == null || lines.isEmpty()) return;
        lineBoxes.put(element.nodeId(), java.util.List.copyOf(lines));
    }

    void putInlineBoxes(UiDomElement element, java.util.List<UiCssInlineBox> runs) {
        if (element == null || runs == null || runs.isEmpty()) return;
        inlineBoxes.put(element.nodeId(), java.util.List.copyOf(runs));
    }

    void putScrollBox(UiDomElement element, UiCssScrollBox scrollBox) {
        if (element == null || scrollBox == null) return;
        scrollBoxes.put(element.nodeId(), scrollBox);
    }

    public Optional<UiCssBox> box(UiDomElement element) {
        if (element == null) return Optional.empty();
        return Optional.ofNullable(boxes.get(element.nodeId()));
    }

    public Map<Integer, UiCssBox> boxes() {
        return Collections.unmodifiableMap(boxes);
    }

    public java.util.List<UiCssLineBox> lineBoxes(UiDomElement element) {
        if (element == null) return java.util.List.of();
        return lineBoxes.getOrDefault(element.nodeId(), java.util.List.of());
    }

    public Map<Integer, java.util.List<UiCssLineBox>> lineBoxes() {
        return Collections.unmodifiableMap(lineBoxes);
    }

    public java.util.List<UiCssInlineBox> inlineBoxes(UiDomElement element) {
        if (element == null) return java.util.List.of();
        return inlineBoxes.getOrDefault(element.nodeId(), java.util.List.of());
    }

    public Map<Integer, java.util.List<UiCssInlineBox>> inlineBoxes() {
        return Collections.unmodifiableMap(inlineBoxes);
    }

    public Optional<UiCssScrollBox> scrollBox(UiDomElement element) {
        if (element == null) return Optional.empty();
        return Optional.ofNullable(scrollBoxes.get(element.nodeId()));
    }

    public Map<Integer, UiCssScrollBox> scrollBoxes() {
        return Collections.unmodifiableMap(scrollBoxes);
    }
}
