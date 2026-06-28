package dev.takesome.htmldom.desktop.resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ordered set of named resource namespaces for one HtmlDom document/runtime. */
public final class HtmlDomResourceBundle {
    private final List<HtmlDomResourceNamespace> namespaces;
    private final Map<String, HtmlDomResourceNamespace> byId;
    private final boolean strict;

    public HtmlDomResourceBundle(List<HtmlDomResourceNamespace> namespaces, boolean strict) {
        ArrayList<HtmlDomResourceNamespace> ordered = new ArrayList<>();
        LinkedHashMap<String, HtmlDomResourceNamespace> index = new LinkedHashMap<>();
        if (namespaces != null) {
            for (HtmlDomResourceNamespace namespace : namespaces) {
                if (namespace == null || index.containsKey(namespace.id())) continue;
                ordered.add(namespace);
                index.put(namespace.id(), namespace);
            }
        }
        if (!index.containsKey("classpath-root")) {
            HtmlDomResourceNamespace root = HtmlDomResourceNamespace.classpath("classpath-root", "");
            ordered.add(root);
            index.put(root.id(), root);
        }
        this.namespaces = List.copyOf(ordered);
        this.byId = Map.copyOf(index);
        this.strict = strict;
    }

    public static HtmlDomResourceBundle forDocument(String sourcePath, String... basePaths) {
        ArrayList<HtmlDomResourceNamespace> namespaces = new ArrayList<>();
        if (basePaths != null) {
            int index = 0;
            for (String base : basePaths) {
                String normalized = normalizeBase(base);
                if (!normalized.isBlank()) namespaces.add(HtmlDomResourceNamespace.hybrid(index++ == 0 ? "explicit" : "explicit-" + index, normalized));
            }
        }
        String documentBase = documentBase(sourcePath);
        if (!documentBase.isBlank()) namespaces.add(HtmlDomResourceNamespace.hybrid("document", documentBase));
        namespaces.add(HtmlDomResourceNamespace.classpath("html-dom-bundled", "html-dom/bundled"));
        namespaces.add(HtmlDomResourceNamespace.hybrid("classpath-root", ""));
        return new HtmlDomResourceBundle(namespaces, Boolean.getBoolean("htmldom.resources.strict"));
    }

    public List<HtmlDomResourceNamespace> namespaces() {
        return namespaces;
    }

    public HtmlDomResourceNamespace namespace(String id) {
        return byId.get(id == null ? "" : id.trim());
    }

    public boolean strict() {
        return strict;
    }

    public HtmlDomResourceBundle withNamespace(HtmlDomResourceNamespace namespace) {
        ArrayList<HtmlDomResourceNamespace> out = new ArrayList<>();
        if (namespace != null) out.add(namespace);
        out.addAll(namespaces);
        return new HtmlDomResourceBundle(out, strict);
    }

    private static String documentBase(String sourcePath) {
        String source = normalizeBase(sourcePath);
        int slash = source.lastIndexOf('/');
        return slash < 0 ? "" : source.substring(0, slash);
    }

    private static String normalizeBase(String raw) {
        String value = raw == null ? "" : raw.trim().replace('\\', '/').replaceAll("/++", "/");
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
