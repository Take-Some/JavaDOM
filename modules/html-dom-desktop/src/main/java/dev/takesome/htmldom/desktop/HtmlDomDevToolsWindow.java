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

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Point;
import java.awt.Composite;
import java.awt.AlphaComposite;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Chrome-like Elements DOM viewer painted by HtmlDom itself: no Swing tables/tree sheet. */
public final class HtmlDomDevToolsWindow {
    private static final String DEVTOOLS_ALWAYS_ON_TOP_PROPERTY = "htmldom.devtools.alwaysOnTop";
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
    private static final int HEADER = 44;
    private static final int LEFT_W = 540;
    private static final int ROW_H = 23;
    private static final int TAB_H = 34;
    private static final int MENU_W = 270;
    private static final int MENU_ROW_H = 32;

    private final HtmlDomSwingPanel inspected;
    private final Canvas canvas = new Canvas();
    private final Set<Integer> collapsedNodeIds = new HashSet<>();
    private JFrame frame;
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

    public HtmlDomDevToolsWindow(HtmlDomSwingPanel inspected) {
        this.inspected = inspected;
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

    private void openOnEdt() {
        if (frame == null || !frame.isDisplayable()) {
            frame = new JFrame("HtmlDom DevTools — Elements");
            frame.setName("HtmlDom DevTools");
            frame.setAutoRequestFocus(true);
            frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            frame.setContentPane(canvas);
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
        return Boolean.parseBoolean(System.getProperty(DEVTOOLS_ALWAYS_ON_TOP_PROPERTY, "false"));
    }

    private void raiseDevToolsWindow() {
        if (frame == null || !frame.isShowing()) return;
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
        private final ArrayList<ComputedHit> computedHits = new ArrayList<>();
        private final ArrayList<HtmlEditButtonHit> htmlEditButtonHits = new ArrayList<>();
        private final ArrayList<HtmlLineHit> htmlLineHits = new ArrayList<>();
        private final ArrayList<MenuHit> menuHits = new ArrayList<>();
        private Rectangle htmlEditorRect = new Rectangle();
        private boolean htmlSelecting;
        private boolean contextMenuOpen;
        private int contextMenuX;
        private int contextMenuY;
        private int contextNodeId;

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
                @Override public void mousePressed(MouseEvent e) { if (popup(e)) openContextMenu(e); else beginHtmlSelection(e); }
                @Override public void mouseReleased(MouseEvent e) { if (popup(e)) openContextMenu(e); else htmlSelecting = false; }
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
            computedHits.clear();
            htmlEditButtonHits.clear();
            htmlLineHits.clear();
            menuHits.clear();
            g.setColor(BG);
            g.fillRect(0, 0, w, h);
            drawHeader(g, w);
            drawDomTree(g, h);
            drawInspector(g, w, h);
            if (contextMenuOpen) drawContextMenu(g);
        }

        private void drawHeader(Graphics2D g, int w) {
            g.setColor(new Color(35, 36, 40));
            g.fillRect(0, 0, w, HEADER);
            g.setColor(BORDER);
            g.drawLine(0, HEADER - 1, w, HEADER - 1);
            g.setFont(monoBold);
            g.setColor(TEXT);
            g.drawString("Elements", 16, 27);
            HtmlDomDevToolsSnapshot snapshot = inspected.devToolsSnapshot();
            g.setFont(mono12);
            g.setColor(MUTED);
            g.drawString("DOM " + snapshot.nodeCount() + " nodes  ·  boxes " + snapshot.layoutBoxCount() + "  ·  events " + inspected.devToolsEventLog().size() + (dirty ? "  ·  modified" : ""), 122, 27);
            if (!saveStatus.isBlank()) {
                g.setColor(dirty ? new Color(253, 214, 99) : new Color(129, 201, 149));
                g.drawString(clip(saveStatus, 72), Math.max(380, w - 670), 27);
            }
            int x = Math.max(520, w - 300);
            drawToolButton(g, x, 8, 82, I_UNDO, "Undo", ToolCommand.UNDO, !undoStack.isEmpty());
            drawToolButton(g, x + 88, 8, 82, I_REDO, "Redo", ToolCommand.REDO, !redoStack.isEmpty());
            drawToolButton(g, x + 176, 8, 96, I_SAVE, "Save", ToolCommand.SAVE, dirty);
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
            String[] tabs = {"Styles", "Computed", "HTML", "Layout", "Attributes", "Events", "Path"};
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
            for (Map.Entry<String, String> entry : e.computedStyle().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                y = drawComputedKV(g, x, y, entry.getKey(), entry.getValue());
            }
            return y;
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
            computedHits.add(new ComputedHit(row, valueBox, key, value));
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
                    g.setColor(TEXT);
                    g.drawString(clip(value, 160), x + 238, y + 17);
                }
            }
            return y + 26;
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
            if (!htmlSelecting) {
                handleMove(e);
                return;
            }
            int offset = htmlOffsetAt(e.getPoint());
            htmlSelectionFocus = offset;
            htmlCaretIndex = offset;
            repaint();
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
                for (ComputedHit hit : computedHits) if (hit.row.contains(e.getPoint()) || hit.valueRect.contains(e.getPoint())) {
                    startComputedEdit(hit);
                    repaint();
                    return;
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
                repaint();
                return;
            }
            for (RowHit hit : rowHits) if (hit.rect.contains(e.getPoint())) {
                selectedNodeId = hit.nodeId;
                rightScroll = 0;
                inspected.setDevToolsHighlightedNodeId(hit.nodeId);
                if (e.getClickCount() >= 2) toggleNode(hit.nodeId);
                repaint();
                return;
            }
        }

