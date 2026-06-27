package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.devtools.HtmlDomDevToolsHitNode;
import dev.takesome.htmldom.devtools.HtmlDomDevToolsPaintNode;
import dev.takesome.htmldom.devtools.HtmlDomDevToolsSnapshot;
import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;
import dev.takesome.htmldom.dom.UiDomNode;
import dev.takesome.htmldom.dom.UiDomText;
import dev.takesome.htmldom.dom.UiDomTraversal;
import dev.takesome.htmldom.markup.UiMarkupParser;

import javax.swing.JDialog;
import javax.swing.WindowConstants;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.Point;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Chrome-like Elements DOM viewer painted by HtmlDom itself: no Swing tables/tree sheet. */
public final class HtmlDomDevToolsWindow {
    private static final Color BG = new Color(31, 32, 35);
    private static final Color PANEL = new Color(38, 39, 42);
    private static final Color CARD = new Color(43, 44, 48);
    private static final Color BORDER = new Color(78, 80, 86);
    private static final Color SELECT = new Color(44, 74, 128);
    private static final Color HOVER = new Color(55, 57, 64);
    private static final Color TEXT = new Color(232, 234, 237);
    private static final Color MUTED = new Color(154, 160, 166);
    private static final Color TAG = new Color(138, 180, 248);
    private static final Color ATTR = new Color(253, 214, 99);
    private static final Color VALUE = new Color(197, 138, 249);
    private static final Color PSEUDO = new Color(129, 201, 149);
    private static final Color ACCENT = new Color(0, 136, 255);
    private static final String I_CHEVRON_RIGHT = "\uf105";
    private static final String I_CHEVRON_DOWN = "\uf107";
    private static final String I_PLUS = "\uf067";
    private static final String I_CODE = "\uf121";
    private static final String I_CLONE = "\uf24d";
    private static final String I_TRASH = "\uf1f8";
    private static final String I_SCISSORS = "\uf0c4";
    private static final String I_COPY = "\uf0c5";
    private static final String I_PASTE = "\uf0ea";
    private static final String I_UNDO = "\uf0e2";
    private static final String I_REDO = "\uf01e";
    private static final String I_SAVE = "\uf0c7";
    private static final String I_TIMES = "\uf00d";
    private static final String I_MINUS = "\uf068";
    private static final String I_DROPPER = "\uf1fb";
    private static final int[] COLOR_PICKER_PRESETS = {
            0xEF4444, 0xE91E63, 0x8B5CF6, 0x6366F1, 0x3B82F6, 0x0EA5E9, 0x06B6D4,
            0x10B981, 0x22C55E, 0xA3E635, 0xFACC15, 0xFDE047, 0xF97316, 0xF4511E,
            0x795548, 0x94A3B8, 0x64748B
    };
    private static final int HEADER = 44;
    private static final int LEFT_W = 540;
    private static final int ROW_H = 23;
    private static final int TAB_H = 34;
    private static final int MENU_W = 270;
    private static final int MENU_ROW_H = 32;

    private final HtmlDomSwingPanel inspected;
    private final HtmlDomConfig config;
    private final Canvas canvas = new Canvas();
    private final Set<Integer> collapsedNodeIds = new HashSet<>();
    private Window frame;
    private float domScroll;
    private float rightScroll;
    private int selectedNodeId;
    private int hoverNodeId;
    private String activeTab = "Computed";
    private HtmlDomDevToolsNodeSnapshot.NodeCopy clipboard;
    private boolean dirty;
    private String saveStatus = "";
    private String editingComputedProperty = "";
    private String editingComputedValue = "";
    private String editingComputedOriginalValue = "";
    private String editingComputedLastAppliedValue = "";
    private boolean editingComputedLiveApplied;
    private String keywordPopupProperty = "";
    private List<String> keywordPopupOptions = List.of();
    private int keywordPopupAnchorX;
    private int keywordPopupAnchorY;
    private String colorPickerProperty = "";
    private String colorPickerOriginalValue = "";
    private Color colorPickerColor = Color.WHITE;
    private float colorPickerHue;
    private float colorPickerSaturation;
    private float colorPickerBrightness = 1f;
    private String colorPickerEditingChannel = "";
    private String colorPickerEditingValue = "";
    private int colorPickerAnchorX;
    private int colorPickerAnchorY;
    private boolean colorPickerUndoCaptured;
    private boolean htmlEditMode;
    private int htmlEditNodeId;
    private String htmlEditText = "";
    private String htmlEditError = "";
    private int htmlCaretIndex;
    private int htmlSelectionAnchor;
    private int htmlSelectionFocus;
    private final ArrayList<HtmlDomDevToolsNodeSnapshot.NodeCopy> undoStack = new ArrayList<>();
    private final ArrayList<HtmlDomDevToolsNodeSnapshot.NodeCopy> redoStack = new ArrayList<>();
    private final ArrayList<DomRow> rows = new ArrayList<>();

    public HtmlDomDevToolsWindow(HtmlDomSwingPanel inspected, HtmlDomConfig config) {
        this.inspected = inspected;
        this.config = config == null ? HtmlDomConfig.defaults() : config;
    }

    public void open() {
        if (SwingUtilities.isEventDispatchThread()) openOnEdt();
        else SwingUtilities.invokeLater(this::openOnEdt);
    }

    public void refresh() {
        if (frame == null || !frame.isDisplayable()) return;
        if (SwingUtilities.isEventDispatchThread()) canvas.repaint();
        else SwingUtilities.invokeLater(canvas::repaint);
    }

    public void close() {
        if (SwingUtilities.isEventDispatchThread()) closeOnEdt();
        else SwingUtilities.invokeLater(this::closeOnEdt);
    }

    private void closeOnEdt() {
        Window current = frame;
        frame = null;
        if (current == null) return;
        current.setAlwaysOnTop(false);
        current.setVisible(false);
        current.dispose();
    }

    private Window createToolWindow() {
        JDialog dialog = switch (config.devToolsWindowType()) {
            case STANDALONE_FRAME -> new JDialog((java.awt.Frame) null, "HtmlDom DevTools — Elements", false);
            case OWNERLESS_DIALOG -> new JDialog((java.awt.Frame) null, "HtmlDom DevTools — Elements", false);
            case OWNED_DIALOG -> createOwnedDialog();
        };
        dialog.setUndecorated(true);
        return dialog;
    }

    private JDialog createOwnedDialog() {
        Window owner = SwingUtilities.getWindowAncestor(inspected);
        if (owner == null) {
            return new JDialog((java.awt.Frame) null, "HtmlDom DevTools — Elements", false);
        }
        return new JDialog(owner, "HtmlDom DevTools — Elements", Dialog.ModalityType.MODELESS);
    }


    private void setToolContentPane(Window window) {
        if (window instanceof JDialog dialog) {
            dialog.setContentPane(canvas);
        }
    }

    private void setToolCloseOperation(Window window) {
        if (window instanceof JDialog dialog) {
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }
    }

