package dev.takesome.htmldom.html;




import static dev.takesome.htmldom.support.validation.HtmlDomValidator.trimToEmpty;
import java.util.Set;



/** Public metadata contract for engine-owned HTML-like tags. */

public record UiHtmlTagMeta(

        String name,

        Set<String> aliases,

        String composerId,

        UiHtmlTagCategory category,

        UiHtmlContentModel contentModel,

        Set<String> allowedAttributes,

        boolean voidTag,

        boolean rawText,

        boolean interactive,

        UiHtmlDefinitionStatus status,

        String replacement,

        String usefulAction,

        String description

) {

    public UiHtmlTagMeta {

        name = value(name);

        aliases = aliases == null ? Set.of() : Set.copyOf(aliases);

        composerId = value(composerId);

        category = category == null ? UiHtmlTagCategory.CONTAINER : category;

        contentModel = contentModel == null ? UiHtmlContentModel.FLOW : contentModel;

        allowedAttributes = allowedAttributes == null ? Set.of() : Set.copyOf(allowedAttributes);

        status = status == null ? UiHtmlDefinitionStatus.STABLE : status;

        replacement = trimToEmpty(replacement);

        usefulAction = trimToEmpty(usefulAction);

        description = trimToEmpty(description);

    }



    public static UiHtmlTagMeta inferred(UiHtmlTagSpec spec) {

        if (spec == null) throw new UiHtmlException("HTML tag spec must not be null");

        String name = spec.name();

        String composer = spec.composerId();

        UiHtmlTagCategory category = inferCategory(name, composer);

        UiHtmlContentModel content = inferContentModel(name, composer, category);

        boolean voidTag = content == UiHtmlContentModel.VOID;

        boolean rawText = content == UiHtmlContentModel.RAW_TEXT;

        boolean interactive = category == UiHtmlTagCategory.CONTROL || hasActionAttributes(spec.allowedAttributes());

        return new UiHtmlTagMeta(

                name,

                spec.aliases(),

                composer,

                category,

                content,

                spec.allowedAttributes(),

                voidTag,

                rawText,

                interactive,

                spec.status(),

                spec.replacement(),

                defaultUsefulAction(category, content),

                defaultDescription(name, category, content)

        );

    }



    private static UiHtmlTagCategory inferCategory(String tag, String composer) {

        String name = value(tag);

        String id = value(composer);

        if ("html".equals(name) || "body".equals(name)) return UiHtmlTagCategory.ROOT;

        if ("head".equals(name) || "link".equals(name) || "meta".equals(name) || "title".equals(name)) return UiHtmlTagCategory.DOCUMENT_METADATA;

        if ("style".equals(name)) return UiHtmlTagCategory.STYLE_RAW;

        if ("template".equals(name)) return UiHtmlTagCategory.TEMPLATE;

        if ("img".equals(name)) return UiHtmlTagCategory.MEDIA;

        if ("i".equals(name) || "icon".equals(id)) return UiHtmlTagCategory.ICON;

        if ("text".equals(id)) return UiHtmlTagCategory.TEXT;

        if ("button".equals(id) || "input".equals(id) || "checkbox".equals(id) || "slider".equals(id) || "combo_box".equals(id)) return UiHtmlTagCategory.CONTROL;

        if ("input_capture".equals(id)) return UiHtmlTagCategory.ENGINE_SPECIAL;

        return UiHtmlTagCategory.CONTAINER;

    }



    private static UiHtmlContentModel inferContentModel(String tag, String composer, UiHtmlTagCategory category) {

        String name = value(tag);

        if ("img".equals(name) || "input".equals(name) || "br".equals(name) || "hr".equals(name) || "link".equals(name) || "meta".equals(name)) return UiHtmlContentModel.VOID;

        if ("head".equals(name)) return UiHtmlContentModel.METADATA;

        if ("title".equals(name)) return UiHtmlContentModel.TEXT_ONLY;

        if ("style".equals(name) || "pre".equals(name) || "code".equals(name)) return UiHtmlContentModel.RAW_TEXT;

        if ("select".equals(name)) return UiHtmlContentModel.OPTIONS_ONLY;

        if (category == UiHtmlTagCategory.TEXT) return UiHtmlContentModel.TEXT_ONLY;

        if (category == UiHtmlTagCategory.CONTROL) return UiHtmlContentModel.MIXED_CONTROL;

        return UiHtmlContentModel.FLOW;

    }



    private static boolean hasActionAttributes(Set<String> attrs) {

        return attrs != null && (attrs.contains("action") || attrs.contains("command") || attrs.contains("data-action"));

    }



    private static String defaultUsefulAction(UiHtmlTagCategory category, UiHtmlContentModel content) {

        if (category == UiHtmlTagCategory.CONTROL) return "Use action/command/data-* for intent routing and bind-* for state.";

        if (category == UiHtmlTagCategory.CONTAINER) return "Use class/style for layout; add action/data-* only when the container is interactive.";

        if (category == UiHtmlTagCategory.MEDIA || category == UiHtmlTagCategory.ICON) return "Usually passive; add action/data-* only for explicit interaction.";

        if (category == UiHtmlTagCategory.DOCUMENT_METADATA) return "Metadata/resource declaration; it is consumed by the document pipeline and not rendered.";

        if (category == UiHtmlTagCategory.STYLE_RAW) return "Raw CSS payload; no action handler.";

        if (category == UiHtmlTagCategory.TEMPLATE) return "Template payload; behavior belongs to Lua/action registry.";

        if (content == UiHtmlContentModel.TEXT_ONLY) return "Text content, i18n-* and bind-text are preferred.";

        return "Declarative structure; behavior is external.";

    }



    private static String defaultDescription(String tag, UiHtmlTagCategory category, UiHtmlContentModel content) {

        return tag + " tag, category=" + category + ", content=" + content + ".";

    }



    private static String value(String value) {

        return trimToEmpty(value);

    }

}
