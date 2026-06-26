package dev.takesome.htmldom.desktop;

import dev.takesome.htmldom.dom.UiDomDocument;
import dev.takesome.htmldom.dom.UiDomElement;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

/** Paints modal/toast overlays and registers overlay hit targets. */
public final class HtmlDomOverlayPaintEngine {
    public void paintOverlays(Graphics2D g, Context context) {
        UiDomElement dialog = context.document().getElementById("showcase-dialog").orElse(null);
        if (dialog != null && dialog.classList().contains("open")) paintDialog(g, dialog, context);
        UiDomElement toast = context.document().getElementById("toast").orElse(null);
        if (toast != null && toast.classList().contains("open")) paintToast(g, toast, context);
    }

    public void paintDialog(Graphics2D g, UiDomElement dialog, Context context) {
        Rectangle screen = new Rectangle(0, 0, context.viewportWidth(), context.viewportHeight());
        int w = Math.min(480, Math.max(320, context.viewportWidth() - 80));
        int h = 230;
        Rectangle card = new Rectangle((context.viewportWidth() - w) / 2, (context.viewportHeight() - h) / 2, w, h);
        RoundRectangle2D.Float cardShape = new RoundRectangle2D.Float(card.x, card.y, card.width, card.height, 26, 26);

        UiDomElement closeElement = context.document().getElementById("close-dialog").orElse(dialog);
        Area backdropHit = new Area(screen);
        backdropHit.subtract(new Area(cardShape));
        context.hitTest().addHit(g, closeElement, backdropHit);

        g.setColor(new Color(0, 0, 0, 145));
        g.fillRect(screen.x, screen.y, screen.width, screen.height);
        g.setColor(context.paint().color("#121e34", Color.DARK_GRAY));
        g.fill(cardShape);
        g.setColor(context.paint().color("#50b7ff", Color.BLUE));
        g.draw(new RoundRectangle2D.Float(card.x, card.y, card.width - 1, card.height - 1, 26, 26));

        Shape oldClip = g.getClip();
        g.setClip(card.x, card.y, card.width, card.height);
        int y = card.y + 46;
        context.text().drawPlain(g, "Bundled component active", card.x + 24, y, 28, Font.BOLD, context.paint().color("#e5f0ff", Color.WHITE), card.width - 48);
        context.text().drawPlain(g, "This dialog is part of the same retained DOM and is painted directly by the desktop renderer.", card.x + 24, y + 42, 15, Font.PLAIN, context.paint().color("#93a4bb", Color.LIGHT_GRAY), card.width - 48);
        g.setClip(oldClip);

        Rectangle close = new Rectangle(card.x + card.width - 126, card.y + card.height - 62, 102, 38);
        context.hitTest().addHit(g, closeElement, close);
        g.setColor(context.paint().color("#0b73e8", Color.BLUE));
        g.fill(new RoundRectangle2D.Float(close.x, close.y, close.width, close.height, 19, 19));
        context.text().centerText(g, "Close", close, 13, Font.BOLD, Color.WHITE);
    }

    public void paintToast(Graphics2D g, UiDomElement toast, Context context) {
        int w = 280;
        int h = 48;
        Rectangle r = new Rectangle(context.viewportWidth() - w - 22, context.viewportHeight() - h - 22, w, h);
        g.setColor(context.paint().color("#121e34", Color.DARK_GRAY));
        g.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 16, 16));
        g.setColor(context.paint().color("#50b7ff", Color.BLUE));
        g.draw(new RoundRectangle2D.Float(r.x, r.y, r.width - 1, r.height - 1, 16, 16));
        context.text().drawPlain(g, context.text().directText(toast), r.x + 16, r.y + 30, 14, Font.BOLD, context.paint().color("#e5f0ff", Color.WHITE), r.width - 32);
    }

    public record Context(
            UiDomDocument document,
            HtmlDomPaintEngine paint,
            HtmlDomTextPaintEngine text,
            HtmlDomHitTestEngine hitTest,
            int viewportWidth,
            int viewportHeight
    ) { }
}
