package dev.takesome.htmldom.bundled;

import dev.takesome.htmldom.desktop.HtmlDomConfig;
import dev.takesome.htmldom.desktop.HtmlDomSwingPanel;
import dev.takesome.htmldom.logging.HtmlDomLog;
import dev.takesome.htmldom.logging.HtmlDomLogger;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Demonstrates animated HtmlDom UI from a self-contained HTML document plus Lua control logic. */
public final class HtmlOnlyAnimationDemo {
    private static final HtmlDomLogger LOG = HtmlDomLog.logger(HtmlOnlyAnimationDemo.class);

    private HtmlOnlyAnimationDemo() {
    }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            LOG.info("HtmlDom HTML-only animation demo is ready, but the environment is headless.");
            return;
        }
        SwingUtilities.invokeLater(HtmlOnlyAnimationDemo::open);
    }

    private static void open() {
        HtmlDomConfig config = HtmlDomConfig.defaults()
                .withAllowDevTools(HtmlDomConfig.DevToolsAvailability.ENABLED)
                .withPreferredSize(1120, 720)
                .withRenderQuality(HtmlDomConfig.RenderQuality.HIGH_QUALITY)
                .withAnimationFrameIntervalMs(16)
                .withAnimationWorkerLimit(4);
        HtmlDomSwingPanel panel = new HtmlDomSwingPanel(html(), "", "html-only-animation-demo.html", "", config);
        panel.executeLua(lua(), "html-only-animation-demo.lua");

        JDialog window = new JDialog((java.awt.Frame) null, "HtmlDom HTML + Lua UI Demo", false);
        window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { System.exit(0); }
        });
        window.setContentPane(panel);
        window.setMinimumSize(new Dimension(960, 640));
        window.setSize(1200, 780);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        panel.requestPanelFocus();
    }

    public static String lua() {
        return """
                local clicks = 0
                local mode = "idle"

                local function set_status(text)
                    dom.setText("lua-status", text)
                    dom.setText("event-count", tostring(clicks))
                end

                local function select_mode(next_mode)
                    mode = next_mode
                    dom.setText("mode-label", string.upper(next_mode))
                    dom.removeClass("core", "hot")
                    dom.removeClass("core", "cold")
                    dom.removeClass("core", "scan")
                    dom.removeClass("stage", "boosted")
                    if next_mode == "hot" then
                        dom.addClass("core", "hot")
                        dom.addClass("stage", "boosted")
                    elseif next_mode == "cold" then
                        dom.addClass("core", "cold")
                    elseif next_mode == "scan" then
                        dom.addClass("core", "scan")
                    end
                end

                function afterClick(action, id)
                    if action == "demo.ignite" then
                        clicks = clicks + 1
                        select_mode("hot")
                        dom.setText("primary-readout", "Lua ignition sequence armed")
                        dom.setText("secondary-readout", "Core temperature rising")
                        set_status("Lua handled " .. action .. " from #" .. id)
                    elseif action == "demo.cool" then
                        clicks = clicks + 1
                        select_mode("cold")
                        dom.setText("primary-readout", "Cooling loop engaged")
                        dom.setText("secondary-readout", "Thermal pressure nominal")
                        set_status("Lua cooled the UI state from #" .. id)
                    elseif action == "demo.scan" then
                        clicks = clicks + 1
                        select_mode("scan")
                        dom.setText("primary-readout", "Diagnostic scan running")
                        dom.setText("secondary-readout", "All HtmlDom nodes responsive")
                        set_status("Lua scan completed through DOM bindings")
                    elseif action == "demo.reset" then
                        clicks = clicks + 1
                        select_mode("idle")
                        dom.setText("primary-readout", "System reset to idle")
                        dom.setText("secondary-readout", "Awaiting operator command")
                        set_status("Lua reset all live demo state")
                    elseif action == "demo.toast" then
                        clicks = clicks + 1
                        dom.toggleClass("toast", "open")
                        dom.setText("toast", "Lua toast toggled by #" .. id)
                        set_status("Lua toggled toast visibility")
                    elseif action == "demo.tab" then
                        clicks = clicks + 1
                        set_status("Lua observed tab button #" .. id)
                    end
                end
                """;
    }

    public static String html() {
        return """
                <html id="demo-root">
                <head>
                    <title>HtmlDom HTML + Lua UI Demo</title>
                    <style>
                        html, body {
                            display: block;
                            margin: 0;
                            padding: 0;
                            width: 100%;
                            height: 100%;
                            background-color: #070b12;
                            color: #edf6ff;
                            font-family: FS Elliot Pro;
                            overflow: hidden;
                        }

                        body { padding: 30px; background-color: #070b12; }

                        .stage {
                            display: block;
                            position: relative;
                            width: 100%;
                            min-height: 640px;
                            padding: 26px;
                            border: 1px solid #28445d;
                            border-radius: 30px;
                            background-color: #0d1724;
                            box-shadow: 0 22px 70px #000000cc;
                            overflow: hidden;
                            transition: background-color 220ms ease-out, border-color 220ms ease-out, transform 220ms ease-out, opacity 220ms ease-out;
                        }

                        .stage.boosted { background-color: #162237; border-color: #f6c661; transform: scale(1.004); }

                        .aurora-a, .aurora-b, .aurora-c {
                            display: block;
                            position: absolute;
                            width: 260px;
                            height: 260px;
                            border-radius: 130px;
                            opacity: 0.54;
                            background-color: #2dbdff;
                            animation-name: orbFloat;
                            animation-duration: 5200ms;
                            animation-timing-function: ease-in-out;
                            animation-iteration-count: infinite;
                            animation-direction: alternate;
                            pointer-events: none;
                        }

                        .aurora-a { left: 675px; top: 18px; background-color: #2dbdff; }
                        .aurora-b { left: 810px; top: 315px; background-color: #ffca57; animation-duration: 6700ms; }
                        .aurora-c { left: 500px; top: 430px; background-color: #7fffd4; animation-duration: 7400ms; }

                        .header { display: block; position: relative; width: 630px; padding: 8px 0 18px 0; animation-name: fadeInUp; animation-duration: 520ms; animation-timing-function: ease-out; }
                        .eyebrow {
                            display: inline-block;
                            padding: 7px 12px;
                            border: 1px solid #4a769b;
                            border-radius: 999px;
                            color: #9fdcff;
                            background-color: #102338;
                            font-size: 13px;
                            letter-spacing: 2px;
                            text-transform: uppercase;
                        }
                        h1 { display: block; margin: 20px 0 8px 0; color: #ffffff; font-size: 44px; line-height: 50px; font-weight: 800; }
                        .subtitle { display: block; width: 610px; color: #b9c7d9; font-size: 17px; line-height: 27px; }

                        .controls {
                            display: block;
                            position: relative;
                            width: 650px;
                            margin-top: 20px;
                            padding: 18px;
                            border: 1px solid #243e58;
                            border-radius: 24px;
                            background-color: #0f1c2d;
                            animation-name: fadeInUp;
                            animation-duration: 680ms;
                            animation-timing-function: ease-out;
                            animation-delay: 90ms;
                        }

                        .control-title { display: block; color: #ffffff; font-size: 20px; font-weight: 800; margin-bottom: 10px; }
                        .control-copy { display: block; color: #aab8c9; font-size: 14px; line-height: 21px; margin-bottom: 14px; }

                        button, .button {
                            display: inline-block;
                            min-width: 108px;
                            padding: 12px 16px;
                            margin-right: 10px;
                            margin-bottom: 10px;
                            border: 1px solid #46779e;
                            border-radius: 14px;
                            background-color: #162b43;
                            color: #f5fbff;
                            font-size: 14px;
                            cursor: pointer;
                            transition: transform 150ms ease-out, background-color 150ms ease-out, border-color 150ms ease-out, color 150ms ease-out;
                        }

                        button:hover, .button:hover { transform: translateY(-3px); background-color: #1e4264; border-color: #75caff; }
                        button:active, .button:active { transform: translateY(1px) scale(0.98); background-color: #08111c; }
                        button.primary { background-color: #214f78; border-color: #75caff; }
                        button.warn { background-color: #5a3515; border-color: #ffd06a; }
                        button.danger { background-color: #401927; border-color: #ff7aa8; }

                        .readouts {
                            display: block;
                            position: relative;
                            width: 650px;
                            margin-top: 18px;
                        }

                        .panel {
                            display: inline-block;
                            vertical-align: top;
                            width: 286px;
                            min-height: 135px;
                            margin-right: 18px;
                            margin-bottom: 18px;
                            padding: 18px;
                            border: 1px solid #243e58;
                            border-radius: 22px;
                            background-color: #111e2e;
                            box-shadow: 0 12px 30px #00000070;
                            transition: transform 180ms ease-out, border-color 180ms ease-out, background-color 180ms ease-out, opacity 180ms ease-out;
                            animation-name: fadeInUp;
                            animation-duration: 760ms;
                            animation-timing-function: ease-out;
                            animation-delay: 150ms;
                        }

                        .panel:hover { transform: translateY(-5px) scale(1.015); border-color: #5abfff; background-color: #14273d; }
                        .panel-title { display: block; color: #ffffff; font-size: 18px; font-weight: 700; margin-bottom: 10px; }
                        .panel-copy { display: block; color: #aab8c9; font-size: 14px; line-height: 21px; }
                        .metric { display: block; margin-top: 15px; font-size: 30px; color: #ffd06a; font-weight: 800; }
                        .bar { display: block; width: 100%; height: 9px; margin-top: 14px; border-radius: 999px; background-color: #08111c; overflow: hidden; }
                        .bar-fill { display: block; width: 70%; height: 9px; border-radius: 999px; background-color: #48c8ff; animation-name: barPulse; animation-duration: 1800ms; animation-timing-function: ease-in-out; animation-iteration-count: infinite; animation-direction: alternate; }

                        .tab-strip { display: block; position: relative; width: 650px; height: 172px; margin-top: 4px; animation-name: fadeInUp; animation-duration: 860ms; animation-timing-function: ease-out; animation-delay: 210ms; }
                        .tab-panel {
                            display: block;
                            position: absolute;
                            left: 0;
                            top: 58px;
                            width: 610px;
                            min-height: 74px;
                            padding: 16px;
                            border: 1px solid #263f5a;
                            border-radius: 18px;
                            background-color: #0a1524;
                            color: #bed0e5;
                            font-size: 14px;
                            line-height: 22px;
                            opacity: 0;
                            transform: translateY(18px);
                            transition: opacity 180ms ease-out, transform 180ms ease-out, border-color 180ms ease-out, background-color 180ms ease-out;
                        }
                        .tab-panel.active { opacity: 1; transform: translateY(0px); border-color: #5abfff; background-color: #0d1d30; }

                        input, select, progress {
                            display: inline-block;
                            margin-right: 10px;
                            margin-top: 8px;
                            padding: 8px 10px;
                            border: 1px solid #395d7e;
                            border-radius: 12px;
                            background-color: #0b1728;
                            color: #dcefff;
                        }
                        input { width: 190px; }
                        select { width: 145px; }
                        progress { width: 180px; height: 22px; }

                        .beacon {
                            display: block;
                            position: absolute;
                            left: 760px;
                            top: 112px;
                            width: 214px;
                            height: 214px;
                            border-radius: 107px;
                            border: 1px solid #f7ca6b;
                            background-color: #ffca5744;
                            animation-name: beaconPulse;
                            animation-duration: 1450ms;
                            animation-timing-function: ease-in-out;
                            animation-iteration-count: infinite;
                            animation-direction: alternate;
                        }

                        .core {
                            display: block;
                            position: absolute;
                            left: 827px;
                            top: 179px;
                            width: 80px;
                            height: 80px;
                            border-radius: 40px;
                            background-color: #ffe5a1;
                            box-shadow: 0 0 40px #ffd166dd;
                            animation-name: coreSpin;
                            animation-duration: 2600ms;
                            animation-timing-function: linear;
                            animation-iteration-count: infinite;
                            transition: background-color 180ms ease-out, opacity 180ms ease-out, transform 180ms ease-out, border-color 180ms ease-out;
                        }
                        .core.hot { background-color: #ff7a45; transform: scale(1.10); }
                        .core.cold { background-color: #8fd9ff; transform: scale(0.94); }
                        .core.scan { background-color: #a78bfa; transform: scale(1.04); }

                        .footer-note { display: block; position: absolute; left: 740px; top: 372px; width: 330px; color: #c7d2e0; font-size: 15px; line-height: 24px; animation-name: fadeInUp; animation-duration: 920ms; animation-timing-function: ease-out; animation-delay: 260ms; }
                        .toast { display: block; position: absolute; left: 760px; top: 510px; width: 310px; padding: 16px; border: 1px solid #75caff; border-radius: 18px; background-color: #102338; color: #eaf8ff; opacity: 0; transform: translateY(18px); transition: opacity 160ms ease-out, transform 160ms ease-out; }
                        .toast.open { opacity: 1; transform: translateY(0px); }

                        @keyframes fadeInUp { from { opacity: 0; transform: translateY(24px); } to { opacity: 1; transform: translateY(0px); } }
                        @keyframes orbFloat { from { transform: translate(0px, 0px) scale(0.90); opacity: 0.32; } to { transform: translate(32px, -26px) scale(1.08); opacity: 0.60; } }
                        @keyframes barPulse { from { opacity: 0.54; } to { opacity: 1.0; } }
                        @keyframes beaconPulse { from { transform: scale(0.94); opacity: 0.28; } to { transform: scale(1.08); opacity: 0.72; } }
                        @keyframes coreSpin { from { transform: rotate(0deg) scale(0.94); } to { transform: rotate(360deg) scale(1.08); } }
                    </style>
                </head>
                <body>
                    <main class="stage" id="stage">
                        <div class="aurora-a"></div><div class="aurora-b"></div><div class="aurora-c"></div>
                        <section class="header">
                            <span class="eyebrow">HTML UI / CSS ANIMATION / LUA CONTROL</span>
                            <h1>Interactive HtmlDom control shell.</h1>
                            <p class="subtitle">Buttons, tabs, input-like controls, status panels, transitions and keyframes are rendered from this HTML document. Lua mutates the DOM state.</p>
                        </section>

                        <section class="controls">
                            <span class="control-title">Operator controls</span>
                            <span class="control-copy">Click buttons to dispatch HtmlDom events. Lua updates classes and text through DOM bindings.</span>
                            <button id="btn-ignite" class="primary" data-action="demo.ignite">Ignite</button>
                            <button id="btn-cool" data-action="demo.cool">Cool</button>
                            <button id="btn-scan" data-action="demo.scan">Scan</button>
                            <button id="btn-toast" data-action="demo.toast">Toast</button>
                            <button id="btn-reset" class="danger" data-action="demo.reset">Reset</button>
                            <br>
                            <input id="operator-input" value="lua://dom-ready">
                            <select id="profile-select"><option>Balanced</option><option>Performance</option><option>Silent</option></select>
                            <progress id="demo-progress" value="68" max="100"></progress>
                        </section>

                        <section class="readouts">
                            <div class="panel">
                                <span class="panel-title">Mode</span>
                                <span class="panel-copy" id="primary-readout">Awaiting operator command</span>
                                <span class="metric" id="mode-label">IDLE</span>
                                <span class="bar"><span class="bar-fill"></span></span>
                            </div>
                            <div class="panel">
                                <span class="panel-title">Lua events</span>
                                <span class="panel-copy" id="secondary-readout">No Lua command handled yet</span>
                                <span class="metric" id="event-count">0</span>
                                <span class="bar"><span class="bar-fill"></span></span>
                            </div>
                            <p class="panel-copy" id="lua-status">Lua runtime loaded. Waiting for data-action.</p>
                        </section>

                        <section class="tab-strip">
                            <button data-tab="diagnostics" data-action="demo.tab" id="tab-diagnostics" class="active">Diagnostics</button>
                            <button data-tab="lua" data-action="demo.tab" id="tab-lua">Lua DOM</button>
                            <button data-tab="paint" data-action="demo.tab" id="tab-paint">Paint</button>
                            <div class="tab-panel active" data-panel="diagnostics">Diagnostics panel: CSS keyframes are active, transitions are hover-driven, and controls dispatch through HtmlDom event routing.</div>
                            <div class="tab-panel" data-panel="lua">Lua panel: afterClick(action, id) receives button actions and mutates text/classes using dom.setText, dom.addClass, dom.removeClass and dom.toggleClass.</div>
                            <div class="tab-panel" data-panel="paint">Paint panel: animation tick prepares runtime effects before Java2D paint reads the current snapshot.</div>
                        </section>

                        <div class="beacon"></div><div class="core" id="core"></div>
                        <p class="footer-note">This is no longer a decorative animation page. It is an HTML UI shell with real controls, native event dispatch and Lua-driven state.</p>
                        <p class="toast" id="toast">Lua toast</p>
                    </main>
                </body>
                </html>
                """;
    }
}
