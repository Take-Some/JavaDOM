package dev.takesome.htmldom.html;



import java.util.Objects;



/** Structured non-fatal HTML parser/runtime diagnostic with source-span coordinates. */

public final class UiHtmlDiagnostic {

    private final UiHtmlDiagnosticSeverity severity;

    private final String code;

    private final String message;

    private final int offset;

    private final int length;

    private final int line;

    private final int column;



    public UiHtmlDiagnostic(

            UiHtmlDiagnosticSeverity severity,

            String code,

            String message,

            int offset,

            int line,

            int column

    ) {

        this(severity, code, message, offset, 1, line, column);

    }



    public UiHtmlDiagnostic(

            UiHtmlDiagnosticSeverity severity,

            String code,

            String message,

            int offset,

            int length,

            int line,

            int column

    ) {

        this.severity = severity == null ? UiHtmlDiagnosticSeverity.WARNING : severity;

        this.code = Objects.requireNonNullElse(code, "html.diagnostic");

        this.message = Objects.requireNonNullElse(message, "");

        this.offset = Math.max(0, offset);

        this.length = Math.max(1, length);

        this.line = Math.max(1, line);

        this.column = Math.max(1, column);

    }



    public UiHtmlDiagnosticSeverity severity() { return severity; }

    public String code() { return code; }

    public String message() { return message; }

    public int offset() { return offset; }

    public int length() { return length; }

    public int endOffset() { return offset + length; }

    public int line() { return line; }

    public int column() { return column; }



    public String jumpTarget() {

        return line + ":" + column;

    }

}
