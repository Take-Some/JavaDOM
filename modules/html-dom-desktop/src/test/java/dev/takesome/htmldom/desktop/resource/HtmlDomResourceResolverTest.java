package dev.takesome.htmldom.desktop.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HtmlDomResourceResolverTest {
    @Test
    void resolvesBundledCssThroughNamedNamespace() {
        HtmlDomResourceResolver resolver = new HtmlDomResourceResolver(
                getClass().getClassLoader(),
                HtmlDomResourceBundle.forDocument("html-dom/bundled/test.ui.html", "html-dom/bundled")
        );

        HtmlDomResource resource = resolver.resolve("showcase.ui.css", HtmlDomResourceKind.CSS).orElseThrow();

        assertEquals(HtmlDomResourceKind.CSS, resource.kind());
        assertTrue(resource.textUtf8().contains(".page"));
        assertEquals(64, resource.sha256().length());
        assertTrue(resource.size() > 0);
    }

    @Test
    void missingResourceReturnsEmptyWhenStrictModeIsDisabled() {
        HtmlDomResourceResolver resolver = new HtmlDomResourceResolver(
                getClass().getClassLoader(),
                HtmlDomResourceBundle.forDocument("html-dom/bundled/test.ui.html", "html-dom/bundled")
        );

        assertTrue(resolver.resolve("missing-resource-does-not-exist.css", HtmlDomResourceKind.CSS).isEmpty());
    }

    @Test
    void manifestValidationReportsMissingRequiredResources() {
        HtmlDomResourceResolver resolver = new HtmlDomResourceResolver(
                getClass().getClassLoader(),
                HtmlDomResourceBundle.forDocument("html-dom/bundled/test.ui.html", "html-dom/bundled")
        );
        HtmlDomResourceManifest manifest = HtmlDomResourceManifest.ofRequired(
                "test-manifest",
                HtmlDomResourceManifest.Entry.required("showcase.ui.css", HtmlDomResourceKind.CSS),
                HtmlDomResourceManifest.Entry.required("missing.css", HtmlDomResourceKind.CSS),
                HtmlDomResourceManifest.Entry.optional("missing-optional.css", HtmlDomResourceKind.CSS)
        );

        HtmlDomResourceManifest.ValidationResult result = manifest.validate(resolver);

        assertFalse(result.ok());
        assertEquals(1, result.missing().size());
        assertEquals("missing.css", result.missing().get(0).path());
    }
}
