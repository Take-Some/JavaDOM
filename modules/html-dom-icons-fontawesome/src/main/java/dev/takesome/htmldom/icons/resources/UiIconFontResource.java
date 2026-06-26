package dev.takesome.htmldom.icons.resources;

/**
 * Classpath descriptor for a bundled icon-font file.
 */
public final class UiIconFontResource {
    private final String id;
    private final String familyId;
    private final String styleId;
    private final String displayName;
    private final String classpathPath;

    public UiIconFontResource(String id, String familyId, String styleId, String displayName, String classpathPath) {
        this.id = clean(id, "id");
        this.familyId = clean(familyId, "familyId");
        this.styleId = clean(styleId, "styleId");
        this.displayName = clean(displayName, "displayName");
        this.classpathPath = cleanPath(classpathPath);
    }

    public String id() {
        return id;
    }

    public String familyId() {
        return familyId;
    }

    public String styleId() {
        return styleId;
    }

    public String key() {
        return familyId + ":" + styleId;
    }

    public String displayName() {
        return displayName;
    }

    public String classpathPath() {
        return classpathPath;
    }

    private static String clean(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    private static String cleanPath(String value) {
        String cleaned = clean(value, "classpathPath").replace('\\', '/');
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }
}
