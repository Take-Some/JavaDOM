package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.css.UiCssCascade;
import dev.takesome.htmldom.css.UiCssLayoutEngine;
import dev.takesome.htmldom.css.UiCssLayoutResult;
import dev.takesome.htmldom.css.UiCssPaintTreeBuilder;
import dev.takesome.htmldom.css.UiCssParser;
import dev.takesome.htmldom.css.UiCssPropertyRegistry;
import dev.takesome.htmldom.css.UiCssScrollBox;
import dev.takesome.htmldom.css.UiCssUserAgentStylesheet;
import dev.takesome.htmldom.css.UiStylesheet;
import dev.takesome.htmldom.devtools.HtmlDomDevTools;
import dev.takesome.htmldom.devtools.HtmlDomDevToolsHitNode;
import dev.takesome.htmldom.devtools.HtmlDomDevToolsSnapshot;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomTraversal;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollAxis;
import dev.takesome.htmldom.desktop.HtmlDomHitTestEngine.ScrollDrag;
import dev.takesome.htmldom.fonts.HtmlDomFonts;
import dev.takesome.htmldom.icons.fontawesome.FontAwesomeFonts;
import dev.takesome.htmldom.markup.UiMarkupDocument;
import dev.takesome.htmldom.markup.UiMarkupParser;
import dev.takesome.htmldom.scripting.lua.HtmlDomLuaRuntime;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Stroke;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Composite;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.Paint;
import java.util.List;

/** Desktop Swing renderer for HtmlDom markup + CSS. No browser and no HTTP server. */
public final class HtmlDomSwingPanel extends JPanel implements HtmlDomInputRouter.Host {
    private final UiDomDocument dom;
    private final UiStylesheet stylesheet;
    private final UiCssCascade cascade = new UiCssCascade();
    private final UiCssLayoutEngine layoutEngine = new UiCssLayoutEngine(UiCssPropertyRegistry.loadBuiltins(), new HtmlDomJava2DTextMeasurer());
    private final UiCssPaintTreeBuilder paintTreeBuilder = new UiCssPaintTreeBuilder();
    private final HtmlDomScrollController scrollController = new HtmlDomScrollController();
    private final HtmlDomPositioningController positioningController = new HtmlDomPositioningController();
    private final HtmlDomFocusController focusController = new HtmlDomFocusController();
    private final HtmlDomHitTestEngine hitTest = new HtmlDomHitTestEngine();
    private final HtmlDomPaintEngine paintEngine = new HtmlDomPaintEngine();
    private final HtmlDomTextPaintEngine textPaintEngine = new HtmlDomTextPaintEngine();
    private final HtmlDomOverlayPaintEngine overlayPaintEngine = new HtmlDomOverlayPaintEngine();
    private final HtmlDomScrollbarPaintEngine scrollbarPaintEngine = new HtmlDomScrollbarPaintEngine();
    private final HtmlDomControlPaintEngine controlPaintEngine = new HtmlDomControlPaintEngine();
    private final HtmlDomTransformEngine transformEngine = new HtmlDomTransformEngine();
    private final HtmlDomTransitionController transitionController = new HtmlDomTransitionController();
    private final Timer transitionTimer = new Timer(16, event -> repaint());
    private final HtmlDomEventDispatcher eventDispatcher = new HtmlDomEventDispatcher();
    private final HtmlDomPseudoStateController pseudoStateController = new HtmlDomPseudoStateController();
    private ScrollDrag activeScrollDrag;
    private UiDomElement hoveredScrollElement;
    private ScrollAxis hoveredScrollAxis;
    private final HtmlDomLuaRuntime lua;
    private final HtmlDomInputRouter inputRouter;
    private HtmlDomDevToolsWindow devToolsWindow;
    private int devToolsHighlightedNodeId;
    private UiCssLayoutResult layout;

