package dev.takesome.htmldom.bundled;

import dev.takesome.htmldom.desktop.HtmlDomSwingPanel;
import dev.takesome.htmldom.logging.HtmlDomLog;
import dev.takesome.htmldom.logging.HtmlDomLogger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Starts the bundled HtmlDom desktop showcase in a JFrame. */
public final class BundledHtmlUi {
    private static final String ROOT = "html-dom/bundled/";
    private static final HtmlDomLogger LOG = HtmlDomLog.logger(BundledHtmlUi.class);

    private BundledHtmlUi() {
    }

    public static void main(String[] args) {
        String markup = resourceText(ROOT + "showcase.ui.html");
        if (GraphicsEnvironment.isHeadless()) {
            LOG.info("HtmlDom desktop UI is ready, but the environment is headless.");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("HtmlDom Desktop HTML-like UI");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new HtmlDomSwingPanel(markup, "", ROOT + "showcase.ui.html", ROOT));
            frame.setMinimumSize(new Dimension(1080, 720));
            frame.setSize(1280, 860);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static String resourceText(String path) {
        try (InputStream stream = BundledHtmlUi.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Resource not found: " + path);
            byte[] bytes = stream.readAllBytes();
            LOG.info("HtmlDom bundled resource loaded path='{}' bytes={}", path, bytes.length);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read resource: " + path, exception);
        }
    }
}
