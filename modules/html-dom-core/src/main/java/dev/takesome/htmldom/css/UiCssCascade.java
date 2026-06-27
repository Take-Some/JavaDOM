package dev.takesome.htmldom.css;

import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;


import static dev.takesome.htmldom.support.validation.HtmlDomValidator.emptyIfNull;
import dev.takesome.htmldom.support.logging.HtmlDomLog;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.takesome.htmldom.support.logging.HtmlDomLog.Logger;

/** Applies parsed CSS rules to the UI DOM and writes computed styles. */
public final class UiCssCascade {
    private static final Logger LOG = HtmlDomLog.logger(UiCssCascade.class);
    private static final String TRACE_COMPUTED_PROPERTY = "htmldom.css.traceComputed";
    private static final String LEGACY_TRACE_COMPUTED_PROPERTY = "helix.ui.css.traceComputed";
    private static final String TRACE_CASCADE_PROPERTY = "htmldom.css.traceCascade";
    private static final UiCssSpecificity USER_AGENT_SPECIFICITY = new UiCssSpecificity(-1000, -1000, -1000);
    private static final int USER_AGENT_ORDER = Integer.MIN_VALUE;
    private static final Pattern VAR_PATTERN = Pattern.compile("var\\(\\s*(--[A-Za-z0-9_-]+)\\s*(?:,\\s*([^)]*))?\\)");
    private static final List<String> INHERITED_PROPERTIES = List.of(
            "color",
            "text-color",
            "icon-color",
            "font",
            "font-family",
            "font-size",
            "font-weight",
            "font-style",
            "line-height",
            "text-align",
            "align",
            "visibility",
            "letter-spacing",
            "word-spacing",
            "white-space",
            "text-transform"
    );
    private final UiCssParser parser = new UiCssParser();
    private final UiCssParseContext parseContext = new UiCssParseContext();
    private final UiCssPropertyRegistry properties;

    public UiCssCascade() {
        this(UiCssPropertyRegistry.loadBuiltins());
    }

    public UiCssCascade(UiCssPropertyRegistry properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public UiCssStyleImpact apply(UiDomDocument document, UiStylesheet stylesheet) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(stylesheet, "stylesheet");
        UiDomElement root = document.root();
        Map<String, String> variables = rootVariables(root, stylesheet);
        long started = System.nanoTime();
        int elementCount = 0;
        int matchedRules = 0;
        int appliedDeclarations = 0;
        UiCssStyleImpact impact = UiCssStyleImpact.NONE;
        for (UiDomElement element : UiDomTraversal.depthFirstElements(root)) {
            elementCount++;
            Map<String, AppliedValue> values = new LinkedHashMap<>();
            Map<String, Map<String, AppliedValue>> pseudoValues = new LinkedHashMap<>();
            applyUserAgentRules(element, values, pseudoValues);
            for (UiCssRule rule : stylesheet.rules()) {
                if (!rule.matches(element)) continue;
                matchedRules++;
                Map<String, AppliedValue> target = rule.hasPseudoElement()
                        ? pseudoValues.computeIfAbsent(rule.pseudoElement(), ignored -> new LinkedHashMap<>())
                        : values;
                for (UiCssDeclaration declaration : rule.declarations()) {
                    if (declaration.property().startsWith("--")) continue;
                    for (UiCssDeclaration expanded : expandDeclaration(declaration)) {
                        put(target, expanded.property(), expanded.value(), rule.specificity(), rule.order(), "css:" + rule.selectorText());
                        appliedDeclarations++;
                    }
                }
            }
            applyInline(element, values);
            applyInheritedValues(element, values);
            LinkedHashMap<String, String> resolved = new LinkedHashMap<>();
            values.forEach((property, value) -> resolved.put(property, resolveVariables(value.value(), variables)));
            LinkedHashMap<String, Map<String, String>> resolvedPseudo = new LinkedHashMap<>();
            pseudoValues.forEach((pseudoElement, style) -> {
                LinkedHashMap<String, String> target = new LinkedHashMap<>();
                style.forEach((property, value) -> target.put(property, resolveVariables(value.value(), variables)));
                resolvedPseudo.put(pseudoElement, target);
            });
            impact = impact.merge(element.replaceComputedStyle(resolved, resolvedPseudo));
            traceComputedStyleIfRequested(element, resolved, values);
        }
        if (Boolean.getBoolean(TRACE_CASCADE_PROPERTY)) {
            LOG.info(
                    "UI CSS cascade applied root='{}' elements={} authorRules={} matchedRules={} declarations={} variables={} impact={} elapsedMs={}",
                    elementDescription(root),
                    elementCount,
                    stylesheet.rules().size(),
                    matchedRules,
                    appliedDeclarations,
                    variables.keySet(),
                    impact,
                    Math.max(0L, (System.nanoTime() - started) / 1_000_000L)
            );
        } else if (LOG.debugEnabled()) {
            LOG.debug(
                    "UI CSS cascade applied root='{}' elements={} authorRules={} matchedRules={} declarations={} variables={} impact={} elapsedMs={}",
                    elementDescription(root),
                    elementCount,
                    stylesheet.rules().size(),
                    matchedRules,
                    appliedDeclarations,
                    variables.keySet(),
                    impact,
                    Math.max(0L, (System.nanoTime() - started) / 1_000_000L)
            );
        }
        return impact;
    }

