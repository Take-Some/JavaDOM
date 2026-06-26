package dev.takesome.htmldom.markup.internal.parse.scanner;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Small parse-local cache for canonical tag and attribute names. */
public final class UiHtmlNameCanonicalizer {
    private final Map<String, String> cache = new HashMap<>(128);

    public String canonical(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String cached = cache.get(raw);
        if (cached != null) {
            return cached;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        cache.put(raw, normalized);
        return normalized;
    }
}
