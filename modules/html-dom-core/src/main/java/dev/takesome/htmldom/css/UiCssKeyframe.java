package dev.takesome.htmldom.css;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record UiCssKeyframe(double offset, Map<String, String> declarations) {
    public UiCssKeyframe {
        offset = Math.max(0.0, Math.min(1.0, offset));
        declarations = declarations == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(declarations));
    }
}
