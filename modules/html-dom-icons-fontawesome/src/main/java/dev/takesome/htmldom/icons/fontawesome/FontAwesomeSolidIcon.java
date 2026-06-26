package dev.takesome.htmldom.icons.fontawesome;

/**
 * Built-in Font Awesome solid icon subset used by engine UI systems.
 */
public enum FontAwesomeSolidIcon implements FontAwesomeIcon {
    CIRCLE("circle", 0xf111),
    CIRCLE_DOT("circle-dot", 0xf192),
    CIRCLE_INFO("circle-info", 0xf05a),
    CIRCLE_CHECK("circle-check", 0xf058),
    CIRCLE_XMARK("circle-xmark", 0xf057),
    CIRCLE_EXCLAMATION("circle-exclamation", 0xf06a),
    WINDOW_MINIMIZE("window-minimize", 0xf2d1),
    WINDOW_MAXIMIZE("window-maximize", 0xf2d0),
    COPY("copy", 0xf0c5),
    ROTATE_LEFT("rotate-left", 0xf2ea),
    ROTATE_RIGHT("rotate-right", 0xf2f9),
    TRIANGLE_EXCLAMATION("triangle-exclamation", 0xf071),
    BUG("bug", 0xf188),
    GEAR("gear", 0xf013),
    GEARS("gears", 0xf085),
    WRENCH("wrench", 0xf0ad),
    SCREWDRIVER_WRENCH("screwdriver-wrench", 0xf7d9),
    POWER_OFF("power-off", 0xf011),
    ROTATE("rotate", 0xf2f1),
    ARROWS_ROTATE("arrows-rotate", 0xf021),
    DOWNLOAD("download", 0xf019),
    UPLOAD("upload", 0xf093),
    FLOPPY_DISK("floppy-disk", 0xf0c7),
    TRASH("trash", 0xf1f8),
    PEN("pen", 0xf304),
    PEN_TO_SQUARE("pen-to-square", 0xf044),
    PLUS("plus", 0xf067),
    MINUS("minus", 0xf068),
    CHECK("check", 0xf00c),
    XMARK("xmark", 0xf00d),
    PLAY("play", 0xf04b),
    PAUSE("pause", 0xf04c),
    STOP("stop", 0xf04d),
    FORWARD("forward", 0xf04e),
    BACKWARD("backward", 0xf04a),
    HOUSE("house", 0xf015),
    USER("user", 0xf007),
    USERS("users", 0xf0c0),
    USER_GEAR("user-gear", 0xf4fe),
    LOCK("lock", 0xf023),
    UNLOCK("unlock", 0xf09c),
    KEY("key", 0xf084),
    SHIELD("shield", 0xf132),
    SHIELD_HALVED("shield-halved", 0xf3ed),
    EYE("eye", 0xf06e),
    EYE_SLASH("eye-slash", 0xf070),
    BELL("bell", 0xf0f3),
    ENVELOPE("envelope", 0xf0e0),
    COMMENT("comment", 0xf075),
    COMMENTS("comments", 0xf086),
    MAGNIFYING_GLASS("magnifying-glass", 0xf002),
    FILTER("filter", 0xf0b0),
    SORT("sort", 0xf0dc),
    BARS("bars", 0xf0c9),
    LIST("list", 0xf03a),
    TABLE("table", 0xf0ce),
    FOLDER("folder", 0xf07b),
    FOLDER_OPEN("folder-open", 0xf07c),
    FILE("file", 0xf15b),
    FILE_LINES("file-lines", 0xf15c),
    DATABASE("database", 0xf1c0),
    SERVER("server", 0xf233),
    MICROCHIP("microchip", 0xf2db),
    TERMINAL("terminal", 0xf120),
    CODE("code", 0xf121),
    LINK("link", 0xf0c1),
    CALENDAR("calendar", 0xf133),
    CLOCK("clock", 0xf017),
    LOCATION_DOT("location-dot", 0xf3c5),
    MAP("map", 0xf279),
    IMAGE("image", 0xf03e),
    GAMEPAD("gamepad", 0xf11b),
    CROSSHAIRS("crosshairs", 0xf05b),
    PERSON_RUNNING("person-running", 0xf70c),
    BOLT("bolt", 0xf0e7),
    FIRE("fire", 0xf06d),
    SKULL("skull", 0xf54c),
    ROBOT("robot", 0xf544),
    BRAIN("brain", 0xf5dc),
    HEART("heart", 0xf004),
    STAR("star", 0xf005),
    VOLUME_HIGH("volume-high", 0xf028),
    MUSIC("music", 0xf001),
    ARROW_LEFT("arrow-left", 0xf060),
    ARROW_RIGHT("arrow-right", 0xf061),
    ARROW_UP("arrow-up", 0xf062),
    ARROW_DOWN("arrow-down", 0xf063),
    CHEVRON_LEFT("chevron-left", 0xf053),
    CHEVRON_RIGHT("chevron-right", 0xf054),
    CHEVRON_UP("chevron-up", 0xf077),
    CHEVRON_DOWN("chevron-down", 0xf078);


    private final String symbolicName;
    private final int codePoint;

    FontAwesomeSolidIcon(String symbolicName, int codePoint) {
        this.symbolicName = symbolicName;
        this.codePoint = codePoint;
    }

    @Override
    public String styleId() {
        return FontAwesomeStyle.SOLID.styleId();
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