    private void openOnEdt() {
        if (frame == null || !frame.isDisplayable()) {
            frame = createToolWindow();
            frame.setName("HtmlDom DevTools");
            frame.setAutoRequestFocus(true);
            setToolCloseOperation(frame);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent event) {
                    frame = null;
                }
            });
            setToolContentPane(frame);
            frame.setSize(1220, 780);
            frame.setMinimumSize(new Dimension(900, 560));
            frame.setLocationByPlatform(true);
        }
        frame.setAlwaysOnTop(alwaysOnTop());
        if (selectedNodeId <= 0) selectedNodeId = inspected.document().documentElement().nodeId();
        canvas.rebuildRows();
        canvas.repaint();
        frame.setVisible(true);
        raiseDevToolsWindow();
        SwingUtilities.invokeLater(this::raiseDevToolsWindow);
    }

    private boolean alwaysOnTop() {
        return config.devToolsZOrder().alwaysOnTop();
    }

    private void raiseDevToolsWindow() {
        if (frame == null || !frame.isShowing() || !config.devToolsZOrder().bringToFrontOnOpen()) return;
        frame.toFront();
        frame.requestFocus();
        canvas.requestFocusInWindow();
    }

    private final class Canvas extends JPanel {
        private final Font mono12 = HtmlDomDevToolsFonts.mono(12f);
        private final Font mono13 = HtmlDomDevToolsFonts.mono(13f);
        private final Font monoBold = HtmlDomDevToolsFonts.mono(13f).deriveFont(Font.BOLD);
        private final Font icon12 = HtmlDomDevToolsFonts.icon(12f);
        private final Font icon14 = HtmlDomDevToolsFonts.icon(14f);
        private final ArrayList<RowHit> rowHits = new ArrayList<>();
        private final ArrayList<ToggleHit> toggleHits = new ArrayList<>();
        private final ArrayList<TabHit> tabHits = new ArrayList<>();
        private final ArrayList<ToolButtonHit> toolButtonHits = new ArrayList<>();
        private final ArrayList<WindowButtonHit> windowButtonHits = new ArrayList<>();
        private final ArrayList<ComputedHit> computedHits = new ArrayList<>();
        private final ArrayList<KeywordOptionHit> keywordOptionHits = new ArrayList<>();
        private final ArrayList<ColorPickerHit> colorPickerHits = new ArrayList<>();
        private final ArrayList<HtmlEditButtonHit> htmlEditButtonHits = new ArrayList<>();
        private final ArrayList<HtmlLineHit> htmlLineHits = new ArrayList<>();
        private final ArrayList<MenuHit> menuHits = new ArrayList<>();
        private final LinkedHashMap<Integer, LinkedHashMap<String, String>> computedStyleCache = new LinkedHashMap<>();
        private Rectangle htmlEditorRect = new Rectangle();
        private Rectangle keywordPopupRect = new Rectangle();
        private Rectangle colorPickerRect = new Rectangle();
        private boolean htmlSelecting;
        private boolean contextMenuOpen;
        private int contextMenuX;
        private int contextMenuY;
        private int contextNodeId;
        private boolean draggingWindow;
        private Point windowDragStartScreen = new Point();
        private Point windowDragStartLocation = new Point();

        Canvas() {
            setBackground(BG);
            setOpaque(true);
            setFocusable(true);
            setCursor(Cursor.getDefaultCursor());
            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { handleMove(e); }
                @Override public void mouseDragged(MouseEvent e) { handleDrag(e); }
                @Override public void mouseExited(MouseEvent e) { clearHover(); }
                @Override public void mouseClicked(MouseEvent e) { handleClick(e); }
                @Override public void mousePressed(MouseEvent e) {
                    if (popup(e)) openContextMenu(e);
                    else if (!beginWindowDrag(e)) beginHtmlSelection(e);
                }
                @Override public void mouseReleased(MouseEvent e) {
                    if (popup(e)) openContextMenu(e);
                    else { htmlSelecting = false; draggingWindow = false; }
                }
                @Override public void mouseWheelMoved(MouseWheelEvent e) { handleWheel(e); }
            };
            addMouseMotionListener(mouse);
            addMouseListener(mouse);
            addMouseWheelListener(mouse);
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) { handleKeyPressed(e); }
                @Override public void keyTyped(KeyEvent e) { handleKeyTyped(e); }
            });
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            rebuildRows();
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                paintChrome(g);
            } finally {
                g.dispose();
            }
        }

        private void paintChrome(Graphics2D g) {
            int w = getWidth();
            int h = getHeight();
            rowHits.clear();
            toggleHits.clear();
            tabHits.clear();
            toolButtonHits.clear();
            windowButtonHits.clear();
            computedHits.clear();
            keywordOptionHits.clear();
            colorPickerHits.clear();
            keywordPopupRect = new Rectangle();
            colorPickerRect = new Rectangle();
            htmlEditButtonHits.clear();
            htmlLineHits.clear();
            menuHits.clear();
            g.setColor(BG);
            g.fillRect(0, 0, w, h);
            drawHeader(g, w);
            drawDomTree(g, h);
            drawInspector(g, w, h);
            if (keywordPopupOpen()) drawKeywordPopup(g);
            if (colorPickerOpen()) drawColorPicker(g);
            if (contextMenuOpen) drawContextMenu(g);
        }

        private void drawHeader(Graphics2D g, int w) {
            g.setColor(new Color(35, 36, 40));
            g.fillRect(0, 0, w, HEADER);
            g.setColor(BORDER);
            g.drawLine(0, HEADER - 1, w, HEADER - 1);
            g.setFont(monoBold);
            g.setColor(TEXT);
            g.drawString("HtmlDom DevTools", 16, 27);
            HtmlDomDevToolsSnapshot snapshot = inspected.devToolsSnapshot();
            g.setFont(mono12);
            g.setColor(MUTED);
            g.drawString("Elements  ·  DOM " + snapshot.nodeCount() + " nodes  ·  boxes " + snapshot.layoutBoxCount() + "  ·  events " + inspected.devToolsEventLog().size() + (dirty ? "  ·  modified" : ""), 174, 27);
            if (!saveStatus.isBlank()) {
                g.setColor(dirty ? new Color(253, 214, 99) : new Color(129, 201, 149));
                g.drawString(clip(saveStatus, 52), Math.max(400, w - 780), 27);
            }
            int windowX = Math.max(0, w - 86);
            drawWindowButton(g, windowX, 8, 34, I_MINUS, WindowCommand.MINIMIZE);
            drawWindowButton(g, windowX + 40, 8, 34, I_TIMES, WindowCommand.CLOSE);
            int x = Math.max(520, w - 390);
            drawToolButton(g, x, 8, 82, I_UNDO, "Undo", ToolCommand.UNDO, !undoStack.isEmpty());
            drawToolButton(g, x + 88, 8, 82, I_REDO, "Redo", ToolCommand.REDO, !redoStack.isEmpty());
            drawToolButton(g, x + 176, 8, 96, I_SAVE, "Save", ToolCommand.SAVE, dirty);
        }

        private void drawWindowButton(Graphics2D g, int x, int y, int w, String icon, WindowCommand command) {
            Rectangle r = new Rectangle(x, y, w, 28);
            windowButtonHits.add(new WindowButtonHit(r, command));
            Point mouse = getMousePositionSafe();
            boolean hovered = mouse != null && r.contains(mouse);
            boolean close = command == WindowCommand.CLOSE;
            g.setColor(hovered ? (close ? new Color(153, 27, 27) : new Color(55, 57, 64)) : new Color(43, 44, 48));
            g.fillRoundRect(x, y, w, 28, 10, 10);
            g.setColor(hovered && close ? new Color(248, 113, 113) : BORDER);
            g.drawRoundRect(x, y, w - 1, 27, 10, 10);
            drawIcon(g, icon, x + 11, y + 19, hovered && close ? Color.WHITE : TEXT, icon14);
        }

        private void drawToolButton(Graphics2D g, int x, int y, int w, String icon, String label, ToolCommand command, boolean enabled) {
            Rectangle r = new Rectangle(x, y, w, 28);
            toolButtonHits.add(new ToolButtonHit(r, command, enabled));
            Point mouse = getMousePositionSafe();
            boolean hovered = enabled && mouse != null && r.contains(mouse);
            g.setColor(hovered ? new Color(55, 57, 64) : new Color(43, 44, 48));
            g.fillRoundRect(x, y, w, 28, 10, 10);
            g.setColor(enabled ? BORDER : new Color(58, 60, 64));
            g.drawRoundRect(x, y, w - 1, 27, 10, 10);
            drawIcon(g, icon, x + 10, y + 19, enabled ? TEXT : new Color(100, 104, 110), icon14);
            g.setFont(mono12);
            g.setColor(enabled ? TEXT : new Color(100, 104, 110));
            g.drawString(label, x + 31, y + 19);
        }

        private void drawDomTree(Graphics2D g, int h) {
            g.setColor(PANEL);
            g.fillRect(0, HEADER, LEFT_W, h - HEADER);
            g.setColor(BORDER);
            g.drawLine(LEFT_W, HEADER, LEFT_W, h);
            g.setFont(mono13);
            int y = HEADER + 10 - Math.round(domScroll);
            for (DomRow row : rows) {
                if (y + ROW_H >= HEADER && y <= h) drawDomRow(g, row, y);
                y += ROW_H;
            }
        }

        private void drawDomRow(Graphics2D g, DomRow row, int y) {
            int id = row.node().nodeId();
            if (id == selectedNodeId) {
                g.setColor(SELECT);
                g.fillRect(0, y, LEFT_W, ROW_H);
            } else if (id == hoverNodeId) {
                g.setColor(HOVER);
                g.fillRect(0, y, LEFT_W, ROW_H);
            }
            rowHits.add(new RowHit(new Rectangle(0, y, LEFT_W, ROW_H), id));
            int x = 10 + row.depth() * 18;
            boolean expandable = expandable(row.node());
            if (expandable) {
                String icon = collapsedNodeIds.contains(id) ? I_CHEVRON_RIGHT : I_CHEVRON_DOWN;
                drawIcon(g, icon, x, y + 16, MUTED, icon12);
                toggleHits.add(new ToggleHit(new Rectangle(x - 4, y, 20, ROW_H), id));
            }
            x += 18;
            if (row.node() instanceof UiDomText text) {
                draw(g, "#text", x, y + 16, MUTED);
                draw(g, " \"" + clip(text.text().trim(), 80) + "\"", x + 48, y + 16, TEXT);
                return;
            }
            UiDomElement e = (UiDomElement) row.node();
            x = draw(g, "<" + e.tagName(), x, y + 16, TAG);
            for (Map.Entry<String, String> attr : e.attributes().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                x = draw(g, " " + attr.getKey(), x, y + 16, ATTR);
                x = draw(g, "=", x, y + 16, MUTED);
                x = draw(g, "\"" + clip(attr.getValue(), 70) + "\"", x, y + 16, VALUE);
            }
            if (!e.pseudoClasses().isEmpty()) x = draw(g, " :" + String.join(":", e.pseudoClasses()), x, y + 16, PSEUDO);
            draw(g, ">", x, y + 16, TAG);
            if (expandable && collapsedNodeIds.contains(id)) draw(g, " … </" + e.tagName() + ">", x + 10, y + 16, MUTED);
        }

        private void drawInspector(Graphics2D g, int w, int h) {
            int x = LEFT_W + 1;
            int contentW = w - x;
            g.setColor(BG);
            g.fillRect(x, HEADER, contentW, h - HEADER);
            UiDomNode selected = nodeById(selectedNodeId);
            drawSelectedHeader(g, selected, x, contentW);
            drawTabs(g, x, contentW);
            drawActivePanel(g, selected, x + 12, HEADER + 86, contentW - 24, h - HEADER - 98);
        }

        private void drawSelectedHeader(Graphics2D g, UiDomNode selected, int x, int w) {
            g.setColor(CARD);
            g.fillRect(x, HEADER, w, 50);
            g.setColor(BORDER);
            g.drawLine(x, HEADER + 49, x + w, HEADER + 49);
            g.setFont(monoBold);
            g.setColor(selected == null ? MUTED : TEXT);
            g.drawString(selected == null ? "No node selected" : nodeTitle(selected), x + 14, HEADER + 21);
            if (selected != null) {
                g.setFont(mono12);
                g.setColor(MUTED);
                g.drawString(domPath(selected), x + 14, HEADER + 40);
            }
        }

        private void drawTabs(Graphics2D g, int x, int w) {
            String[] tabs = {"Styles", "Computed", "Current CSS", "HTML", "Layout", "Attributes", "Events", "Path"};
            int tx = x;
            int y = HEADER + 50;
            g.setColor(PANEL);
            g.fillRect(x, y, w, TAB_H);
            for (String tab : tabs) {
                int tw = Math.max(74, tab.length() * 9 + 28);
                boolean active = tab.equals(activeTab);
                if (active) {
                    g.setColor(BG);
                    g.fillRect(tx, y, tw, TAB_H);
                    g.setColor(ACCENT);
                    g.fillRect(tx, y + TAB_H - 3, tw, 3);
                }
                g.setFont(mono12);
                g.setColor(active ? TEXT : MUTED);
                g.drawString(tab, tx + 14, y + 22);
                tabHits.add(new TabHit(new Rectangle(tx, y, tw, TAB_H), tab));
                tx += tw;
            }
            g.setColor(BORDER);
            g.drawLine(x, y + TAB_H - 1, x + w, y + TAB_H - 1);
        }

        private void drawActivePanel(Graphics2D g, UiDomNode selected, int x, int y, int w, int h) {
            g.setClip(x, y, w, h);
            int rowY = y + 8 - Math.round(rightScroll);
            if (selected == null) {
                drawLine(g, x, rowY, "No selected node", MUTED);
                g.setClip(null);
                return;
            }
            UiDomElement e = selected instanceof UiDomElement element ? element : null;
            switch (activeTab) {
                case "Styles" -> rowY = drawStyles(g, e, x, rowY, w);
                case "Computed" -> rowY = drawComputed(g, e, x, rowY, w);
                case "Current CSS" -> rowY = drawCurrentCss(g, e, x, rowY, w);
                case "HTML" -> rowY = drawHtmlEditor(g, selected, x, rowY, w, h);
                case "Layout" -> rowY = drawLayout(g, selected, x, rowY, w);
                case "Attributes" -> rowY = drawAttributes(g, selected, e, x, rowY, w);
                case "Events" -> rowY = drawEvents(g, x, rowY, w);
                default -> drawWrapped(g, x, rowY, w, domPath(selected), TEXT);
            }
            g.setClip(null);
        }

        private int drawHtmlEditor(Graphics2D g, UiDomNode selected, int x, int y, int w, int h) {
            if (selected == null) return drawKV(g, x, y, "html", "no selected node", false);
            if (!htmlEditMode || htmlEditNodeId != selected.nodeId()) startHtmlEdit(selected);
            htmlEditButtonHits.clear();
            htmlLineHits.clear();
            int editorH = Math.max(80, h - 58);
            Rectangle editor = new Rectangle(x, y, w, editorH);
            htmlEditorRect = editor;
            g.setColor(new Color(15, 23, 34));
            g.fillRoundRect(editor.x, editor.y, editor.width, editor.height, 10, 10);
            g.setColor(new Color(75, 85, 99));
            g.drawRoundRect(editor.x, editor.y, editor.width - 1, editor.height - 1, 10, 10);

            Shape oldClip = g.getClip();
            g.setClip(editor.x + 1, editor.y + 1, editor.width - 2, editor.height - 2);
            g.setFont(mono12);
            FontMetrics fm = g.getFontMetrics();
            int charW = Math.max(1, fm.charWidth('m'));
            int lineH = 18;
            int textX = editor.x + 54;
            int numberX = editor.x + 8;
            String[] lines = htmlEditText.split("\\R", -1);
            int rowTop = editor.y + 6 - Math.round(rightScroll);
            int offset = 0;
            int selStart = Math.min(htmlSelectionAnchor, htmlSelectionFocus);
            int selEnd = Math.max(htmlSelectionAnchor, htmlSelectionFocus);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineStart = offset;
                int lineEnd = lineStart + line.length();
                int baseline = rowTop + 14;
                Rectangle row = new Rectangle(editor.x + 1, rowTop, editor.width - 2, lineH);
                htmlLineHits.add(new HtmlLineHit(row, textX, baseline, lineStart, lineEnd, line));
                if (rowTop + lineH >= editor.y && rowTop <= editor.y + editor.height) {
                    g.setColor((i & 1) == 0 ? new Color(17, 25, 37) : new Color(18, 27, 40));
                    g.fillRect(row.x, row.y, row.width, row.height);
                    g.setColor(new Color(31, 41, 55));
                    g.drawLine(row.x, row.y + row.height - 1, row.x + row.width, row.y + row.height - 1);
                    g.setColor(new Color(75, 85, 99));
                    g.drawString(Integer.toString(i + 1), numberX, baseline);
                    drawHtmlSelection(g, textX, rowTop, lineH, charW, lineStart, lineEnd, selStart, selEnd);
                    HtmlDomDevToolsHtmlSyntaxPainter.drawLine(g, line, textX, baseline, mono12, TEXT, TAG, ATTR, VALUE, MUTED);
                    if (htmlCaretIndex >= lineStart && htmlCaretIndex <= lineEnd) {
                        int cx = textX + Math.max(0, htmlCaretIndex - lineStart) * charW;
                        g.setColor(new Color(226, 232, 240));
                        g.drawLine(cx, rowTop + 3, cx, rowTop + lineH - 3);
                    }
                }
                rowTop += lineH;
                offset = lineEnd + 1;
            }
            if (htmlEditText.isBlank()) {
                htmlLineHits.add(new HtmlLineHit(new Rectangle(editor.x + 1, editor.y + 6, editor.width - 2, lineH), textX, editor.y + 20, 0, 0, ""));
                g.setColor(new Color(75, 85, 99));
                g.drawString("1", numberX, editor.y + 20);
                g.setColor(new Color(226, 232, 240));
                g.drawLine(textX, editor.y + 9, textX, editor.y + 21);
            }
            g.setClip(oldClip);

            if (!htmlEditError.isBlank()) {
                g.setFont(mono12);
                g.setColor(new Color(248, 113, 113));
                g.drawString(clip(htmlEditError, 150), editor.x + 8, editor.y + editor.height - 10);
            }
            int by = y + h - 38;
            int applyW = 132;
            int cancelW = 118;
            Rectangle apply = new Rectangle(x + w - applyW - cancelW - 18, by, applyW, 30);
            Rectangle cancel = new Rectangle(x + w - cancelW - 8, by, cancelW, 30);
            htmlEditButtonHits.add(new HtmlEditButtonHit(apply, HtmlEditCommand.APPLY));
            htmlEditButtonHits.add(new HtmlEditButtonHit(cancel, HtmlEditCommand.CANCEL));
            drawHtmlEditButton(g, apply, "Применить", new Color(22, 163, 74), new Color(187, 247, 208));
            drawHtmlEditButton(g, cancel, "Отменить", new Color(185, 28, 28), new Color(254, 202, 202));
            return y + h;
        }

        private void drawHtmlSelection(Graphics2D g, int textX, int rowTop, int lineH, int charW, int lineStart, int lineEnd, int selStart, int selEnd) {
            if (selEnd <= selStart) return;
            int from = Math.max(selStart, lineStart);
            int to = Math.min(selEnd, lineEnd);
            if (to <= from) return;
            int sx = textX + (from - lineStart) * charW;
            int sw = Math.max(charW, (to - from) * charW);
            g.setColor(new Color(59, 130, 246, 110));
            g.fillRect(sx, rowTop + 2, sw, lineH - 3);
        }

        private void drawHtmlEditButton(Graphics2D g, Rectangle r, String label, Color bg, Color fg) {
            Point mouse = getMousePositionSafe();
            boolean hovered = mouse != null && r.contains(mouse);
            Color fill = hovered ? bg.brighter() : bg;
            g.setColor(new Color(0, 0, 0, hovered ? 70 : 35));
            g.fillRoundRect(r.x + 2, r.y + 3, r.width, r.height, 10, 10);
            g.setColor(fill);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g.setColor(hovered ? Color.WHITE : bg.brighter());
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 10, 10);
            g.setFont(mono12.deriveFont(Font.BOLD));
            g.setColor(fg);
            int textW = g.getFontMetrics().stringWidth(label);
            g.drawString(label, r.x + Math.max(8, (r.width - textW) / 2), r.y + 20);
        }

        private boolean keywordPopupOpen() {
            return !keywordPopupProperty.isBlank() && !keywordPopupOptions.isEmpty();
        }

        private boolean colorPickerOpen() {
            return !colorPickerProperty.isBlank();
        }

        private void openKeywordPopup(ComputedHit hit) {
            if (hit == null) return;
            List<String> options = HtmlDomDevToolsCssValue.keywordOptions(hit.property());
            if (options.isEmpty()) return;
            closeColorPicker();
            cancelComputedEdit();
            keywordPopupProperty = hit.property();
            keywordPopupOptions = options;
            Rectangle anchor = hit.keywordRect().width > 0 ? hit.keywordRect() : hit.valueRect();
            keywordPopupAnchorX = anchor.x;
            keywordPopupAnchorY = anchor.y + anchor.height;
            contextMenuOpen = false;
            requestFocusInWindow();
        }

        private void closeKeywordPopup() {
            keywordPopupProperty = "";
            keywordPopupOptions = List.of();
            keywordPopupRect = new Rectangle();
            keywordOptionHits.clear();
        }

        private void drawKeywordPopup(Graphics2D g) {
            if (!keywordPopupOpen()) return;
            int popupW = 230;
            int rowH = 24;
            int popupH = 10 + keywordPopupOptions.size() * rowH;
            int x = clamp(keywordPopupAnchorX - popupW + 24, LEFT_W + 8, Math.max(LEFT_W + 8, getWidth() - popupW - 8));
            int y = clamp(keywordPopupAnchorY + 3, HEADER + 8, Math.max(HEADER + 8, getHeight() - popupH - 8));
            keywordPopupRect = new Rectangle(x, y, popupW, popupH);
            String current = selectedComputedValue(keywordPopupProperty);
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g.setColor(Color.BLACK);
            g.fillRoundRect(x + 5, y + 6, popupW, popupH, 14, 14);
            g.setComposite(old);
            g.setColor(new Color(248, 250, 252));
            g.fillRoundRect(x, y, popupW, popupH, 14, 14);
            g.setColor(new Color(203, 213, 225));
            g.drawRoundRect(x, y, popupW - 1, popupH - 1, 14, 14);
            Point mouse = getMousePositionSafe();
            int rowY = y + 5;
            for (String option : keywordPopupOptions) {
                Rectangle row = new Rectangle(x + 6, rowY, popupW - 12, rowH);
                keywordOptionHits.add(new KeywordOptionHit(row, keywordPopupProperty, option));
                boolean hovered = mouse != null && row.contains(mouse);
                boolean selected = option.equalsIgnoreCase(current == null ? "" : current.trim());
                if (hovered || selected) {
                    g.setColor(selected ? new Color(219, 234, 254) : new Color(241, 245, 249));
                    g.fillRoundRect(row.x, row.y + 2, row.width, row.height - 4, 8, 8);
                }
                g.setFont(mono12);
                g.setColor(selected ? new Color(30, 64, 175) : new Color(30, 41, 59));
                g.drawString(option, row.x + 10, row.y + 17);
                rowY += rowH;
            }
        }

        private void openColorPicker(ComputedHit hit) {
            if (hit == null) return;
            closeKeywordPopup();
            cancelComputedEdit();
            Color parsed = HtmlDomDevToolsCssValue.parseColor(hit.value());
            colorPickerProperty = hit.property();
            colorPickerOriginalValue = hit.value() == null ? "" : hit.value();
            colorPickerColor = parsed == null ? Color.WHITE : parsed;
            syncColorPickerHsbFromColor();
            colorPickerEditingChannel = "";
            colorPickerEditingValue = "";
            colorPickerUndoCaptured = false;
            Rectangle anchor = hit.swatchRect().width > 0 ? hit.swatchRect() : hit.valueRect();
            colorPickerAnchorX = anchor.x;
            colorPickerAnchorY = anchor.y + anchor.height;
            contextMenuOpen = false;
            requestFocusInWindow();
        }

        private void closeColorPicker() {
            colorPickerProperty = "";
            colorPickerOriginalValue = "";
            colorPickerColor = Color.WHITE;
            colorPickerHue = 0f;
            colorPickerSaturation = 0f;
            colorPickerBrightness = 1f;
            colorPickerEditingChannel = "";
            colorPickerEditingValue = "";
            colorPickerUndoCaptured = false;
            colorPickerRect = new Rectangle();
            colorPickerHits.clear();
        }

        private void drawColorPicker(Graphics2D g) {
            if (!colorPickerOpen()) return;
            int popupW = 238;
            int popupH = 326;
            int x = clamp(colorPickerAnchorX, LEFT_W + 8, Math.max(LEFT_W + 8, getWidth() - popupW - 8));
            int y = clamp(colorPickerAnchorY + 5, HEADER + 8, Math.max(HEADER + 8, getHeight() - popupH - 8));
            colorPickerRect = new Rectangle(x, y, popupW, popupH);

            Composite oldComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f));
            g.setColor(Color.BLACK);
            g.fillRoundRect(x + 4, y + 5, popupW, popupH, 8, 8);
            g.setComposite(oldComposite);
            g.setColor(new Color(248, 250, 252));
            g.fillRoundRect(x, y, popupW, popupH, 6, 6);
            g.setColor(new Color(203, 213, 225));
            g.drawRoundRect(x, y, popupW - 1, popupH - 1, 6, 6);

            Rectangle sv = new Rectangle(x, y, popupW, 170);
            colorPickerHits.add(new ColorPickerHit(sv, "sv"));
            drawSaturationValueSquare(g, sv);
            int svKnobX = sv.x + Math.round(clampUnit(colorPickerSaturation) * (sv.width - 1));
            int svKnobY = sv.y + Math.round((1f - clampUnit(colorPickerBrightness)) * (sv.height - 1));
            drawColorPickerTarget(g, svKnobX, svKnobY);

            int controlsY = y + sv.height + 8;
            Rectangle eyedropper = new Rectangle(x + 12, controlsY + 10, 18, 18);
            colorPickerHits.add(new ColorPickerHit(eyedropper, "eyedropper"));
            drawEyedropper(g, eyedropper);

            Rectangle preview = new Rectangle(x + 48, controlsY + 5, 31, 31);
            drawColorPreviewCircle(g, preview);

            Rectangle hue = new Rectangle(x + 90, controlsY + 7, popupW - 108, 12);
            colorPickerHits.add(new ColorPickerHit(hue, "hue"));
            drawHueTrack(g, hue);
            drawSliderKnob(g, hue.x + Math.round(clampUnit(colorPickerHue) * (hue.width - 1)), hue.y + hue.height / 2);

            Rectangle alpha = new Rectangle(x + 90, controlsY + 25, popupW - 108, 12);
            colorPickerHits.add(new ColorPickerHit(alpha, "alpha"));
            drawAlphaTrack(g, alpha);
            drawSliderKnob(g, alpha.x + Math.round((alpha.width - 1) * (colorPickerColor.getAlpha() / 255f)), alpha.y + alpha.height / 2);

            int fieldY = controlsY + 48;
            int fieldW = 40;
            int fieldGap = 8;
            int fieldX = x + 18;
            drawColorNumberField(g, "r", Integer.toString(colorPickerColor.getRed()), fieldX, fieldY, fieldW);
            drawColorNumberField(g, "g", Integer.toString(colorPickerColor.getGreen()), fieldX + (fieldW + fieldGap), fieldY, fieldW);
            drawColorNumberField(g, "b", Integer.toString(colorPickerColor.getBlue()), fieldX + 2 * (fieldW + fieldGap), fieldY, fieldW);
            drawColorNumberField(g, "a", colorPickerAlphaFieldValue(), fieldX + 3 * (fieldW + fieldGap), fieldY, fieldW);

            drawColorPresetGrid(g, x + 16, fieldY + 54, popupW - 32);
        }

        private void drawSaturationValueSquare(Graphics2D g, Rectangle r) {
            if (r.width <= 0 || r.height <= 0) return;
            BufferedImage image = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
            int maxX = Math.max(1, r.width - 1);
            int maxY = Math.max(1, r.height - 1);
            for (int yy = 0; yy < r.height; yy++) {
                float brightness = 1f - yy / (float) maxY;
                for (int xx = 0; xx < r.width; xx++) {
                    float saturation = xx / (float) maxX;
                    image.setRGB(xx, yy, Color.HSBtoRGB(clampUnit(colorPickerHue), saturation, brightness));
                }
            }
            g.drawImage(image, r.x, r.y, null);
            g.setColor(new Color(0, 0, 0, 38));
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        }

        private void drawHueTrack(Graphics2D g, Rectangle r) {
            for (int xx = 0; xx < r.width; xx++) {
                float hue = xx / (float) Math.max(1, r.width - 1);
                g.setColor(Color.getHSBColor(hue, 1f, 1f));
                g.drawLine(r.x + xx, r.y, r.x + xx, r.y + r.height - 1);
            }
            g.setColor(new Color(148, 163, 184));
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 10, 10);
        }

        private void drawAlphaTrack(Graphics2D g, Rectangle r) {
            drawCheckerboard(g, r, 4);
            Color rgb = colorFromHsb(255);
            for (int xx = 0; xx < r.width; xx++) {
                int alpha = Math.round(xx / (float) Math.max(1, r.width - 1) * 255f);
                g.setColor(new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha));
                g.drawLine(r.x + xx, r.y, r.x + xx, r.y + r.height - 1);
            }
            g.setColor(new Color(148, 163, 184));
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 10, 10);
        }

        private void drawCheckerboard(Graphics2D g, Rectangle r, int cell) {
            int size = Math.max(2, cell);
            for (int yy = r.y; yy < r.y + r.height; yy += size) {
                for (int xx = r.x; xx < r.x + r.width; xx += size) {
                    boolean dark = (((xx - r.x) / size) + ((yy - r.y) / size)) % 2 == 0;
                    g.setColor(dark ? new Color(203, 213, 225) : new Color(241, 245, 249));
                    g.fillRect(xx, yy, Math.min(size, r.x + r.width - xx), Math.min(size, r.y + r.height - yy));
                }
            }
        }

        private void drawColorPickerTarget(Graphics2D g, int cx, int cy) {
            g.setStroke(new BasicStroke(2f));
            g.setColor(Color.WHITE);
            g.drawOval(cx - 6, cy - 6, 12, 12);
            g.setColor(new Color(15, 23, 42));
            g.drawOval(cx - 5, cy - 5, 10, 10);
            g.setStroke(new BasicStroke(1f));
        }

        private void drawSliderKnob(Graphics2D g, int cx, int cy) {
            g.setColor(Color.WHITE);
            g.fillOval(cx - 6, cy - 6, 12, 12);
            g.setColor(new Color(148, 163, 184));
            g.drawOval(cx - 6, cy - 6, 12, 12);
        }

        private void drawColorPreviewCircle(Graphics2D g, Rectangle r) {
            Shape oldClip = g.getClip();
            g.setClip(new Ellipse2D.Float(r.x, r.y, r.width, r.height));
            drawCheckerboard(g, r, 5);
            g.setColor(colorPickerColor);
            g.fillOval(r.x, r.y, r.width, r.height);
            g.setClip(oldClip);
            g.setColor(new Color(203, 213, 225));
            g.drawOval(r.x, r.y, r.width - 1, r.height - 1);
        }

        private void drawEyedropper(Graphics2D g, Rectangle r) {
            Point mouse = getMousePositionSafe();
            boolean hovered = mouse != null && r.contains(mouse);
            g.setFont(icon14);
            g.setColor(hovered ? ACCENT : new Color(15, 23, 42));
            g.drawString(I_DROPPER, r.x + 1, r.y + 15);
        }

        private void drawColorNumberField(Graphics2D g, String channel, String value, int x, int y, int w) {
            Rectangle box = new Rectangle(x, y, w, 24);
            colorPickerHits.add(new ColorPickerHit(box, "field:" + channel));
            boolean editing = channel.equals(colorPickerEditingChannel);
            String display = editing ? colorPickerEditingValue : value;
            g.setColor(editing ? new Color(255, 255, 255) : new Color(248, 250, 252));
            g.fillRoundRect(box.x, box.y, box.width, box.height, 4, 4);
            g.setColor(editing ? ACCENT : new Color(203, 213, 225));
            g.drawRoundRect(box.x, box.y, box.width - 1, box.height - 1, 4, 4);
            g.setFont(mono12);
            g.setColor(new Color(15, 23, 42));
            g.drawString(clip(display, 5), box.x + 7, box.y + 16);
            g.setColor(new Color(100, 116, 139));
            g.drawString(channel.toUpperCase(Locale.ROOT), box.x + 17, box.y + 39);
        }

        private void drawColorPresetGrid(Graphics2D g, int x, int y, int maxW) {
            int cell = 12;
            int gap = 10;
            int columns = Math.max(1, (maxW + gap) / (cell + gap));
            for (int i = 0; i < COLOR_PICKER_PRESETS.length; i++) {
                int col = i % columns;
                int row = i / columns;
                Rectangle swatch = new Rectangle(x + col * (cell + gap), y + row * 20, cell, cell);
                colorPickerHits.add(new ColorPickerHit(swatch, "preset:" + COLOR_PICKER_PRESETS[i]));
                g.setColor(new Color(COLOR_PICKER_PRESETS[i]));
                g.fillRect(swatch.x, swatch.y, swatch.width, swatch.height);
                g.setColor(new Color(203, 213, 225));
                g.drawRect(swatch.x, swatch.y, swatch.width - 1, swatch.height - 1);
            }
        }

        private boolean handleKeywordPopupClick(Point point) {
            if (!keywordPopupOpen()) return false;
            for (KeywordOptionHit hit : keywordOptionHits) {
                if (!hit.rect().contains(point)) continue;
                applyComputedValue(hit.property(), hit.value(), true);
                closeKeywordPopup();
                repaint();
                return true;
            }
            return keywordPopupRect.contains(point);
        }

        private boolean handleColorPickerClick(Point point) {
            if (!colorPickerOpen()) return false;
            for (ColorPickerHit hit : colorPickerHits) {
                if (!hit.rect().contains(point)) continue;
                String channel = hit.channel();
                if (channel.startsWith("field:")) {
                    beginColorFieldEdit(channel.substring("field:".length()));
                    repaint();
                    return true;
                }
                if (!colorPickerEditingChannel.isBlank()) commitColorFieldEdit();
                switch (channel) {
                    case "sv" -> updateColorPickerFromSaturationValue(hit.rect(), point);
                    case "hue" -> updateColorPickerFromHue(hit.rect(), point);
                    case "alpha" -> updateColorPickerFromAlpha(hit.rect(), point);
                    case "eyedropper" -> saveStatus = "eyedropper: click a rendered color swatch or preset";
                    default -> {
                        if (channel.startsWith("preset:")) {
                            int rgb = Integer.parseInt(channel.substring("preset:".length()));
                            colorPickerColor = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, colorPickerColor.getAlpha());
                            syncColorPickerHsbFromColor();
                            applyColorPickerValue();
                        }
                    }
                }
                repaint();
                return true;
            }
            return colorPickerRect.contains(point);
        }

        private void updateColorPickerFromSaturationValue(Rectangle rect, Point point) {
            colorPickerSaturation = unitValue(rect, point.x);
            float brightnessY = rect.height <= 1 ? 0f : clampUnit((point.y - rect.y) / (float) Math.max(1, rect.height - 1));
            colorPickerBrightness = 1f - brightnessY;
            colorPickerColor = colorFromHsb(colorPickerColor.getAlpha());
            applyColorPickerValue();
        }

        private void updateColorPickerFromHue(Rectangle rect, Point point) {
            colorPickerHue = unitValue(rect, point.x);
            colorPickerColor = colorFromHsb(colorPickerColor.getAlpha());
            applyColorPickerValue();
        }

        private void updateColorPickerFromAlpha(Rectangle rect, Point point) {
            int alpha = Math.round(unitValue(rect, point.x) * 255f);
            colorPickerColor = new Color(colorPickerColor.getRed(), colorPickerColor.getGreen(), colorPickerColor.getBlue(), alpha);
            applyColorPickerValue();
        }

        private float unitValue(Rectangle rect, int x) {
            if (rect == null || rect.width <= 1) return 0f;
            return clampUnit((x - rect.x) / (float) Math.max(1, rect.width - 1));
        }

        private Color colorFromHsb(int alpha) {
            int rgb = Color.HSBtoRGB(clampUnit(colorPickerHue), clampUnit(colorPickerSaturation), clampUnit(colorPickerBrightness));
            Color base = new Color(rgb);
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), clamp(alpha, 0, 255));
        }

        private void syncColorPickerHsbFromColor() {
            float[] hsb = Color.RGBtoHSB(colorPickerColor.getRed(), colorPickerColor.getGreen(), colorPickerColor.getBlue(), null);
            colorPickerHue = hsb[0];
            colorPickerSaturation = hsb[1];
            colorPickerBrightness = hsb[2];
        }

        private float clampUnit(float value) {
            return Math.max(0f, Math.min(1f, value));
        }

        private String colorPickerAlphaFieldValue() {
            double alpha = colorPickerColor.getAlpha() / 255.0;
            if (Math.abs(alpha - Math.rint(alpha)) < 0.000_001) return Long.toString(Math.round(alpha));
            String out = String.format(Locale.ROOT, "%.3f", alpha);
            while (out.contains(".") && out.endsWith("0")) out = out.substring(0, out.length() - 1);
            if (out.endsWith(".")) out = out.substring(0, out.length() - 1);
            return out;
        }

        private void beginColorFieldEdit(String channel) {
            colorPickerEditingChannel = switch (channel) {
                case "r", "g", "b", "a" -> channel;
                default -> "";
            };
            colorPickerEditingValue = switch (colorPickerEditingChannel) {
                case "r" -> Integer.toString(colorPickerColor.getRed());
                case "g" -> Integer.toString(colorPickerColor.getGreen());
                case "b" -> Integer.toString(colorPickerColor.getBlue());
                case "a" -> colorPickerAlphaFieldValue();
                default -> "";
            };
            requestFocusInWindow();
        }

        private boolean handleColorPickerKeyPressed(KeyEvent e) {
            if (!colorPickerOpen()) return false;
            if (!colorPickerEditingChannel.isBlank()) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER -> commitColorFieldEdit();
                    case KeyEvent.VK_ESCAPE -> cancelColorFieldEdit();
                    case KeyEvent.VK_BACK_SPACE -> {
                        if (!colorPickerEditingValue.isEmpty()) colorPickerEditingValue = colorPickerEditingValue.substring(0, colorPickerEditingValue.length() - 1);
                    }
                    case KeyEvent.VK_DELETE -> colorPickerEditingValue = "";
                    case KeyEvent.VK_UP -> nudgeColorField(1);
                    case KeyEvent.VK_DOWN -> nudgeColorField(-1);
                    default -> { return false; }
                }
                repaint();
                e.consume();
                return true;
            }
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                closeColorPicker();
                repaint();
                e.consume();
                return true;
            }
            return false;
        }

        private boolean handleColorPickerKeyTyped(KeyEvent e) {
            if (!colorPickerOpen() || colorPickerEditingChannel.isBlank()) return false;
            char ch = e.getKeyChar();
            if (ch == KeyEvent.CHAR_UNDEFINED || ch == 10 || ch == 13 || ch == 8 || ch == 127 || Character.isISOControl(ch)) return false;
            if (Character.isDigit(ch) || ch == '.' && "a".equals(colorPickerEditingChannel)) {
                colorPickerEditingValue += ch;
                repaint();
                e.consume();
                return true;
            }
            return false;
        }

        private void nudgeColorField(int direction) {
            try {
                if ("a".equals(colorPickerEditingChannel)) {
                    double parsed = colorPickerEditingValue.isBlank() ? colorPickerColor.getAlpha() / 255.0 : Double.parseDouble(colorPickerEditingValue);
                    if (parsed <= 1.0) parsed = Math.max(0.0, Math.min(1.0, parsed + direction * 0.01));
                    else parsed = Math.max(0.0, Math.min(255.0, parsed + direction));
                    colorPickerEditingValue = colorPickerEditingValue.contains(".") || parsed <= 1.0 ? String.format(Locale.ROOT, "%.2f", parsed) : Integer.toString((int) Math.round(parsed));
                    return;
                }
                int parsed = colorPickerEditingValue.isBlank() ? 0 : Integer.parseInt(colorPickerEditingValue);
                colorPickerEditingValue = Integer.toString(clamp(parsed + direction, 0, 255));
            } catch (RuntimeException ignored) {
                colorPickerEditingValue = "";
            }
        }

        private void commitColorFieldEdit() {
            if (colorPickerEditingChannel.isBlank()) return;
            try {
                int r = colorPickerColor.getRed();
                int g = colorPickerColor.getGreen();
                int b = colorPickerColor.getBlue();
                int a = colorPickerColor.getAlpha();
                switch (colorPickerEditingChannel) {
                    case "r" -> r = parseRgbField(colorPickerEditingValue);
                    case "g" -> g = parseRgbField(colorPickerEditingValue);
                    case "b" -> b = parseRgbField(colorPickerEditingValue);
                    case "a" -> a = parseAlphaField(colorPickerEditingValue);
                    default -> { }
                }
                colorPickerColor = new Color(r, g, b, a);
                syncColorPickerHsbFromColor();
                applyColorPickerValue();
                colorPickerEditingChannel = "";
                colorPickerEditingValue = "";
            } catch (RuntimeException ex) {
                saveStatus = "invalid color channel: " + colorPickerEditingChannel;
            }
        }

        private void cancelColorFieldEdit() {
            colorPickerEditingChannel = "";
            colorPickerEditingValue = "";
        }

        private int parseRgbField(String raw) {
            if (raw == null || raw.isBlank()) return 0;
            return clamp(Math.round(Float.parseFloat(raw.trim())), 0, 255);
        }

        private int parseAlphaField(String raw) {
            if (raw == null || raw.isBlank()) return 255;
            float parsed = Float.parseFloat(raw.trim());
            if (parsed <= 1f) return clamp(Math.round(parsed * 255f), 0, 255);
            return clamp(Math.round(parsed), 0, 255);
        }

        private void applyColorPickerValue() {
            if (colorPickerProperty.isBlank()) return;
            if (!colorPickerUndoCaptured) {
                undoStack.add(HtmlDomDevToolsNodeSnapshot.copy(inspected.document().documentElement()));
                while (undoStack.size() > 128) undoStack.remove(0);
                redoStack.clear();
                colorPickerUndoCaptured = true;
            }
            applyComputedValue(colorPickerProperty, HtmlDomDevToolsCssValue.formatColor(colorPickerColor), false);
            saveStatus = "runtime color: " + colorPickerProperty;
        }

        private void drawContextMenu(Graphics2D g) {
            List<MenuItem> items = menuItems();
            int h = 12 + items.size() * MENU_ROW_H;
            int x = clamp(contextMenuX, 8, Math.max(8, getWidth() - MENU_W - 8));
            int y = clamp(contextMenuY, 8, Math.max(8, getHeight() - h - 8));
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            g.setColor(Color.BLACK);
            g.fillRoundRect(x + 5, y + 6, MENU_W, h, 18, 18);
            g.setComposite(old);
            g.setColor(new Color(248, 250, 252));
            g.fillRoundRect(x, y, MENU_W, h, 18, 18);
            g.setColor(new Color(218, 220, 224));
            g.drawRoundRect(x, y, MENU_W - 1, h - 1, 18, 18);
            int rowY = y + 6;
            Point mouse = getMousePositionSafe();
            for (MenuItem item : items) {
                if (item.separator()) {
                    g.setColor(new Color(224, 224, 224));
                    g.drawLine(x + 14, rowY + MENU_ROW_H / 2, x + MENU_W - 14, rowY + MENU_ROW_H / 2);
                    rowY += MENU_ROW_H;
                    continue;
                }
                Rectangle r = new Rectangle(x + 6, rowY, MENU_W - 12, MENU_ROW_H);
                menuHits.add(new MenuHit(r, item.command(), item.enabled()));
                boolean hovered = mouse != null && r.contains(mouse);
                if (hovered && item.enabled()) {
                    g.setColor(new Color(232, 240, 254));
                    g.fillRoundRect(r.x, r.y + 2, r.width, r.height - 4, 10, 10);
                }
                Color fg = item.enabled() ? new Color(32, 33, 36) : new Color(156, 163, 175);
                drawIcon(g, item.icon(), x + 20, rowY + 21, fg, icon14);
                g.setFont(mono12);
                g.setColor(fg);
                g.drawString(item.label(), x + 50, rowY + 21);
                rowY += MENU_ROW_H;
            }
        }

        private int drawStyles(Graphics2D g, UiDomElement e, int x, int y, int w) {
            if (e == null) return drawKV(g, x, y, "style", "no inline style", false);
            String raw = e.attribute("style", "").trim();
            if (raw.isBlank()) return drawKV(g, x, y, "inline style", "—", false);
            for (String item : raw.split(";")) {
                int c = item.indexOf(':');
                if (c > 0) y = drawKV(g, x, y, item.substring(0, c).trim(), item.substring(c + 1).trim(), false);
            }
            return y;
        }

        private int drawComputed(Graphics2D g, UiDomElement e, int x, int y, int w) {
            if (e == null) return drawKV(g, x, y, "computed", "text node", false);
            LinkedHashMap<String, String> style = stableComputedStyle(e);
            if (style.isEmpty()) return drawKV(g, x, y, "computed", "waiting for layout snapshot", false);
            for (Map.Entry<String, String> entry : style.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                y = drawComputedKV(g, x, y, entry.getKey(), entry.getValue());
            }
            return y;
        }

        private int drawCurrentCss(Graphics2D g, UiDomElement e, int x, int y, int w) {
            if (e == null) return drawKV(g, x, y, "current css", "text node", false);
            LinkedHashMap<String, String> style = stableComputedStyle(e);
            if (style.isEmpty()) return drawKV(g, x, y, "current css", "waiting for layout snapshot", false);
            ArrayList<String> lines = new ArrayList<>();
            lines.add(selector(e) + " {");
            for (Map.Entry<String, String> entry : style.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                lines.add("    " + entry.getKey() + ": " + entry.getValue() + ";");
            }
            lines.add("}");
            int lineH = 20;
            int numberW = 42;
            int rowIndex = 1;
            for (String line : lines) {
                Rectangle row = new Rectangle(x, y, Math.max(0, getWidth() - x - 18), lineH);
                if (y > -80 && y < getHeight() + 80) {
                    g.setColor((rowIndex & 1) == 0 ? new Color(33, 34, 38) : new Color(36, 37, 40));
                    g.fillRect(row.x, row.y, row.width, row.height);
                    g.setFont(mono12);
                    g.setColor(new Color(98, 103, 113));
                    g.drawString(Integer.toString(rowIndex), x + 8, y + 15);
                    HtmlDomDevToolsCssSyntaxPainter.drawLine(g, line, x + numberW, y + 15, mono12, TEXT, TAG, ATTR, VALUE, MUTED);
                }
                y += lineH;
                rowIndex++;
            }
            return y + 8;
        }

        private LinkedHashMap<String, String> stableComputedStyle(UiDomElement e) {
            if (e == null) return new LinkedHashMap<>();
            LinkedHashMap<String, String> current = new LinkedHashMap<>(e.computedStyle());
            if (!current.isEmpty()) {
                computedStyleCache.put(e.nodeId(), current);
                trimComputedCache();
                return current;
            }
            LinkedHashMap<String, String> cached = computedStyleCache.get(e.nodeId());
            return cached == null ? new LinkedHashMap<>() : new LinkedHashMap<>(cached);
        }

        private void rememberComputedValue(UiDomElement e, String property, String value) {
            if (e == null || property == null || property.isBlank()) return;
            LinkedHashMap<String, String> style = computedStyleCache.computeIfAbsent(e.nodeId(), ignored -> new LinkedHashMap<>());
            String next = value == null ? "" : value.trim();
            if (next.isBlank()) style.remove(property);
            else style.put(property, next);
            trimComputedCache();
        }

        private void trimComputedCache() {
            while (computedStyleCache.size() > 96) {
                Integer first = computedStyleCache.keySet().iterator().next();
                computedStyleCache.remove(first);
            }
        }

        private int drawAttributes(Graphics2D g, UiDomNode node, UiDomElement e, int x, int y, int w) {
            y = drawKV(g, x, y, "nodeId", Integer.toString(node.nodeId()), true);
            y = drawKV(g, x, y, "nodeType", node.nodeType().name(), true);
            y = drawKV(g, x, y, "nodeName", node.nodeName(), true);
            y = drawKV(g, x, y, "children", Integer.toString(node.childCount()), true);
            if (e != null) {
                for (Map.Entry<String, String> attr : e.attributes().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) y = drawKV(g, x, y, attr.getKey(), attr.getValue(), false);
                if (!e.pseudoClasses().isEmpty()) y = drawKV(g, x, y, ":pseudo", String.join(" ", e.pseudoClasses()), false);
            } else if (node instanceof UiDomText text) y = drawKV(g, x, y, "text", clip(text.text(), 260), false);
            return y;
        }

        private int drawLayout(Graphics2D g, UiDomNode node, int x, int y, int w) {
            int id = node.nodeId();
            HtmlDomDevToolsSnapshot snapshot = inspected.devToolsSnapshot();
            var box = snapshot.layoutNodes().stream().filter(n -> n.nodeId() == id).findFirst().orElse(null);
            if (box == null) y = drawKV(g, x, y, "layout", "no box", false);
            else {
                y = drawKV(g, x, y, "x", Float.toString(box.x()), true);
                y = drawKV(g, x, y, "y", Float.toString(box.y()), true);
                y = drawKV(g, x, y, "width", Float.toString(box.width()), true);
                y = drawKV(g, x, y, "height", Float.toString(box.height()), true);
                y = drawKV(g, x, y, "lineBoxes", Integer.toString(box.lineBoxes()), true);
                y = drawKV(g, x, y, "inlineBoxes", Integer.toString(box.inlineBoxes()), true);
            }
            for (HtmlDomDevToolsHitNode hit : inspected.devToolsHitTargets()) if (hit.nodeId() == id) {
                y = drawKV(g, x, y, "hit", hit.x() + ", " + hit.y() + "  " + hit.width() + " × " + hit.height(), false);
                break;
            }
            for (HtmlDomDevToolsPaintNode paint : snapshot.paintNodes()) if (paint.nodeId() == id) {
                y = drawKV(g, x, y, "paint.order", Integer.toString(paint.order()), true);
                y = drawKV(g, x, y, "z-index", paint.zIndexAuto() ? "auto" : Integer.toString(paint.zIndex()), true);
                y = drawKV(g, x, y, "stacking", Boolean.toString(paint.stackingContext()), true);
                y = drawKV(g, x, y, "opacity", Float.toString(paint.opacity()), true);
                break;
            }
            return y;
        }

        private int drawEvents(Graphics2D g, int x, int y, int w) {
            List<HtmlDomEventDispatcher.EventLogEntry> events = inspected.devToolsEventLog();
            int from = Math.max(0, events.size() - 120);
            for (HtmlDomEventDispatcher.EventLogEntry e : events.subList(from, events.size())) {
                y = drawKV(g, x, y, "#" + e.sequence() + " " + e.type(), e.targetSelector() + "  phase=" + phase(e.eventPhase()) + "  bubbles=" + e.bubbles(), false);
                y = drawWrapped(g, x + 18, y, w - 18, e.composedPath(), MUTED);
            }
            if (events.isEmpty()) y = drawKV(g, x, y, "events", "empty", false);
            return y;
        }

        private int drawComputedKV(Graphics2D g, int x, int y, String key, String value) {
            Rectangle row = new Rectangle(x, y, Math.max(0, getWidth() - x - 18), 24);
            Rectangle valueBox = new Rectangle(x + 232, y + 2, Math.max(80, getWidth() - x - 262), 20);
            FontMetrics fm = getFontMetrics(mono12);
            boolean colorValue = HtmlDomDevToolsCssValue.colorValue(key, value);
            Color swatchColor = HtmlDomDevToolsCssValue.parseColor(value);
            boolean keyword = HtmlDomDevToolsCssValue.keywordProperty(key);
            HtmlDomDevToolsCssValue.Metric metric = HtmlDomDevToolsCssValue.metric(value).orElse(null);
            int textX = valueBox.x + 7;
            Rectangle swatchRect = colorValue ? new Rectangle(textX, y + 5, 14, 14) : new Rectangle();
            if (colorValue) textX += 22;
            Rectangle keywordRect = keyword ? new Rectangle(valueBox.x + valueBox.width - 22, y + 3, 20, 18) : new Rectangle();
            int availableEnd = keyword ? keywordRect.x - 6 : valueBox.x + valueBox.width - 6;
            Rectangle unitRect = new Rectangle();
            if (metric != null && !metric.unit().isBlank()) {
                String numberPart = metricNumberPart(value, metric.unit());
                int numberW = fm.stringWidth(numberPart);
                int unitW = Math.max(10, fm.stringWidth(metric.unit()));
                unitRect = new Rectangle(textX + numberW, y + 3, unitW + 6, 18);
            }
            computedHits.add(new ComputedHit(row, valueBox, swatchRect, unitRect, keywordRect, key, value));
            if (y > -80 && y < getHeight() + 80) {
                g.setFont(mono12);
                g.setColor(new Color(36, 37, 40));
                g.fillRect(row.x, row.y, row.width, row.height);
                g.setColor(ATTR);
                g.drawString(key, x + 8, y + 17);
                if (key.equals(editingComputedProperty)) {
                    g.setColor(new Color(16, 24, 39));
                    g.fillRoundRect(valueBox.x, valueBox.y, valueBox.width, valueBox.height, 6, 6);
                    g.setColor(ACCENT);
                    g.drawRoundRect(valueBox.x, valueBox.y, valueBox.width - 1, valueBox.height - 1, 6, 6);
                    g.setColor(TEXT);
                    g.drawString(clip(editingComputedValue, 160) + "|", valueBox.x + 7, y + 17);
                } else {
                    int vx = valueBox.x + 7;
                    if (colorValue) {
                        drawColorSwatch(g, swatchRect, swatchColor == null ? new Color(0, 0, 0, 0) : swatchColor);
                        vx += 22;
                    }
                    if (metric != null && !metric.unit().isBlank()) {
                        String numberPart = metricNumberPart(value, metric.unit());
                        g.setColor(TEXT);
                        g.drawString(clip(numberPart, 80), vx, y + 17);
                        int ux = vx + fm.stringWidth(numberPart);
                        g.setColor(ACCENT);
                        g.drawString(metric.unit(), ux + 3, y + 17);
                        g.drawLine(ux + 3, y + 19, ux + 3 + fm.stringWidth(metric.unit()), y + 19);
                    } else {
                        g.setColor(TEXT);
                        g.drawString(clip(value, Math.max(24, (availableEnd - vx) / Math.max(1, fm.charWidth('m')))), vx, y + 17);
                    }
                    if (keyword) drawKeywordButton(g, keywordRect, key, value);
                }
            }
            return y + 26;
        }

        private String metricNumberPart(String value, String unit) {
            String raw = value == null ? "" : value.trim();
            String suffix = unit == null ? "" : unit;
            if (!suffix.isBlank() && raw.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
                return raw.substring(0, raw.length() - suffix.length());
            }
            return raw;
        }

        private void drawColorSwatch(Graphics2D g, Rectangle r, Color color) {
            if (r == null || r.width <= 0 || r.height <= 0) return;
            for (int yy = r.y; yy < r.y + r.height; yy += 4) {
                for (int xx = r.x; xx < r.x + r.width; xx += 4) {
                    boolean dark = ((xx + yy) / 4 & 1) == 0;
                    g.setColor(dark ? new Color(104, 108, 118) : new Color(222, 226, 232));
                    g.fillRect(xx, yy, Math.min(4, r.x + r.width - xx), Math.min(4, r.y + r.height - yy));
                }
            }
            if (color != null) {
                g.setColor(color);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
            g.setColor(new Color(12, 14, 18));
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            g.setColor(BORDER);
            g.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
        }

        private void drawKeywordButton(Graphics2D g, Rectangle r, String property, String value) {
            if (r == null || r.width <= 0) return;
            Point mouse = getMousePositionSafe();
            boolean hovered = mouse != null && r.contains(mouse);
            boolean open = property != null && property.equals(keywordPopupProperty);
            g.setColor(open ? new Color(30, 64, 115) : hovered ? new Color(55, 57, 64) : new Color(47, 49, 54));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 7, 7);
            g.setColor(open ? ACCENT : BORDER);
            g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 7, 7);
            drawIcon(g, I_CHEVRON_DOWN, r.x + 6, r.y + 14, TEXT, icon12);
        }

        private int drawKV(Graphics2D g, int x, int y, String key, String value, boolean dim) {
            if (y > -80 && y < getHeight() + 80) {
                g.setFont(mono12);
                g.setColor(new Color(36, 37, 40));
                g.fillRect(x, y, Math.max(0, getWidth() - x - 18), 24);
                g.setColor(dim ? MUTED : ATTR);
                g.drawString(key, x + 8, y + 17);
                g.setColor(dim ? MUTED : TEXT);
                g.drawString(clip(value, 120), x + 238, y + 17);
            }
            return y + 26;
        }

        private int drawWrapped(Graphics2D g, int x, int y, int w, String text, Color color) {
            g.setFont(mono12);
            g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            String s = text == null ? "" : text;
            int maxChars = Math.max(12, w / Math.max(1, fm.charWidth('m')));
            while (!s.isBlank()) {
                int cut = Math.min(maxChars, s.length());
                g.drawString(s.substring(0, cut), x + 8, y + 17);
                s = s.substring(cut).trim();
                y += 18;
            }
            return y + 8;
        }

        private void drawLine(Graphics2D g, int x, int y, String text, Color color) {
            g.setFont(mono12);
            g.setColor(color);
            g.drawString(text, x + 8, y + 17);
        }

        private int draw(Graphics2D g, String s, int x, int y, Color color) {
            g.setFont(mono13);
            g.setColor(color);
            g.drawString(s, x, y);
            return x + g.getFontMetrics().stringWidth(s);
        }

        private void drawIcon(Graphics2D g, String icon, int x, int y, Color color, Font font) {
            g.setFont(font);
            g.setColor(color);
            g.drawString(icon, x, y);
        }

        private void handleMove(MouseEvent e) {
            if (contextMenuOpen) {
                repaint();
                return;
            }
            int next = 0;
            for (RowHit hit : rowHits) if (hit.rect.contains(e.getPoint())) { next = hit.nodeId; break; }
            if (next != hoverNodeId) {
                hoverNodeId = next;
                inspected.setDevToolsHighlightedNodeId(next);
                repaint();
                return;
            }
            if (htmlEditMode || !toolButtonHits.isEmpty()) repaint();
        }

        private void handleDrag(MouseEvent e) {
            if (draggingWindow) {
                moveToolWindow(e);
                return;
            }
            if (colorPickerOpen() && colorPickerRect.contains(e.getPoint())) {
                handleColorPickerClick(e.getPoint());
                return;
            }
            if (!htmlSelecting) {
                handleMove(e);
                return;
            }
            int offset = htmlOffsetAt(e.getPoint());
            htmlSelectionFocus = offset;
            htmlCaretIndex = offset;
            repaint();
        }

        private boolean beginWindowDrag(MouseEvent e) {
            Point point = e.getPoint();
            if (point == null || point.y < 0 || point.y >= HEADER) return false;
            for (WindowButtonHit hit : windowButtonHits) if (hit.rect().contains(point)) return false;
            for (ToolButtonHit hit : toolButtonHits) if (hit.rect.contains(point)) return false;
            Window current = frame;
            if (current == null) return false;
            draggingWindow = true;
            windowDragStartScreen = e.getLocationOnScreen();
            windowDragStartLocation = current.getLocation();
            requestFocusInWindow();
            return true;
        }

        private void moveToolWindow(MouseEvent e) {
            Window current = frame;
            if (current == null) return;
            Point screen = e.getLocationOnScreen();
            int dx = screen.x - windowDragStartScreen.x;
            int dy = screen.y - windowDragStartScreen.y;
            current.setLocation(windowDragStartLocation.x + dx, windowDragStartLocation.y + dy);
        }

        private boolean handleWindowButtonClick(Point point) {
            if (point == null) return false;
            for (WindowButtonHit hit : windowButtonHits) {
                if (!hit.rect().contains(point)) continue;
                executeWindowCommand(hit.command());
                return true;
            }
            return false;
        }

        private void executeWindowCommand(WindowCommand command) {
            Window current = frame;
            if (command == null || current == null) return;
            switch (command) {
                case CLOSE -> closeOnEdt();
                case MINIMIZE -> current.setVisible(false);
            }
        }

        private void beginHtmlSelection(MouseEvent e) {
            if (!htmlEditMode || !"HTML".equals(activeTab) || !htmlEditorRect.contains(e.getPoint())) return;
            int offset = htmlOffsetAt(e.getPoint());
            htmlCaretIndex = offset;
            htmlSelectionAnchor = offset;
            htmlSelectionFocus = offset;
            htmlSelecting = true;
            requestFocusInWindow();
            repaint();
        }

        private int htmlOffsetAt(Point point) {
            if (point == null) return clampIndex(htmlCaretIndex);
            if (htmlLineHits.isEmpty()) return clampIndex(htmlEditText.length());
            FontMetrics fm = getFontMetrics(mono12);
            int charW = Math.max(1, fm.charWidth('m'));
            HtmlLineHit fallback = htmlLineHits.get(htmlLineHits.size() - 1);
            for (HtmlLineHit hit : htmlLineHits) {
                if (point.y >= hit.row.y && point.y <= hit.row.y + hit.row.height) {
                    int column = Math.max(0, Math.round((point.x - hit.textX) / (float) charW));
                    return clampIndex(Math.max(hit.startOffset, Math.min(hit.endOffset, hit.startOffset + column)));
                }
                if (point.y < hit.row.y) return clampIndex(hit.startOffset);
                fallback = hit;
            }
            return clampIndex(fallback.endOffset);
        }

        private void clearHover() {
            if (contextMenuOpen) return;
            hoverNodeId = 0;
            inspected.setDevToolsHighlightedNodeId(0);
            repaint();
        }

        private void handleClick(MouseEvent e) {
            if (contextMenuOpen) {
                if (handleMenuClick(e.getPoint())) return;
                contextMenuOpen = false;
                repaint();
                return;
            }
            if (colorPickerOpen()) {
                if (handleColorPickerClick(e.getPoint())) return;
                closeColorPicker();
                repaint();
                return;
            }
            if (keywordPopupOpen()) {
                if (handleKeywordPopupClick(e.getPoint())) return;
                closeKeywordPopup();
                repaint();
                return;
            }
            if (handleWindowButtonClick(e.getPoint())) return;
            for (ToolButtonHit hit : toolButtonHits) if (hit.rect.contains(e.getPoint())) {
                if (hit.enabled) executeToolCommand(hit.command);
                repaint();
                return;
            }
            if (htmlEditMode && "HTML".equals(activeTab)) {
                for (HtmlEditButtonHit hit : htmlEditButtonHits) if (hit.rect.contains(e.getPoint())) {
                    if (hit.command == HtmlEditCommand.APPLY) applyHtmlEdit();
                    else cancelHtmlEdit();
                    repaint();
                    return;
                }
                requestFocusInWindow();
            }
            if ("Computed".equals(activeTab)) {
                for (ComputedHit hit : computedHits) {
                    Point point = e.getPoint();
                    if (hit.swatchRect().contains(point)) {
                        openColorPicker(hit);
                        repaint();
                        return;
                    }
                    if (hit.keywordRect().contains(point)) {
                        openKeywordPopup(hit);
                        repaint();
                        return;
                    }
                    if (hit.unitRect().contains(point) || hit.row().contains(point) || hit.valueRect().contains(point)) {
                        startComputedEdit(hit);
                        repaint();
                        return;
                    }
                }
                cancelComputedEdit();
            }
            for (ToggleHit hit : toggleHits) if (hit.rect.contains(e.getPoint())) {
                toggleNode(hit.nodeId);
                repaint();
                return;
            }
            for (TabHit hit : tabHits) if (hit.rect.contains(e.getPoint())) {
                activeTab = hit.tab;
                rightScroll = 0;
                closeKeywordPopup();
                closeColorPicker();
                clearComputedEditState();
                repaint();
                return;
            }
            for (RowHit hit : rowHits) if (hit.rect.contains(e.getPoint())) {
                selectedNodeId = hit.nodeId;
                rightScroll = 0;
                closeKeywordPopup();
                closeColorPicker();
                clearComputedEditState();
                inspected.setDevToolsHighlightedNodeId(hit.nodeId);
                if (e.getClickCount() >= 2) toggleNode(hit.nodeId);
                repaint();
                return;
            }
        }

        private void startComputedEdit(ComputedHit hit) {
            if (hit == null) return;
            closeKeywordPopup();
            closeColorPicker();
            editingComputedProperty = hit.property();
            editingComputedValue = hit.value() == null ? "" : hit.value();
            editingComputedOriginalValue = editingComputedValue;
            editingComputedLastAppliedValue = editingComputedValue;
            editingComputedLiveApplied = false;
            contextMenuOpen = false;
            requestFocusInWindow();
        }

        private void cancelComputedEdit() {
            if (editingComputedProperty.isBlank()) return;
            clearComputedEditState();
            repaint();
        }

        private void revertComputedEdit() {
            if (editingComputedProperty.isBlank()) return;
            String property = editingComputedProperty;
            String originalValue = editingComputedOriginalValue;
            boolean restore = editingComputedLiveApplied;
            clearComputedEditState();
            if (restore) {
                applyComputedValue(property, originalValue, false);
                saveStatus = "runtime style reverted: " + property;
            } else {
                repaint();
            }
        }

        private void clearComputedEditState() {
            editingComputedProperty = "";
            editingComputedValue = "";
            editingComputedOriginalValue = "";
            editingComputedLastAppliedValue = "";
            editingComputedLiveApplied = false;
        }

        private void commitComputedEdit() {
            if (editingComputedProperty.isBlank()) return;
            String property = editingComputedProperty;
            String value = editingComputedValue.trim();
            boolean alreadyApplied = editingComputedLiveApplied && value.equals(editingComputedLastAppliedValue);
            boolean useExistingUndoSnapshot = editingComputedLiveApplied;
            clearComputedEditState();
            if (alreadyApplied) {
                saveStatus = "runtime style: " + property;
                repaint();
                return;
            }
            applyComputedValue(property, value, !useExistingUndoSnapshot);
        }

        private void applyComputedValue(String property, String value, boolean undoable) {
            if (property == null || property.isBlank()) return;
            UiDomNode node = nodeById(selectedNodeId);
            if (node instanceof UiDomElement element) {
                String safeProperty = property.trim();
                String safeValue = value == null ? "" : value.trim();
                if (undoable) mutate(() -> HtmlDomDevToolsInlineStyle.applyRuntimeComputedStyle(element, safeProperty, safeValue));
                else {
                    HtmlDomDevToolsInlineStyle.applyRuntimeComputedStyle(element, safeProperty, safeValue);
                    dirty = true;
                }
                rememberComputedValue(element, safeProperty, safeValue);
                saveStatus = "runtime style: " + safeProperty;
            }
            rebuildRows();
            inspected.repaint();
            repaint();
        }

        private void applyComputedLivePreview(String property, String value) {
            if (property == null || property.isBlank()) return;
            if (!editingComputedLiveApplied) {
                undoStack.add(HtmlDomDevToolsNodeSnapshot.copy(inspected.document().documentElement()));
                while (undoStack.size() > 128) undoStack.remove(0);
                redoStack.clear();
                editingComputedLiveApplied = true;
            }
            editingComputedLastAppliedValue = value == null ? "" : value.trim();
            applyComputedValue(property, editingComputedLastAppliedValue, false);
            saveStatus = "runtime style live: " + property;
        }

        private String selectedComputedValue(String property) {
            UiDomNode node = nodeById(selectedNodeId);
            if (!(node instanceof UiDomElement element)) return "";
            return stableComputedStyle(element).getOrDefault(property, "");
        }

        private boolean nudgeComputedEdit(int direction, KeyEvent e) {
            if (editingComputedProperty.isBlank()) return false;
            if (HtmlDomDevToolsCssValue.metricValue(editingComputedValue)) {
                editingComputedValue = HtmlDomDevToolsCssValue.nudgeMetric(editingComputedValue, direction, e.isShiftDown(), e.isAltDown());
                applyComputedLivePreview(editingComputedProperty, editingComputedValue);
                return true;
            }
            List<String> options = HtmlDomDevToolsCssValue.keywordOptions(editingComputedProperty);
            if (!options.isEmpty()) {
                int index = -1;
                for (int i = 0; i < options.size(); i++) {
                    if (options.get(i).equalsIgnoreCase(editingComputedValue.trim())) { index = i; break; }
                }
                int next = index + (direction > 0 ? 1 : -1);
                if (next < 0) next = options.size() - 1;
                if (next >= options.size()) next = 0;
                editingComputedValue = options.get(next);
                repaint();
                return true;
            }
            return false;
        }

        private void handleKeyPressed(KeyEvent e) {
            if (htmlEditMode && "HTML".equals(activeTab)) {
                handleHtmlEditKeyPressed(e);
                return;
            }
            if (colorPickerOpen()) {
                if (handleColorPickerKeyPressed(e)) return;
                return;
            }
            if (keywordPopupOpen()) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeKeywordPopup();
                    repaint();
                    e.consume();
                }
                return;
            }
            if (editingComputedProperty.isBlank()) return;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER -> { commitComputedEdit(); e.consume(); }
                case KeyEvent.VK_ESCAPE -> { revertComputedEdit(); e.consume(); }
                case KeyEvent.VK_UP -> { if (nudgeComputedEdit(1, e)) e.consume(); }
                case KeyEvent.VK_DOWN -> { if (nudgeComputedEdit(-1, e)) e.consume(); }
                case KeyEvent.VK_BACK_SPACE -> {
                    if (!editingComputedValue.isEmpty()) editingComputedValue = editingComputedValue.substring(0, editingComputedValue.length() - 1);
                    repaint();
                    e.consume();
                }
                case KeyEvent.VK_DELETE -> {
                    editingComputedValue = "";
                    repaint();
                    e.consume();
                }
                default -> { }
            }
        }

        private void handleKeyTyped(KeyEvent e) {
            if (htmlEditMode && "HTML".equals(activeTab)) {
                handleHtmlEditKeyTyped(e);
                return;
            }
            if (colorPickerOpen()) {
                if (handleColorPickerKeyTyped(e)) return;
                return;
            }
            if (editingComputedProperty.isBlank()) return;
            char ch = e.getKeyChar();
            if (ch == KeyEvent.CHAR_UNDEFINED || ch == 10 || ch == 13 || ch == 8 || ch == 127 || Character.isISOControl(ch)) return;
            editingComputedValue += ch;
            repaint();
            e.consume();
        }

        private void handleHtmlEditKeyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE -> { cancelHtmlEdit(); e.consume(); }
                case KeyEvent.VK_ENTER -> {
                    if (e.isControlDown()) applyHtmlEdit();
                    else insertHtmlText("\n");
                    repaint();
                    e.consume();
                }
                case KeyEvent.VK_TAB -> {
                    insertHtmlText("    ");
                    repaint();
                    e.consume();
                }
                case KeyEvent.VK_BACK_SPACE -> {
                    backspaceHtmlText();
                    repaint();
                    e.consume();
                }
                case KeyEvent.VK_DELETE -> {
                    deleteHtmlText();
                    repaint();
                    e.consume();
                }
                case KeyEvent.VK_LEFT -> {
                    moveHtmlCaret(Math.max(0, htmlCaretIndex - 1), e.isShiftDown());
                    repaint();
                    e.consume();
                }
                case KeyEvent.VK_RIGHT -> {
                    moveHtmlCaret(Math.min(htmlEditText.length(), htmlCaretIndex + 1), e.isShiftDown());
                    repaint();
                    e.consume();
                }
                case KeyEvent.VK_HOME -> {
                    moveHtmlCaret(0, e.isShiftDown());
                    repaint();
                    e.consume();
                }
                case KeyEvent.VK_END -> {
                    moveHtmlCaret(htmlEditText.length(), e.isShiftDown());
                    repaint();
                    e.consume();
                }
                case KeyEvent.VK_A -> {
                    if (e.isControlDown()) {
                        htmlSelectionAnchor = 0;
                        htmlSelectionFocus = htmlEditText.length();
                        htmlCaretIndex = htmlSelectionFocus;
                        repaint();
                        e.consume();
                    }
                }
                default -> { }
            }
        }

        private void handleHtmlEditKeyTyped(KeyEvent e) {
            char ch = e.getKeyChar();
            if (ch == KeyEvent.CHAR_UNDEFINED || ch == 10 || ch == 13 || ch == 8 || ch == 127 || Character.isISOControl(ch)) return;
            insertHtmlText(String.valueOf(ch));
            htmlEditError = "";
            repaint();
            e.consume();
        }

        private void insertHtmlText(String text) {
            if (text == null || text.isEmpty()) return;
            int start = selectionStart();
            int end = selectionEnd();
            htmlEditText = htmlEditText.substring(0, start) + text + htmlEditText.substring(end);
            htmlCaretIndex = start + text.length();
            htmlSelectionAnchor = htmlCaretIndex;
            htmlSelectionFocus = htmlCaretIndex;
        }

        private void backspaceHtmlText() {
            if (hasHtmlSelection()) {
                deleteHtmlSelection();
                return;
            }
            if (htmlCaretIndex <= 0) return;
            htmlEditText = htmlEditText.substring(0, htmlCaretIndex - 1) + htmlEditText.substring(htmlCaretIndex);
            htmlCaretIndex--;
            htmlSelectionAnchor = htmlCaretIndex;
            htmlSelectionFocus = htmlCaretIndex;
        }

        private void deleteHtmlText() {
            if (hasHtmlSelection()) {
                deleteHtmlSelection();
                return;
            }
            if (htmlCaretIndex >= htmlEditText.length()) return;
            htmlEditText = htmlEditText.substring(0, htmlCaretIndex) + htmlEditText.substring(htmlCaretIndex + 1);
            htmlSelectionAnchor = htmlCaretIndex;
            htmlSelectionFocus = htmlCaretIndex;
        }

        private void deleteHtmlSelection() {
            int start = selectionStart();
            int end = selectionEnd();
            if (end <= start) return;
            htmlEditText = htmlEditText.substring(0, start) + htmlEditText.substring(end);
            htmlCaretIndex = start;
            htmlSelectionAnchor = start;
            htmlSelectionFocus = start;
        }

        private boolean hasHtmlSelection() {
            return selectionEnd() > selectionStart();
        }

        private int selectionStart() {
            return Math.min(clampIndex(htmlSelectionAnchor), clampIndex(htmlSelectionFocus));
        }

        private int selectionEnd() {
            return Math.max(clampIndex(htmlSelectionAnchor), clampIndex(htmlSelectionFocus));
        }

        private void moveHtmlCaret(int index, boolean extendSelection) {
            htmlCaretIndex = clampIndex(index);
            if (extendSelection) htmlSelectionFocus = htmlCaretIndex;
            else {
                htmlSelectionAnchor = htmlCaretIndex;
                htmlSelectionFocus = htmlCaretIndex;
            }
        }

        private int clampIndex(int index) {
            return Math.max(0, Math.min(htmlEditText.length(), index));
        }

        private void openContextMenu(MouseEvent e) {
            Point point = e.getPoint();
            int node = nodeAt(point);
            if (node <= 0 && selectedNodeId > 0 && point.x >= LEFT_W) node = selectedNodeId;
            if (node <= 0) return;
            selectedNodeId = node;
            hoverNodeId = node;
            inspected.setDevToolsHighlightedNodeId(node);
            contextNodeId = node;
            contextMenuX = e.getX();
            contextMenuY = e.getY();
            contextMenuOpen = true;
            closeKeywordPopup();
            closeColorPicker();
            editingComputedProperty = "";
            editingComputedValue = "";
            repaint();
        }

        private boolean handleMenuClick(Point point) {
            for (MenuHit hit : menuHits) {
                if (!hit.rect.contains(point)) continue;
                if (hit.enabled) executeMenuCommand(hit.command);
                contextMenuOpen = false;
                repaint();
                return true;
            }
            return false;
        }

        private void executeToolCommand(ToolCommand command) {
            switch (command) {
                case UNDO -> undo();
                case REDO -> redo();
                case SAVE -> saveHtmlFiles();
            }
            rebuildRows();
            inspected.repaint();
        }

        private void executeMenuCommand(MenuCommand command) {
            UiDomNode node = nodeById(contextNodeId);
            if (node == null || command == null) return;
            switch (command) {
                case ADD_ATTRIBUTE -> mutate(() -> addAttribute(node));
                case EDIT_HTML -> startHtmlEdit(node);
                case DUPLICATE -> mutate(() -> duplicateNode(node));
                case DELETE -> mutate(() -> deleteNode(node));
                case CUT -> mutate(() -> { clipboard = HtmlDomDevToolsNodeSnapshot.copy(node); deleteNode(node); });
                case COPY -> clipboard = HtmlDomDevToolsNodeSnapshot.copy(node);
                case PASTE -> mutate(() -> pasteNode(node));
            }
            rightScroll = 0;
            rebuildRows();
            inspected.repaint();
        }

        private void handleWheel(MouseWheelEvent e) {
            if (contextMenuOpen) {
                contextMenuOpen = false;
                repaint();
                return;
            }
            float amount = (float) e.getPreciseWheelRotation() * 38f;
            if (e.getX() < LEFT_W) domScroll = Math.max(0f, domScroll + amount);
            else rightScroll = Math.max(0f, rightScroll + amount);
            repaint();
        }

        private void rebuildRows() {
            rows.clear();
            collect(inspected.document().documentElement(), 0);
        }

        private void collect(UiDomNode node, int depth) {
            if (node instanceof UiDomText text && text.text().trim().isBlank()) return;
            rows.add(new DomRow(node, depth));
            if (collapsedNodeIds.contains(node.nodeId())) return;
            for (UiDomNode child : node.children()) collect(child, depth + 1);
        }
    }

    private void startHtmlEdit(UiDomNode node) {
        if (node == null) return;
        htmlEditMode = true;
        htmlEditNodeId = node.nodeId();
        htmlEditText = HtmlDomDevToolsHtmlSerializer.serializeNodeToString(node);
        htmlEditError = "";
        htmlCaretIndex = htmlEditText.length();
        htmlSelectionAnchor = htmlCaretIndex;
        htmlSelectionFocus = htmlCaretIndex;
        activeTab = "HTML";
        rightScroll = 0;
        canvas.requestFocusInWindow();
    }

    private void cancelHtmlEdit() {
        htmlEditMode = false;
        htmlEditNodeId = 0;
        htmlEditText = "";
        htmlEditError = "";
        htmlCaretIndex = 0;
        htmlSelectionAnchor = 0;
        htmlSelectionFocus = 0;
        activeTab = "Computed";
        rightScroll = 0;
    }

    private void applyHtmlEdit() {
        UiDomNode node = nodeById(htmlEditNodeId);
        if (node == null) {
            htmlEditError = "node disappeared";
            return;
        }
        try {
            HtmlDomDevToolsNodeSnapshot.NodeCopy replacement = parseHtmlEditReplacement(htmlEditText);
            mutate(() -> replaceNode(node, replacement.create(inspected.document())));
            htmlEditMode = false;
            htmlEditNodeId = 0;
            htmlEditText = "";
            htmlEditError = "";
            htmlCaretIndex = 0;
            htmlSelectionAnchor = 0;
            htmlSelectionFocus = 0;
            activeTab = "Computed";
            saveStatus = "html applied";
        } catch (RuntimeException error) {
            htmlEditError = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        }
    }

    private HtmlDomDevToolsNodeSnapshot.NodeCopy parseHtmlEditReplacement(String source) {
        String html = source == null ? "" : source.trim();
        if (html.isBlank()) return HtmlDomDevToolsNodeSnapshot.text("");
        UiDomDocument parsed = new UiMarkupParser().parse(html, "devtools-html-edit.html").dom();
        UiDomNode candidate;
        String lower = html.toLowerCase();
        if (lower.startsWith("<html") || lower.startsWith("<!doctype")) candidate = parsed.documentElement();
        else {
            UiDomElement body = parsed.body().orElse(parsed.renderRoot());
            candidate = firstMeaningfulChild(body);
            if (candidate == null) candidate = body;
        }
        return HtmlDomDevToolsNodeSnapshot.copy(candidate);
    }

    private UiDomNode firstMeaningfulChild(UiDomElement element) {
        if (element == null) return null;
        for (UiDomNode child : element.children()) {
            if (child instanceof UiDomText text && text.text().trim().isBlank()) continue;
            return child;
        }
        return null;
    }

    private void replaceNode(UiDomNode previous, UiDomNode next) {
        if (previous == null || next == null) return;
        UiDomElement parent = previous.parent();
        if (parent == null) {
            if (next instanceof UiDomElement root) {
                inspected.document().setRoot(root);
                selectedNodeId = root.nodeId();
                inspected.setDevToolsHighlightedNodeId(root.nodeId());
            }
            return;
        }
        int index = parent.children().indexOf(previous);
        if (index < 0) index = parent.childCount();
        parent.removeChild(previous);
        parent.insertChild(index, next);
        selectedNodeId = next.nodeId();
        inspected.setDevToolsHighlightedNodeId(next.nodeId());
        collapsedNodeIds.remove(parent.nodeId());
    }

    private String serializeNodeToString(UiDomNode node) {
        StringBuilder out = new StringBuilder(4096);
        out.append(HtmlDomDevToolsHtmlSerializer.serializeNodeToString(node));
        return out.toString();
    }

    private void mutate(Runnable mutation) {
        if (mutation == null) return;
        undoStack.add(HtmlDomDevToolsNodeSnapshot.copy(inspected.document().documentElement()));
        while (undoStack.size() > 128) undoStack.remove(0);
        redoStack.clear();
        mutation.run();
        dirty = true;
        saveStatus = "modified";
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        redoStack.add(HtmlDomDevToolsNodeSnapshot.copy(inspected.document().documentElement()));
        restoreSnapshot(undoStack.remove(undoStack.size() - 1));
        dirty = true;
        saveStatus = "undo";
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        undoStack.add(HtmlDomDevToolsNodeSnapshot.copy(inspected.document().documentElement()));
        restoreSnapshot(redoStack.remove(redoStack.size() - 1));
        dirty = true;
        saveStatus = "redo";
    }

    private void restoreSnapshot(HtmlDomDevToolsNodeSnapshot.NodeCopy snapshot) {
        if (snapshot == null) return;
        UiDomNode restored = snapshot.create(inspected.document());
        if (restored instanceof UiDomElement root) {
            inspected.document().setRoot(root);
            selectedNodeId = root.nodeId();
            hoverNodeId = 0;
            collapsedNodeIds.clear();
            inspected.setDevToolsHighlightedNodeId(0);
            inspected.repaint();
        }
    }

    private void saveHtmlFiles() {
        String html = HtmlDomDevToolsHtmlSerializer.serializeDocument(inspected.document());
        ArrayList<Path> targets = saveTargets();
        int saved = 0;
        String last = "";
        for (Path target : targets) {
            try {
                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(target, html, StandardCharsets.UTF_8);
                saved++;
                last = target.toString();
            } catch (Exception ignored) {
            }
        }
        if (saved > 0) {
            dirty = false;
            saveStatus = "saved " + saved + " file" + (saved == 1 ? "" : "s") + (last.isBlank() ? "" : ": " + last);
        } else {
            saveStatus = "save failed: no writable html target";
        }
    }

    private ArrayList<Path> saveTargets() {
        ArrayList<Path> out = new ArrayList<>();
        String rel = "modules/html-dom-desktop/src/main/resources/html-dom/bundled/showcase.ui.html";
        String buildRel = "modules/html-dom-desktop/build/resources/main/html-dom/bundled/showcase.ui.html";
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        addIfExists(out, cwd.resolve(rel));
        addIfExists(out, cwd.resolve(buildRel));
        addIfExists(out, cwd.resolve("../HtmlDom").normalize().resolve(rel));
        addIfExists(out, cwd.resolve("../HtmlDom").normalize().resolve(buildRel));
        addIfExists(out, Paths.get(System.getProperty("user.home", ""), "Documents", "Repos", "HtmlDom").resolve(rel));
        addIfExists(out, Paths.get(System.getProperty("user.home", ""), "Documents", "Repos", "HtmlDom").resolve(buildRel));
        if (out.isEmpty()) out.add(cwd.resolve("html-dom-devtools-save.ui.html"));
        return out;
    }

    private void addIfExists(ArrayList<Path> out, Path path) {
        if (path == null) return;
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.exists(normalized) && !out.contains(normalized)) out.add(normalized);
    }

    private List<MenuItem> menuItems() {
        UiDomNode node = nodeById(contextNodeId());
        boolean hasNode = node != null;
        boolean element = node instanceof UiDomElement;
        boolean removable = hasNode && node.parent() != null;
        return List.of(
                new MenuItem(MenuCommand.EDIT_HTML, I_CODE, "Edit Block", element, false),
                new MenuItem(MenuCommand.COPY, I_COPY, "Copy Block", hasNode, false),
                new MenuItem(MenuCommand.DUPLICATE, I_CLONE, "Duplicate Block", removable, false),
                new MenuItem(MenuCommand.DELETE, I_TRASH, "Delete Block", removable, false),
                MenuItem.separatorItem(),
                new MenuItem(MenuCommand.ADD_ATTRIBUTE, I_PLUS, "Add Attribute", element, false),
                new MenuItem(MenuCommand.CUT, I_SCISSORS, "Cut Block", removable, false),
                new MenuItem(MenuCommand.PASTE, I_PASTE, "Paste Block", clipboard != null && hasNode, false)
        );
    }

    private boolean popup(MouseEvent e) {
        return e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e);
    }

    private int contextNodeId() {
        return canvas.contextNodeId;
    }

    private int nodeAt(Point point) {
        for (RowHit hit : canvas.rowHits) if (hit.rect.contains(point)) return hit.nodeId;
        return 0;
    }

    private void toggleNode(int nodeId) {
        UiDomNode node = nodeById(nodeId);
        if (!expandable(node)) return;
        if (!collapsedNodeIds.remove(nodeId)) collapsedNodeIds.add(nodeId);
    }

    private boolean expandable(UiDomNode node) {
        if (!(node instanceof UiDomElement)) return false;
        for (UiDomNode child : node.children()) {
            if (child instanceof UiDomText text && text.text().trim().isBlank()) continue;
            return true;
        }
        return false;
    }

    private void addAttribute(UiDomNode node) {
        if (!(node instanceof UiDomElement element)) return;
        String name = "data-devtools";
        int index = 1;
        while (element.hasAttribute(name)) name = "data-devtools-" + (++index);
        element.setAttribute(name, "true");
        selectedNodeId = element.nodeId();
        activeTab = "Attributes";
    }

    private void duplicateNode(UiDomNode node) {
        if (node == null || node.parent() == null) return;
        UiDomElement parent = node.parent();
        int index = parent.children().indexOf(node);
        UiDomNode clone = HtmlDomDevToolsNodeSnapshot.copy(node).create(inspected.document());
        parent.insertChild(index + 1, clone);
        selectedNodeId = clone.nodeId();
    }

    private void deleteNode(UiDomNode node) {
        if (node == null || node.parent() == null) return;
        UiDomElement parent = node.parent();
        parent.removeChild(node);
        selectedNodeId = parent.nodeId();
        hoverNodeId = 0;
        inspected.setDevToolsHighlightedNodeId(selectedNodeId);
    }

    private void pasteNode(UiDomNode node) {
        if (clipboard == null || node == null) return;
        UiDomElement target = node instanceof UiDomElement element ? element : node.parent();
        if (target == null) return;
        UiDomNode pasted = clipboard.create(inspected.document());
        target.appendChild(pasted);
        selectedNodeId = pasted.nodeId();
        collapsedNodeIds.remove(target.nodeId());
    }

    private UiDomNode nodeById(int id) {
        if (id <= 0) return null;
        for (UiDomNode node : UiDomTraversal.depthFirst(inspected.document().documentElement())) if (node.nodeId() == id) return node;
        return null;
    }

    private String nodeTitle(UiDomNode node) {
        return node instanceof UiDomElement e ? selector(e) + "  #" + node.nodeId() : "#text  #" + node.nodeId();
    }

    private String domPath(UiDomNode node) {
        ArrayList<String> parts = new ArrayList<>();
        UiDomNode current = node;
        while (current != null) {
            parts.add(current instanceof UiDomElement e ? selector(e) : "#text");
            current = current.parent();
        }
        StringBuilder out = new StringBuilder();
        for (int i = parts.size() - 1; i >= 0; i--) {
            if (out.length() > 0) out.append(" > ");
            out.append(parts.get(i));
        }
        return out.toString();
    }

    private String selector(UiDomElement e) {
        String classes = String.join(".", e.classList().values());
        return e.tagName() + (e.id().isBlank() ? "" : "#" + e.id()) + (classes.isBlank() ? "" : "." + classes);
    }

    private Point getMousePositionSafe() {
        try { return canvas.getMousePosition(); }
        catch (RuntimeException ignored) { return null; }
    }

    private String clip(String value, int max) {
        String v = value == null ? "" : value.replace("\n", "\\n").replace("\r", "");
        return v.length() <= max ? v : v.substring(0, Math.max(0, max - 1)) + "…";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String phase(int phase) {
        return switch (phase) {
            case HtmlDomEventDispatcher.DomEvent.CAPTURING_PHASE -> "capture";
            case HtmlDomEventDispatcher.DomEvent.AT_TARGET -> "target";
            case HtmlDomEventDispatcher.DomEvent.BUBBLING_PHASE -> "bubble";
            default -> "none";
        };
    }

    private enum ToolCommand { UNDO, REDO, SAVE }
    private enum WindowCommand { MINIMIZE, CLOSE }
    private enum HtmlEditCommand { APPLY, CANCEL }
    private enum MenuCommand { ADD_ATTRIBUTE, EDIT_HTML, DUPLICATE, DELETE, CUT, COPY, PASTE }
    private record DomRow(UiDomNode node, int depth) { }
    private record RowHit(Rectangle rect, int nodeId) { }
    private record ToggleHit(Rectangle rect, int nodeId) { }
    private record TabHit(Rectangle rect, String tab) { }
    private record ComputedHit(Rectangle row, Rectangle valueRect, Rectangle swatchRect, Rectangle unitRect, Rectangle keywordRect, String property, String value) { }
    private record KeywordOptionHit(Rectangle rect, String property, String value) { }
    private record ColorPickerHit(Rectangle rect, String channel) { }
    private record HtmlEditButtonHit(Rectangle rect, HtmlEditCommand command) { }
    private record HtmlLineHit(Rectangle row, int textX, int baseline, int startOffset, int endOffset, String text) { }
    private record ToolButtonHit(Rectangle rect, ToolCommand command, boolean enabled) { }
    private record WindowButtonHit(Rectangle rect, WindowCommand command) { }
    private record MenuHit(Rectangle rect, MenuCommand command, boolean enabled) { }
    private record MenuItem(MenuCommand command, String icon, String label, boolean enabled, boolean separator) {
        static MenuItem separatorItem() { return new MenuItem(null, "", "", false, true); }
    }
}