        private void startComputedEdit(ComputedHit hit) {
            if (hit == null) return;
            editingComputedProperty = hit.property;
            editingComputedValue = hit.value == null ? "" : hit.value;
            contextMenuOpen = false;
            requestFocusInWindow();
        }

        private void cancelComputedEdit() {
            if (editingComputedProperty.isBlank()) return;
            editingComputedProperty = "";
            editingComputedValue = "";
            repaint();
        }

        private void commitComputedEdit() {
            if (editingComputedProperty.isBlank()) return;
            UiDomNode node = nodeById(selectedNodeId);
            if (node instanceof UiDomElement element) {
                String property = editingComputedProperty;
                String value = editingComputedValue.trim();
                mutate(() -> HtmlDomDevToolsInlineStyle.applyRuntimeComputedStyle(element, property, value));
                saveStatus = "runtime style: " + property;
            }
            editingComputedProperty = "";
            editingComputedValue = "";
            rebuildRows();
            inspected.repaint();
            repaint();
        }

        private void handleKeyPressed(KeyEvent e) {
            if (htmlEditMode && "HTML".equals(activeTab)) {
                handleHtmlEditKeyPressed(e);
                return;
            }
            if (editingComputedProperty.isBlank()) return;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ENTER -> { commitComputedEdit(); e.consume(); }
                case KeyEvent.VK_ESCAPE -> { cancelComputedEdit(); e.consume(); }
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
            int node = nodeAt(e.getPoint());
            if (node <= 0) return;
            selectedNodeId = node;
            hoverNodeId = node;
            inspected.setDevToolsHighlightedNodeId(node);
            contextNodeId = node;
            contextMenuX = e.getX();
            contextMenuY = e.getY();
            contextMenuOpen = true;
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
                new MenuItem(MenuCommand.ADD_ATTRIBUTE, I_PLUS, "Добавить атрибут", element, false),
                new MenuItem(MenuCommand.EDIT_HTML, I_CODE, "Редактировать как HTML", element, false),
                new MenuItem(MenuCommand.DUPLICATE, I_CLONE, "Дублировать элемент", removable, false),
                new MenuItem(MenuCommand.DELETE, I_TRASH, "Удалить элемент", removable, false),
                MenuItem.separatorItem(),
                new MenuItem(MenuCommand.CUT, I_SCISSORS, "Вырезать", removable, false),
                new MenuItem(MenuCommand.COPY, I_COPY, "Скопировать", hasNode, false),
                new MenuItem(MenuCommand.PASTE, I_PASTE, "Вставить", clipboard != null && hasNode, false)
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
    private enum HtmlEditCommand { APPLY, CANCEL }
    private enum MenuCommand { ADD_ATTRIBUTE, EDIT_HTML, DUPLICATE, DELETE, CUT, COPY, PASTE }
    private record DomRow(UiDomNode node, int depth) { }
    private record RowHit(Rectangle rect, int nodeId) { }
    private record ToggleHit(Rectangle rect, int nodeId) { }
    private record TabHit(Rectangle rect, String tab) { }
    private record ComputedHit(Rectangle row, Rectangle valueRect, String property, String value) { }
    private record HtmlEditButtonHit(Rectangle rect, HtmlEditCommand command) { }
    private record HtmlLineHit(Rectangle row, int textX, int baseline, int startOffset, int endOffset, String text) { }
    private record ToolButtonHit(Rectangle rect, ToolCommand command, boolean enabled) { }
    private record MenuHit(Rectangle rect, MenuCommand command, boolean enabled) { }
    private record MenuItem(MenuCommand command, String icon, String label, boolean enabled, boolean separator) {
        static MenuItem separatorItem() { return new MenuItem(null, "", "", false, true); }
    }
}