    public HtmlDomSwingPanel(String markup, String css) {
        UiMarkupDocument document = new UiMarkupParser().parse(markup == null ? "" : markup, "desktop.ui.html");
        this.dom = document.dom();
        FontAwesomeFonts.register(HtmlDomFonts.registry());
        this.lua = new HtmlDomLuaRuntime(this.dom);
        this.lua.execute(optionalResource("html-dom/bundled/showcase.lua"), "showcase.lua");
        this.stylesheet = UiCssUserAgentStylesheet.stylesheet().plus(new UiCssParser().parse(css == null ? "" : css));
        setOpaque(true);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        this.inputRouter = new HtmlDomInputRouter(this);
        this.inputRouter.install(this);
        this.transitionTimer.setRepeats(true);
    }

    public UiDomDocument document() {
        return dom;
    }

    public HtmlDomEventDispatcher.ListenerRegistration addEventListener(String elementId, String type, HtmlDomEventDispatcher.Handler handler) {
        return addEventListener(elementId, type, false, handler);
    }

    public HtmlDomEventDispatcher.ListenerRegistration addEventListener(String elementId, String type, boolean capture, HtmlDomEventDispatcher.Handler handler) {
        UiDomElement element = dom.getElementById(elementId == null ? "" : elementId).orElse(null);
        return addEventListener(element, type, capture, handler);
    }

    public HtmlDomEventDispatcher.ListenerRegistration addEventListener(UiDomElement element, String type, HtmlDomEventDispatcher.Handler handler) {
        return addEventListener(element, type, false, handler);
    }

    public HtmlDomEventDispatcher.ListenerRegistration addEventListener(UiDomElement element, String type, boolean capture, HtmlDomEventDispatcher.Handler handler) {
        return eventDispatcher.addEventListener(element, type, capture, handler);
    }

    public HtmlDomDevToolsSnapshot devToolsSnapshot() {
        if (layout == null) {
            cascade.apply(dom, stylesheet);
            applyTransitions();
            layout = layoutEngine.layout(dom, Math.max(1, getWidth()), Math.max(1, getHeight()));
            scrollController.sync(layout);
        }
        return HtmlDomDevTools.snapshot(dom, layout, paintTreeBuilder.build(dom, layout), scrollController.resolvedScrollBoxes(layout));
    }

    public List<HtmlDomDevToolsHitNode> devToolsHitTargets() {
        return hitTest.devToolsHitTargets(this::selector);
    }

    public List<HtmlDomEventDispatcher.EventLogEntry> devToolsEventLog() {
        return eventDispatcher.eventLog();
    }

    public void setDevToolsHighlightedNodeId(int nodeId) {
        int next = Math.max(0, nodeId);
        if (devToolsHighlightedNodeId == next) return;
        devToolsHighlightedNodeId = next;
        repaint();
    }

