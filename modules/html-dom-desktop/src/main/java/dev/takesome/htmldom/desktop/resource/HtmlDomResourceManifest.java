package dev.takesome.htmldom.desktop.resource;

import dev.takesome.htmldom.logging.HtmlDomLog;
import dev.takesome.htmldom.logging.HtmlDomLogger;

import java.util.ArrayList;
import java.util.List;

public final class HtmlDomResourceManifest {
    private static final HtmlDomLogger LOG = HtmlDomLog.logger(HtmlDomResourceManifest.class);
    private final String id;
    private final List<Entry> entries;

    public HtmlDomResourceManifest(String id, List<Entry> entries) {
        this.id = id == null || id.isBlank() ? "html-dom-resource-manifest" : id.trim();
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static HtmlDomResourceManifest ofRequired(String id, Entry... entries) {
        return new HtmlDomResourceManifest(id, entries == null ? List.of() : List.of(entries));
    }

    public ValidationResult validate(HtmlDomResourceResolver resolver) {
        if (resolver == null) throw new IllegalArgumentException("resolver must not be null");
        ArrayList<Entry> missing = new ArrayList<>();
        long started = System.nanoTime();
        for (Entry entry : entries) {
            if (entry == null || !entry.required()) continue;
            if (resolver.resolve(entry.path(), entry.kind()).isEmpty()) missing.add(entry);
        }
        ValidationResult result = new ValidationResult(id, entries.size(), missing, Math.max(0L, (System.nanoTime() - started) / 1_000_000L));
        if (result.ok()) LOG.info("HtmlDom resource manifest valid id='{}' entries={} elapsedMs={}", id, entries.size(), result.elapsedMs());
        else LOG.warn("HtmlDom resource manifest invalid id='{}' entries={} missing={} elapsedMs={}", id, entries.size(), missing, result.elapsedMs());
        return result;
    }

    public String id() { return id; }
    public List<Entry> entries() { return entries; }

    public record Entry(String path, HtmlDomResourceKind kind, boolean required) {
        public Entry {
            path = path == null ? "" : path.trim();
            kind = kind == null ? HtmlDomResourceKind.BINARY : kind;
        }
        public static Entry required(String path, HtmlDomResourceKind kind) { return new Entry(path, kind, true); }
        public static Entry optional(String path, HtmlDomResourceKind kind) { return new Entry(path, kind, false); }
    }

    public record ValidationResult(String manifestId, int totalEntries, List<Entry> missing, long elapsedMs) {
        public ValidationResult { missing = missing == null ? List.of() : List.copyOf(missing); }
        public boolean ok() { return missing.isEmpty(); }
    }
}
