package dev.takesome.htmldom.icons.fontawesome;

/**
 * Built-in Font Awesome brands icon subset used by engine UI systems.
 */
public enum FontAwesomeBrandIcon implements FontAwesomeIcon {
    _42_GROUP("42-group", 0xe080),
    ACCESSIBLE_ICON("accessible-icon", 0xf368),
    ANDROID("android", 0xf17b),
    ANGULAR("angular", 0xf420),
    APPLE("apple", 0xf179),
    APP_STORE("app-store", 0xf36f),
    AWS("aws", 0xf375),
    BOOTSTRAP("bootstrap", 0xf836),
    CHROME("chrome", 0xf268),
    CLOUDFLARE("cloudflare", 0xe07d),
    CSS3("css3", 0xf13c),
    CSS3_ALT("css3-alt", 0xf38b),
    DEV("dev", 0xf6cc),
    DISCORD("discord", 0xf392),
    DOCKER("docker", 0xf395),
    EDGE("edge", 0xf282),
    FACEBOOK("facebook", 0xf230),
    FIGMA("figma", 0xf799),
    FIREFOX("firefox", 0xf269),
    GIT("git", 0xf1d3),
    GIT_ALT("git-alt", 0xf841),
    GITHUB("github", 0xf09b),
    GITHUB_ALT("github-alt", 0xf113),
    GITLAB("gitlab", 0xf296),
    GOOGLE("google", 0xf1a0),
    GOOGLE_DRIVE("google-drive", 0xf3aa),
    HTML5("html5", 0xf13b),
    INSTAGRAM("instagram", 0xf16d),
    JAVA("java", 0xf4e4),
    JS("js", 0xf3b8),
    LINUX("linux", 0xf17c),
    MARKDOWN("markdown", 0xf60f),
    MICROSOFT("microsoft", 0xf3ca),
    NODE("node", 0xf419),
    NODE_JS("node-js", 0xf3d3),
    NPM("npm", 0xf3d4),
    PHP("php", 0xf457),
    PYTHON("python", 0xf3e2),
    REACT("react", 0xf41b),
    REDDIT("reddit", 0xf1a1),
    RUST("rust", 0xe07a),
    SLACK("slack", 0xf3ef),
    STACK_OVERFLOW("stack-overflow", 0xf16c),
    STEAM("steam", 0xf1b6),
    TELEGRAM("telegram", 0xf3fe),
    TWITCH("twitch", 0xf1e8),
    UNITY("unity", 0xe049),
    VUEJS("vuejs", 0xf41f),
    WINDOWS("windows", 0xf17a),
    WORDPRESS("wordpress", 0xf19a),
    YOUTUBE("youtube", 0xf16a),
    TWITTER("twitter", 0xf099),
    VK("vk", 0xf189),
    YANDEX("yandex", 0xf413);


    private final String symbolicName;
    private final int codePoint;

    FontAwesomeBrandIcon(String symbolicName, int codePoint) {
        this.symbolicName = symbolicName;
        this.codePoint = codePoint;
    }

    @Override
    public String styleId() {
        return FontAwesomeStyle.BRANDS.styleId();
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