    private void applyUserAgentRules(
            UiDomElement element,
            Map<String, AppliedValue> values,
            Map<String, Map<String, AppliedValue>> pseudoValues
    ) {
        for (UiCssRule rule : UiCssUserAgentStylesheet.stylesheet().rules()) {
            if (!rule.matches(element)) continue;
            Map<String, AppliedValue> target = rule.hasPseudoElement()
                    ? pseudoValues.computeIfAbsent(rule.pseudoElement(), ignored -> new LinkedHashMap<>())
                    : values;
            for (UiCssDeclaration declaration : rule.declarations()) {
                if (declaration.property().startsWith("--")) continue;
                for (UiCssDeclaration expanded : expandDeclaration(declaration)) {
                    put(target, expanded.property(), expanded.value(), USER_AGENT_SPECIFICITY, USER_AGENT_ORDER + rule.order(), "ua:" + rule.selectorText());
                }
            }
        }
    }

    private void applyInheritedValues(UiDomElement element, Map<String, AppliedValue> values) {
        UiDomElement parent = element.parent();
        if (parent == null) return;
        UiCssSpecificity inheritedSpecificity = new UiCssSpecificity(-1, -1, -1);
        for (String property : INHERITED_PROPERTIES) {
            String parentValue = parent.baseComputedStyle().getOrDefault(property, "");
            AppliedValue current = values.get(property);
            if (current != null && "inherit".equalsIgnoreCase(current.value())) {
                values.put(property, new AppliedValue(parentValue, current.specificity(), current.order(), current.source() + ":inherit"));
            } else if (current == null && !parentValue.isBlank()) {
                values.put(property, new AppliedValue(parentValue, inheritedSpecificity, -1, "inherit:" + elementDescription(parent)));
            }
        }
    }

