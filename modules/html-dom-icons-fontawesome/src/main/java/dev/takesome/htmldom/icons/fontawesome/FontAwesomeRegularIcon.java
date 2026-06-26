package dev.takesome.htmldom.icons.fontawesome;

/**
 * Built-in Font Awesome regular icon subset used by engine UI systems.
 */
public enum FontAwesomeRegularIcon implements FontAwesomeIcon {
    ADDRESS_BOOK("address-book", 0xf2ba),
    ADDRESS_CARD("address-card", 0xf2bc),
    BELL("bell", 0x1f514),
    BOOKMARK("bookmark", 0x1f516),
    CALENDAR("calendar", 0x1f4c6),
    CALENDAR_CHECK("calendar-check", 0xf274),
    CALENDAR_DAYS("calendar-days", 0xf073),
    CHART_BAR("chart-bar", 0xf080),
    CHESS_KING("chess-king", 0xf43f),
    CIRCLE("circle", 0x1f7e4),
    CIRCLE_CHECK("circle-check", 0xf05d),
    CIRCLE_DOT("circle-dot", 0x1f518),
    CIRCLE_DOWN("circle-down", 0xf358),
    CIRCLE_LEFT("circle-left", 0xf359),
    CIRCLE_PAUSE("circle-pause", 0xf28c),
    CIRCLE_PLAY("circle-play", 0xf144),
    CIRCLE_QUESTION("circle-question", 0xf29c),
    CIRCLE_RIGHT("circle-right", 0xf35a),
    CIRCLE_STOP("circle-stop", 0xf28e),
    CIRCLE_UP("circle-up", 0xf35b),
    CIRCLE_USER("circle-user", 0xf2be),
    CIRCLE_XMARK("circle-xmark", 0xf05c),
    CLIPBOARD("clipboard", 0x1f4cb),
    CLONE("clone", 0xf24d),
    CLOCK("clock", 0x1f553),
    COMMENT("comment", 0x1f5e9),
    COMMENTS("comments", 0x1f5ea),
    COMPASS("compass", 0x1f9ed),
    COPY("copy", 0xf0c5),
    COPYRIGHT("copyright", 0xf1f9),
    ENVELOPE("envelope", 0x1f582),
    EYE("eye", 0x1f441),
    FACE_SMILE("face-smile", 0x1f642),
    FILE("file", 0x1f5cb),
    FILE_AUDIO("file-audio", 0xf1c7),
    FILE_CODE("file-code", 0xf1c9),
    FILE_EXCEL("file-excel", 0xf1c3),
    FILE_IMAGE("file-image", 0x1f5bb),
    FILE_LINES("file-lines", 0x1f5ce),
    FILE_PDF("file-pdf", 0xf1c1),
    FILE_POWERPOINT("file-powerpoint", 0xf1c4),
    FILE_VIDEO("file-video", 0xf1c8),
    FILE_WORD("file-word", 0xf1c2),
    FLAG("flag", 0x1f3f4),
    FLOPPY_DISK("floppy-disk", 0x1f5aa),
    FOLDER("folder", 0x1f5bf),
    FOLDER_OPEN("folder-open", 0x1f5c1),
    FONT_AWESOME("font-awesome", 0xf4e6),
    GEM("gem", 0x1f48e),
    HAND("hand", 0x1f91a),
    HANDSHAKE("handshake", 0xf2b5),
    HEART("heart", 0x1f9e1),
    HOURGLASS("hourglass", 0xf254),
    ID_BADGE("id-badge", 0xf2c1),
    ID_CARD("id-card", 0xf2c3),
    IMAGE("image", 0xf03e),
    KEYBOARD("keyboard", 0xf11c),
    LEMON("lemon", 0x1f34b),
    LIGHTBULB("lightbulb", 0x1f4a1),
    MAP("map", 0x1f5fa),
    MESSAGE("message", 0xf27a),
    MONEY_BILL_1("money-bill-1", 0xf3d1),
    MOON("moon", 0x1f319),
    NEWSPAPER("newspaper", 0x1f4f0),
    NOTE_STICKY("note-sticky", 0xf24a),
    OBJECT_GROUP("object-group", 0xf247),
    PAPER_PLANE("paper-plane", 0xf1d9),
    PASTE("paste", 0xf0ea),
    PEN_TO_SQUARE("pen-to-square", 0xf044),
    RECTANGLE_LIST("rectangle-list", 0xf022),
    RECTANGLE_XMARK("rectangle-xmark", 0xf410),
    REGISTERED("registered", 0xf25d),
    SHARE_FROM_SQUARE("share-from-square", 0xf14d),
    SNOWFLAKE("snowflake", 0xf2dc),
    SQUARE("square", 0xf0c8),
    SQUARE_CARET_DOWN("square-caret-down", 0xf150),
    SQUARE_CARET_LEFT("square-caret-left", 0xf191),
    SQUARE_CARET_RIGHT("square-caret-right", 0xf152),
    SQUARE_CARET_UP("square-caret-up", 0xf151),
    SQUARE_CHECK("square-check", 0xf14a),
    SQUARE_FULL("square-full", 0x1f7eb),
    STAR("star", 0xf006),
    STAR_HALF("star-half", 0xf123),
    THUMBS_DOWN("thumbs-down", 0x1f44e),
    THUMBS_UP("thumbs-up", 0x1f44d),
    TRASH_CAN("trash-can", 0xf2ed),
    USER("user", 0x1f464),
    WINDOW_MAXIMIZE("window-maximize", 0x1f5d6),
    WINDOW_MINIMIZE("window-minimize", 0x1f5d5),
    WINDOW_RESTORE("window-restore", 0xf2d2);


    private final String symbolicName;
    private final int codePoint;

    FontAwesomeRegularIcon(String symbolicName, int codePoint) {
        this.symbolicName = symbolicName;
        this.codePoint = codePoint;
    }

    @Override
    public String styleId() {
        return FontAwesomeStyle.REGULAR.styleId();
    }

    @Override
    public String symbolicName() {
        return symbolicName;
    }

    @Override
    public int codePoint() {
        return codePoint;
    }

}
