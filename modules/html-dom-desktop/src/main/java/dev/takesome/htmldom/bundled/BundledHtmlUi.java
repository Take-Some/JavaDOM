package dev.takesome.htmldom.bundled;

import dev.takesome.htmldom.desktop.HtmlDomSwingPanel;
import dev.takesome.htmldom.logging.HtmlDomLog;
import dev.takesome.htmldom.logging.HtmlDomLogger;

import javax.swing.JDialog;
import javax.swing.WindowConstants;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Starts the bundled HtmlDom desktop showcase in an ownerless desktop dialog. */
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
            JDialog window = new JDialog((java.awt.Frame) null, "HtmlDom Desktop HTML-like UI", false);
            window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    System.exit(0);
                }
            });
            window.setContentPane(new HtmlDomSwingPanel(markup, "", ROOT + "showcase.ui.html", ROOT));
            window.setMinimumSize(new Dimension(1080, 720));
            window.setSize(1280, 860);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
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