    private Map<String, String> rootVariables(UiDomElement root, UiStylesheet stylesheet) {
        LinkedHashMap<String, AppliedValue> values = new LinkedHashMap<>();
        for (UiCssRule rule : stylesheet.rules()) {
            if (rule.hasPseudoElement() || !rule.matches(root)) continue;
            for (UiCssDeclaration declaration : rule.declarations()) {
                if (declaration.property().startsWith("--")) put(values, canonicalProperty(declaration.property()), declaration.value(), rule.specificity(), rule.order(), "css:" + rule.selectorText());
            }
        }
        for (UiCssDeclaration declaration : parser.declarations(root.attribute("style", ""))) {
            if (declaration.property().startsWith("--")) put(values, canonicalProperty(declaration.property()), declaration.value(), new UiCssSpecificity(1000, 0, 0), Integer.MAX_VALUE, "inline:style");
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        values.forEach((key, value) -> out.put(key, value.value()));
        return out;
    }

    private void applyInline(UiDomElement element, Map<String, AppliedValue> values) {
        String inline = element.attribute("style", "");
        if (inline.isBlank()) return;
        for (UiCssDeclaration declaration : parser.declarations(inline)) {
            if (declaration.property().startsWith("--")) continue;
            for (UiCssDeclaration expanded : expandDeclaration(declaration)) {
                put(values, expanded.property(), expanded.value(), new UiCssSpecificity(1000, 0, 0), Integer.MAX_VALUE, "inline:style");
            }
        }
    }

    private List<UiCssDeclaration> expandDeclaration(UiCssDeclaration declaration) {
        UiCssPropertySpec spec = properties.find(declaration.property()).orElse(null);
        if (spec instanceof UiCssShorthandPropertySpec shorthand) {
            ArrayList<UiCssDeclaration> out = new ArrayList<>();
            for (UiCssDeclaration expanded : shorthand.expand(parseContext, declaration.value())) {
                if (!expanded.property().equals(declaration.property())) out.addAll(expandDeclaration(expanded));
            }
            return out;
        }
        return List.of(new UiCssDeclaration(canonicalProperty(declaration.property()), declaration.value()));
    }

    private String canonicalProperty(String property) {
        if (property == null || property.isBlank() || property.startsWith("-" + "-")) return emptyIfNull(property);
        return properties.find(property).map(UiCssPropertySpec::name).orElse(property.trim().toLowerCase(java.util.Locale.ROOT));
    }

    private void put(Map<String, AppliedValue> values, String property, String value, UiCssSpecificity specificity, int order, String source) {
        AppliedValue existing = values.get(property);
        AppliedValue next = new AppliedValue(value, specificity, order, emptyIfNull(source));
        if (existing == null || next.winsOver(existing)) values.put(property, next);
    }

    private void traceComputedStyleIfRequested(UiDomElement element, Map<String, String> resolved, Map<String, AppliedValue> values) {
        if (element == null || resolved == null || resolved.isEmpty()) return;
        String configured = configuredTraceSelectors();
        if (configured.isBlank()) return;
        for (String rawSelector : configured.split("\s*,\s*")) {
            String selector = trimToEmpty(rawSelector);
            if (selector.isBlank() || !matchesTraceSelector(element, selector)) continue;
            String rawFamily = firstNonBlank(resolved.get("font-family"), resolved.get("font"));
            String resolvedFontId = UiCssFontFamilyResolver.resolveEngineFontId(rawFamily, resolved);
            LOG.info(
                    "UI CSS computed trace selector='{}' element='{}' font-family='{}' font='{}' resolvedFontId='{}' font-size='{}' scale='{}' color='{}' text-align='{}' align='{}' sources={} style={}",
                    selector,
                    elementDescription(element),
                    resolved.getOrDefault("font-family", ""),
                    resolved.getOrDefault("font", ""),
                    resolvedFontId,
                    resolved.getOrDefault("font-size", ""),
                    resolved.getOrDefault("scale", ""),
                    resolved.getOrDefault("color", ""),
                    resolved.getOrDefault("text-align", ""),
                    resolved.getOrDefault("align", ""),
                    sourceSummary(values),
                    resolved
            );
        }
    }

    private boolean matchesTraceSelector(UiDomElement element, String selector) {
        try {
            return element.matches(selector);
        } catch (RuntimeException error) {
            LOG.warn("Ignoring invalid UI CSS computed trace selector='{}': {}", selector, error.getMessage());
            return false;
        }
    }

    private String configuredTraceSelectors() {
        String configured = trimToEmpty(System.getProperty(TRACE_COMPUTED_PROPERTY, ""));
        if (!configured.isBlank()) return configured;
        return trimToEmpty(System.getProperty(LEGACY_TRACE_COMPUTED_PROPERTY, ""));
    }

    private Map<String, String> sourceSummary(Map<String, AppliedValue> values) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (String property : List.of("font-family", "font", "font-size", "scale", "color", "text-align", "align")) {
            AppliedValue value = values.get(property);
            if (value != null) out.put(property, value.source() + " @" + value.specificity() + ":" + value.order());
        }
        return out;
    }

    private String firstNonBlank(String first, String second) {
        String a = trimToEmpty(first);
        return a.isBlank() ? trimToEmpty(second) : a;
    }

    private String elementDescription(UiDomElement element) {
        if (element == null) return "";
        StringBuilder out = new StringBuilder(element.tagName());
        if (!element.id().isBlank()) out.append('#').append(element.id());
        for (String className : element.classList().values()) out.append('.').append(className);
        return out.toString();
    }

    private String resolveVariables(String value, Map<String, String> variables) {
        if (value == null || value.isBlank() || variables == null || variables.isEmpty()) return emptyIfNull(value);
        String result = value;
        for (int i = 0; i < 8; i++) {
            Matcher matcher = VAR_PATTERN.matcher(result);
            StringBuilder out = new StringBuilder();
            boolean changed = false;
            while (matcher.find()) {
                String name = matcher.group(1);
                String fallback = trimToEmpty(matcher.group(2));
                String replacement = variables.getOrDefault(name, fallback);
                matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
                changed = true;
            }
            matcher.appendTail(out);
            result = out.toString().trim();
            if (!changed) break;
        }
        return result;
    }

    private record AppliedValue(String value, UiCssSpecificity specificity, int order, String source) {
        boolean winsOver(AppliedValue other) {
            int compare = specificity.compareTo(other.specificity);
            return compare > 0 || (compare == 0 && order >= other.order);
        }
    }
}
