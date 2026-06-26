package dev.takesome.htmldom.dom;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/** XML-compatible parser that builds the HtmlDom DOM. */
public final class UiDomParser {
    public UiDomDocument parse(String source) {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("UI DOM source must not be blank");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setIgnoringComments(true);
            factory.setExpandEntityReferences(false);
            secure(factory);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document parsed = builder.parse(new InputSource(new StringReader(source)));
            parsed.getDocumentElement().normalize();

            UiDomDocument document = new UiDomDocument();
            document.setRoot(element(document, parsed.getDocumentElement()));
            document.drainMutations();
            document.root().clearDirty();
            return document;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid HtmlDom DOM markup: " + ex.getMessage(), ex);
        }
    }

    private UiDomElement element(UiDomDocument document, Element source) {
        UiDomElement element = document.createElement(source.getTagName());
        NamedNodeMap attrs = source.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            element.setAttribute(attr.getNodeName(), attr.getNodeValue());
        }
        NodeList children = source.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                element.appendChild(element(document, childElement));
            } else if (child instanceof CharacterData text) {
                String value = collapse(text.getData());
                if (!value.isBlank()) element.appendChild(document.createText(value));
            }
        }
        return element;
    }

    private void secure(DocumentBuilderFactory factory) {
        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }

    private void setFeature(DocumentBuilderFactory factory, String feature, boolean enabled) {
        try {
            factory.setFeature(feature, enabled);
        } catch (Exception ignored) {
            // XML provider may not support every hardening flag.
        }
    }

    private String collapse(String value) {
        if (value == null || value.isBlank()) return "";
        return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim().replaceAll("\\s+", " ");
    }
}
