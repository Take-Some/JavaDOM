package dev.takesome.htmldom.html;



import java.util.ArrayList;

import java.util.List;



/** Collects recoverable HTML parser diagnostics without aborting UI scene compilation. */

public final class UiHtmlDiagnosticSink {

    private final ArrayList<UiHtmlDiagnostic> diagnostics = new ArrayList<>();



    public void warn(String code, String message, int offset, int line, int column) {

        warn(code, message, offset, 1, line, column);

    }



    public void warn(String code, String message, int offset, int length, int line, int column) {

        diagnostics.add(new UiHtmlDiagnostic(UiHtmlDiagnosticSeverity.WARNING, code, message, offset, length, line, column));

    }



    public boolean hasDiagnostics() {

        return !diagnostics.isEmpty();

    }



    public List<UiHtmlDiagnostic> snapshot() {

        return List.copyOf(diagnostics);

    }

}