    private static String optionalResource(String path) {
        try (java.io.InputStream stream = HtmlDomSwingPanel.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) return "";
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException ignored) {
            return "";
        }
    }

    @Override public UiCssLayoutResult layoutResult() {
        return layout;
    }

    @Override public void ensureLayout() {
        if (layout != null) return;
        cascade.apply(dom, stylesheet);
        applyTransitions();
        layout = layoutEngine.layout(dom, Math.max(1, getWidth()), Math.max(1, getHeight()));
        scrollController.sync(layout);
    }

    @Override public HtmlDomHitTestEngine hitTest() {
        return hitTest;
    }

    @Override public HtmlDomScrollController scrollController() {
        return scrollController;
    }

    @Override public HtmlDomScrollbarPaintEngine scrollbarPaintEngine() {
        return scrollbarPaintEngine;
    }

    @Override public HtmlDomFocusController focusController() {
        return focusController;
    }

    @Override public void repaintHost() {
        repaint();
    }

    @Override public void requestPanelFocus() {
        requestFocusInWindow();
    }

    @Override public void dispatchEvent(String type, UiDomElement target) {
        eventDispatcher.dispatch(type, target);
    }

    @Override public void openDevTools() {
        if (devToolsWindow == null) devToolsWindow = new HtmlDomDevToolsWindow(this);
        devToolsWindow.open();
    }

    @Override public ScrollDrag activeScrollDrag() {
        return activeScrollDrag;
    }

    @Override public void setActiveScrollDrag(ScrollDrag drag) {
        activeScrollDrag = drag;
    }

    @Override public UiDomElement hoveredScrollElement() {
        return hoveredScrollElement;
    }

    @Override public ScrollAxis hoveredScrollAxis() {
        return hoveredScrollAxis;
    }

    @Override public void setHoveredScrollbar(UiDomElement element, ScrollAxis axis) {
        hoveredScrollElement = element;
        hoveredScrollAxis = axis;
    }

    @Override public boolean setHoveredElement(UiDomElement element) {
        return pseudoStateController.setHoveredElement(dom.documentElement(), element);
    }

    @Override public boolean setActiveElement(UiDomElement element) {
        return pseudoStateController.setActiveElement(dom.documentElement(), element);
    }

    @Override public boolean clearActiveElement() {
        return pseudoStateController.clearActiveElement(dom.documentElement());
    }

    @Override public void activate(UiDomElement element) {
        if (element == null) return;
        eventDispatcher.dispatch("click", element, domEvent -> {
            if (domEvent.defaultPrevented()) return;
            UiDomElement current = domEvent.currentTarget();
            if (current == null) return;
            String action = current.data("action", "");
            String tab = current.data("tab", "");
            boolean actionable = !action.isBlank()
                    || !tab.isBlank()
                    || "button".equals(current.tagName())
                    || "button".equals(current.attribute("role", ""))
                    || current.classList().contains("button");
            if (!actionable) return;
            if (lua.call("onClick", action, current.id())) {
                domEvent.preventDefault();
                domEvent.stopPropagation();
                return;
            }
            if ("theme".equals(action)) toggleTheme();
            else if ("pulse".equals(action)) pulse();
            else if ("dialog".equals(action)) setDialog(true);
            else if ("close-dialog".equals(action)) setDialog(false);
            else if (!tab.isBlank()) activateTab(tab);
            lua.call("afterClick", action, current.id());
            domEvent.preventDefault();
            domEvent.stopPropagation();
        });
    }

    public void scrollIntoView(String elementId, String block, String inline) {
        inputRouter.scrollIntoView(elementId, block, inline);
    }

    private ScrollAnchor captureScrollAnchor() {
        if (layout == null || focusController.focusedElement() == null) return null;
        UiDomElement scroller = nearestScrollContainer(focusController.focusedElement());
        if (scroller == null) return null;
        UiCssScrollBox box = scrollBox(scroller);
        if (box == null) return null;
        Rectangle anchorRect = rect(focusController.focusedElement());
        return new ScrollAnchor(scroller, focusController.focusedElement(), anchorRect.x, anchorRect.y, box.scrollX(), box.scrollY());
    }

    private void restoreScrollAnchor(ScrollAnchor anchor) {
        if (anchor == null || anchor.scroller == null || anchor.element == null) return;
        UiCssScrollBox box = scrollBox(anchor.scroller);
        if (box == null) return;
        Rectangle after = rect(anchor.element);
        float nextX = anchor.scrollX + (after.x - anchor.x);
        float nextY = anchor.scrollY + (after.y - anchor.y);
        scrollController.set(anchor.scroller, box, nextX, nextY);
    }

    @Override public UiDomElement nearestScrollContainer(UiDomElement element) {
        return positioningController.nearestScrollContainer(element, this::scrollBox);
    }

    @Override public UiDomElement firstScrollContainer() {
        if (layout == null) return null;
        for (UiDomElement element : UiDomTraversal.depthFirstElements(dom.documentElement())) {
            if (scrollBox(element) != null) return element;
        }
        return null;
    }

    private void toggleTheme() {
        UiDomElement body = dom.body().orElse(dom.renderRoot());
        boolean light = body.classList().contains("theme-light");
        if (light) {
            body.classList().remove("theme-light");
            body.classList().add("theme-dark");
        } else {
            body.classList().remove("theme-dark");
            body.classList().add("theme-light");
        }
    }

    private void activateTab(String name) {
        for (UiDomElement element : dom.getElementsByTagName("button")) {
            if (!element.data("tab", "").isBlank()) {
                setClass(element, "active", name.equals(element.data("tab", "")));
            }
        }
        for (UiDomElement element : dom.getElementsByClassName("tab-panel")) {
            setClass(element, "active", name.equals(element.data("panel", "")));
        }
    }

    private void pulse() {
        UiDomElement body = dom.body().orElse(dom.renderRoot());
        body.classList().add("pulse");
        UiDomElement toast = dom.getElementById("toast").orElse(null);
        if (toast != null) toast.classList().add("open");
        Timer timer = new Timer(900, event -> {
            body.classList().remove("pulse");
            if (toast != null) toast.classList().remove("open");
            repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    @Override public void setDialogOpen(boolean open) {
        setDialog(open);
    }

    private void setDialog(boolean open) {
        dom.getElementById("showcase-dialog").ifPresent(dialog -> setClass(dialog, "open", open));
    }

    @Override public boolean dialogOpen() {
        return dom.getElementById("showcase-dialog").map(dialog -> dialog.classList().contains("open")).orElse(false);
    }

    private void setClass(UiDomElement element, String token, boolean enabled) {
        if (enabled && !element.classList().contains(token)) element.classList().add(token);
        if (!enabled && element.classList().contains(token)) element.classList().remove(token);
    }

    private void applyTransitions() {
        boolean running = transitionController.apply(dom);
        for (HtmlDomTransitionController.TransitionEndEvent event : transitionController.drainFinishedEvents()) {
            eventDispatcher.dispatchTransitionEnd(event.element(), event.propertyName(), event.elapsedMs(), domEvent -> {
                UiDomElement target = domEvent.target();
                UiDomElement current = domEvent.currentTarget();
                lua.call("onTransitionEnd", domEvent.propertyName(), target == null ? "" : target.id(), current == null ? "" : current.id(), Long.toString(domEvent.elapsedMs()));
            });
        }
        updateTransitionTimer(running);
    }

    private void updateTransitionTimer(boolean running) {
        if (running && !transitionTimer.isRunning()) transitionTimer.start();
        else if (!running && transitionTimer.isRunning()) transitionTimer.stop();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            ScrollAnchor anchor = captureScrollAnchor();
            cascade.apply(dom, stylesheet);
            applyTransitions();
            layout = layoutEngine.layout(dom, Math.max(1, getWidth()), Math.max(1, getHeight()));
            scrollController.sync(layout);
            restoreScrollAnchor(anchor);
            hitTest.clear();
            paintElement(g, dom.documentElement());
            paintFixedLayer(g);
            paintOverlayElements(g);
            paintDevToolsHighlight(g);
            if (devToolsWindow != null) devToolsWindow.refresh();
        } finally {
            g.dispose();
        }
    }

    private void paintElement(Graphics2D g, UiDomElement element) {
        if (skip(element)) return;
        if (paintEngine.fixed(element) && !paintEngine.paintingFixedLayer()) return;
        if ("dialog".equals(element.tagName()) || element.classList().contains("toast")) return;
        Rectangle rect = rect(element);
        if (rect.width <= 0 || rect.height <= 0) return;
        float opacity = opacity(element);
        if (opacity <= 0f) return;
        Graphics2D layer = (Graphics2D) g.create();
        try {
            transformEngine.apply(layer, element, rect);
            if (opacity < 0.999f) layer.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            paintBackground(layer, element, rect);
            paintBorder(layer, element, rect);
            paintContent(layer, element, rect);
            paintOutline(layer, element, rect);
            paintScrollbars(layer, element, rect);
        } finally {
            layer.dispose();
        }
    }

    private void paintFixedLayer(Graphics2D g) {
        paintEngine.paintFixedLayer(g, dom, this::paintElement);
    }

    private void paintBackground(Graphics2D g, UiDomElement element, Rectangle r) {
        paintEngine.paintBackground(g, element, r);
    }


    private void paintBorder(Graphics2D g, UiDomElement element, Rectangle r) {
        paintEngine.paintBorder(g, element, r);
    }


    private void paintContent(Graphics2D g, UiDomElement element, Rectangle r) {
        Graphics2D content = (Graphics2D) g.create();
        Shape pushedClip = null;
        try {
            if (clickable(element)) hitTest.addHit(g, element, r);
            UiCssScrollBox scroll = layout == null ? null : scrollBox(element);
            if (scroll != null) hitTest.addScrollHit(g, element, r);
            if (clipsContents(element)) {
                Shape clip = clipShape(element, r);
                pushedClip = g.getTransform().createTransformedShape(clip);
                hitTest.pushClip(pushedClip);
                content.clip(clip);
            }
            if (scroll != null && (scroll.scrollX() > 0f || scroll.scrollY() > 0f)) content.translate(-scroll.scrollX(), -scroll.scrollY());
            paintSpecialOrText(content, element, r);
            paintNormalDescendants(content, element);
            paintPositionedDescendants(content, element, r);
        } finally {
            hitTest.popClip(pushedClip);
            content.dispose();
        }
    }

    private void paintNormalDescendants(Graphics2D g, UiDomElement element) {
        for (UiDomElement childElement : paintTreeBuilder.orderedChildren(element)) {
            if (paintEngine.fixed(childElement) && !paintEngine.paintingFixedLayer()) continue;
            if (!positioned(childElement)) paintElement(g, childElement);
        }
    }

    private void paintPositionedDescendants(Graphics2D g, UiDomElement element, Rectangle parentRect) {
        for (UiDomElement childElement : paintTreeBuilder.orderedChildren(element)) {
            if (paintEngine.fixed(childElement) && !paintEngine.paintingFixedLayer()) continue;
            if (positioned(childElement)) paintElement(g, childElement);
        }
    }

    private void paintOutline(Graphics2D g, UiDomElement element, Rectangle r) {
        paintEngine.paintOutline(g, element, r, element == focusController.focusedElement());
    }


    private void paintDevToolsHighlight(Graphics2D g) {
        if (devToolsHighlightedNodeId <= 0 || layout == null) return;
        UiDomElement element = null;
        for (UiDomElement candidate : UiDomTraversal.depthFirstElements(dom.documentElement())) {
            if (candidate.nodeId() == devToolsHighlightedNodeId) {
                element = candidate;
                break;
            }
        }
        if (element == null || skip(element)) return;
        Rectangle borderBox = rect(element);
        if (borderBox.width <= 0 || borderBox.height <= 0) return;

        DevToolsInsets margin = cssInsets(element, "margin", 0);
        DevToolsInsets border = cssInsets(element, "border", fallbackBorderWidth(element));
        DevToolsInsets padding = cssInsets(element, "padding", 0);
        Rectangle marginBox = expand(borderBox, margin);
        Rectangle paddingBox = inset(borderBox, border);
        Rectangle contentBox = inset(paddingBox, padding);
        int radius = Math.max(0, cssPixels(firstStyle(element, "border-radius", "border-top-left-radius"), 18, Math.min(borderBox.width, borderBox.height)));

        Composite oldComposite = g.getComposite();
        Stroke oldStroke = g.getStroke();
        Font oldFont = g.getFont();
        Color oldColor = g.getColor();
        Paint oldPaint = g.getPaint();
        try {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            fillRounded(g, marginBox, radius + 10, new Color(245, 197, 91, 64));
            fillRounded(g, borderBox, radius, new Color(124, 179, 66, 92));
            fillRounded(g, paddingBox, Math.max(0, radius - Math.max(border.top(), border.left())), new Color(74, 222, 128, 58));
            fillRounded(g, contentBox, Math.max(0, radius - Math.max(border.top() + padding.top(), border.left() + padding.left())), new Color(90, 132, 255, 72));

            paintDevToolsHatch(g, marginBox, new Color(117, 91, 22, 54));
            paintDevToolsChildOverlays(g, element, marginBox);

            g.setStroke(new BasicStroke(1.15f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 3f}, 0f));
            g.setColor(new Color(177, 75, 255, 220));
            drawRounded(g, paddingBox, Math.max(0, radius - 2));
            drawRounded(g, contentBox, Math.max(0, radius - 4));

            g.setStroke(new BasicStroke(2f));
            g.setColor(new Color(0, 136, 255, 245));
            drawRounded(g, marginBox, radius + 10);

            drawDevToolsLabel(g, element, borderBox, marginBox);
        } finally {
            g.setComposite(oldComposite);
            g.setStroke(oldStroke);
            g.setFont(oldFont);
            g.setColor(oldColor);
            g.setPaint(oldPaint);
        }
    }

    private void paintDevToolsChildOverlays(Graphics2D g, UiDomElement element, Rectangle clip) {
        Stroke oldStroke = g.getStroke();
        Shape oldClip = g.getClip();
        try {
            g.clip(clip);
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
            for (UiDomElement child : paintTreeBuilder.orderedChildren(element)) {
                if (skip(child)) continue;
                Rectangle r = rect(child);
                if (r.width <= 0 || r.height <= 0 || !r.intersects(clip)) continue;
                g.setColor(new Color(168, 85, 247, 38));
                g.fillRect(r.x, r.y, r.width, r.height);
                g.setColor(new Color(168, 85, 247, 205));
                g.drawRect(r.x, r.y, Math.max(0, r.width - 1), Math.max(0, r.height - 1));
            }
        } finally {
            g.setClip(oldClip);
            g.setStroke(oldStroke);
        }
    }

    private void paintDevToolsHatch(Graphics2D g, Rectangle r, Color color) {
        if (r.width <= 0 || r.height <= 0) return;
        Shape oldClip = g.getClip();
        Stroke oldStroke = g.getStroke();
        try {
            g.clip(r);
            g.setColor(color);
            g.setStroke(new BasicStroke(1f));
            int step = 8;
            for (int x = r.x - r.height; x < r.x + r.width + r.height; x += step) {
                g.drawLine(x, r.y + r.height, x + r.height, r.y);
            }
        } finally {
            g.setClip(oldClip);
            g.setStroke(oldStroke);
        }
    }

    private void drawDevToolsLabel(Graphics2D g, UiDomElement element, Rectangle borderBox, Rectangle anchorBox) {
        String selectorText = selector(element);
        String dimensionText = borderBox.width + " × " + borderBox.height;
        Font mono = HtmlDomDevToolsFonts.mono(12f);
        Font monoBold = mono.deriveFont(Font.BOLD);
        Font icon = HtmlDomDevToolsFonts.icon(12f);
        g.setFont(monoBold);
        FontMetrics selectorMetrics = g.getFontMetrics();
        int selectorW = selectorMetrics.stringWidth(selectorText);
        g.setFont(mono);
        FontMetrics textMetrics = g.getFontMetrics();
        int dimW = textMetrics.stringWidth(dimensionText);
        int labelW = Math.min(getWidth() - 16, 31 + selectorW + 12 + dimW + 12);
        int labelH = 26;
        int labelX = clamp(anchorBox.x, 8, Math.max(8, getWidth() - labelW - 8));
        int labelY = anchorBox.y - labelH - 7;
        if (labelY < 8) labelY = anchorBox.y + anchorBox.height + 7;
        if (labelY + labelH > getHeight() - 8) labelY = Math.max(8, getHeight() - labelH - 8);

        g.setColor(new Color(0, 0, 0, 55));
        g.fillRoundRect(labelX + 2, labelY + 3, labelW, labelH, 6, 6);
        g.setColor(new Color(255, 255, 255, 248));
        g.fillRoundRect(labelX, labelY, labelW, labelH, 6, 6);
        g.setColor(new Color(218, 220, 224));
        g.drawRoundRect(labelX, labelY, labelW - 1, labelH - 1, 6, 6);
        g.setFont(icon);
        g.setColor(new Color(66, 133, 244));
        g.drawString("\uf121", labelX + 8, labelY + 17);
        g.setFont(monoBold);
        g.setColor(new Color(124, 58, 237));
        g.drawString(selectorText, labelX + 28, labelY + 17);
        g.setFont(mono);
        g.setColor(new Color(31, 41, 55));
        g.drawString(dimensionText, labelX + 28 + selectorW + 12, labelY + 17);
    }

    private void fillRounded(Graphics2D g, Rectangle r, int radius, Color color) {
        if (r.width <= 0 || r.height <= 0) return;
        g.setColor(color);
        if (radius > 0) g.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, radius * 2f, radius * 2f));
        else g.fillRect(r.x, r.y, r.width, r.height);
    }

    private void drawRounded(Graphics2D g, Rectangle r, int radius) {
        if (r.width <= 0 || r.height <= 0) return;
        if (radius > 0) g.draw(new RoundRectangle2D.Float(r.x, r.y, Math.max(0, r.width - 1), Math.max(0, r.height - 1), radius * 2f, radius * 2f));
        else g.drawRect(r.x, r.y, Math.max(0, r.width - 1), Math.max(0, r.height - 1));
    }

    private Rectangle expand(Rectangle r, DevToolsInsets inset) {
        return new Rectangle(r.x - inset.left(), r.y - inset.top(), Math.max(0, r.width + inset.left() + inset.right()), Math.max(0, r.height + inset.top() + inset.bottom()));
    }

    private Rectangle inset(Rectangle r, DevToolsInsets inset) {
        return new Rectangle(r.x + inset.left(), r.y + inset.top(), Math.max(0, r.width - inset.left() - inset.right()), Math.max(0, r.height - inset.top() - inset.bottom()));
    }

    private DevToolsInsets cssInsets(UiDomElement element, String group, int fallback) {
        String shorthand = "border".equals(group) ? firstStyle(element, "border-width", "border") : firstStyle(element, group);
        DevToolsInsets parsed = parseCssBox(shorthand, fallback, Math.max(1, rect(element).width));
        int top = cssPixels(firstStyle(element, group + "-top" + ("border".equals(group) ? "-width" : "")), parsed.top(), Math.max(1, rect(element).height));
        int right = cssPixels(firstStyle(element, group + "-right" + ("border".equals(group) ? "-width" : "")), parsed.right(), Math.max(1, rect(element).width));
        int bottom = cssPixels(firstStyle(element, group + "-bottom" + ("border".equals(group) ? "-width" : "")), parsed.bottom(), Math.max(1, rect(element).height));
        int left = cssPixels(firstStyle(element, group + "-left" + ("border".equals(group) ? "-width" : "")), parsed.left(), Math.max(1, rect(element).width));
        return new DevToolsInsets(top, right, bottom, left);
    }

    private DevToolsInsets parseCssBox(String raw, int fallback, int reference) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) return new DevToolsInsets(fallback, fallback, fallback, fallback);
        String[] parts = value.split("\\s+");
        int a = cssPixels(parts.length > 0 ? parts[0] : "", fallback, reference);
        int b = cssPixels(parts.length > 1 ? parts[1] : "", a, reference);
        int c = cssPixels(parts.length > 2 ? parts[2] : "", a, reference);
        int d = cssPixels(parts.length > 3 ? parts[3] : "", b, reference);
        return new DevToolsInsets(a, b, c, d);
    }

    private int fallbackBorderWidth(UiDomElement element) {
        String border = firstStyle(element, "border", "border-style", "border-color");
        return border.isBlank() || "none".equalsIgnoreCase(border.trim()) ? 0 : 1;
    }

    private String firstStyle(UiDomElement element, String... names) {
        for (String name : names) {
            String value = element.style(name, "").trim();
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private int cssPixels(String raw, int fallback, int reference) {
        String value = raw == null ? "" : raw.trim().toLowerCase();
        if (value.isBlank() || "auto".equals(value) || "none".equals(value)) return fallback;
        if ("thin".equals(value)) return 1;
        if ("medium".equals(value)) return 3;
        if ("thick".equals(value)) return 5;
        String first = value.split("\\s+")[0];
        try {
            if (first.endsWith("px")) return Math.round(Float.parseFloat(first.substring(0, first.length() - 2).trim()));
            if (first.endsWith("%")) return Math.round(Float.parseFloat(first.substring(0, first.length() - 1).trim()) * reference / 100f);
            if (first.endsWith("rem") || first.endsWith("em")) return Math.round(Float.parseFloat(first.substring(0, first.length() - 2).trim()) * 16f);
            return Math.round(Float.parseFloat(first));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record DevToolsInsets(int top, int right, int bottom, int left) { }

    private HtmlDomRenderContext renderContext() {
        return new HtmlDomRenderContext(
                dom,
                layout,
                paintEngine,
                textPaintEngine,
                controlPaintEngine,
                overlayPaintEngine,
                scrollbarPaintEngine,
                hitTest,
                focusController.focusedElement(),
                hoveredScrollElement,
                hoveredScrollAxis,
                activeScrollDrag,
                getWidth(),
                getHeight()
        );
    }

    private HtmlDomScrollbarPaintEngine.Context scrollbarContext() {
        return renderContext().scrollbars();
    }

    private void paintScrollbars(Graphics2D g, UiDomElement element, Rectangle r) {
        scrollbarPaintEngine.paintScrollbars(g, element, r, scrollBox(element), scrollbarContext());
    }


    private boolean clipsContents(UiDomElement element) {
        return paintEngine.clipsContents(element);
    }


    private Shape clipShape(UiDomElement element, Rectangle rect) {
        return paintEngine.clipShape(element, rect);
    }


    private HtmlDomOverlayPaintEngine.Context overlayContext() {
        return renderContext().overlays();
    }

    private void paintOverlayElements(Graphics2D g) {
        overlayPaintEngine.paintOverlays(g, overlayContext());
    }


    private HtmlDomControlPaintEngine.Context controlContext() {
        return renderContext().controls();
    }

    private HtmlDomTextPaintEngine.Context textContext() {
        return renderContext().text();
    }

    private void paintSpecialOrText(Graphics2D g, UiDomElement element, Rectangle r) {
        if (controlPaintEngine.paintControl(g, element, r, controlContext())) return;
        textPaintEngine.paintSpecialOrText(g, element, r, textContext());
    }


    @Override public UiCssScrollBox scrollBox(UiDomElement element) {
        if (layout == null || element == null) return null;
        UiCssScrollBox box = layout.scrollBox(element).orElse(null);
        return box == null ? null : scrollController.resolve(element, box);
    }


    private boolean positioned(UiDomElement element) {
        return positioningController.positioned(element);
    }

    @Override public boolean clickable(UiDomElement element) {
        if (element == null) return false;
        return "button".equals(element.tagName())
                || "button".equals(element.attribute("role", ""))
                || element.classList().contains("button")
                || !element.data("action", "").isBlank()
                || !element.data("tab", "").isBlank();
    }

    private boolean skip(UiDomElement element) {
        String tag = element.tagName();
        if (tag.equals("head") || tag.equals("title") || tag.equals("style") || tag.equals("option")) return true;
        return "none".equals(element.style("display", ""));
    }

    @Override public Rectangle rect(UiDomElement element) {
        return positioningController.rect(element, layout, getWidth(), getHeight(), this::scrollBox);
    }

    private String selector(UiDomElement element) {
        String classes = String.join(" ", element.classList().values());
        return element.tagName() + (element.id().isBlank() ? "" : "#" + element.id()) + (classes.isBlank() ? "" : "." + classes.replace(' ', '.'));
    }


    private float opacity(UiDomElement element) {
        String raw = element.style("opacity", "").trim();
        if (raw.isBlank()) return 1f;
        try { return Math.max(0f, Math.min(1f, Float.parseFloat(raw))); }
        catch (RuntimeException ignored) { return 1f; }
    }


    private record ScrollAnchor(UiDomElement scroller, UiDomElement element, int x, int y, float scrollX, float scrollY) { }

}
