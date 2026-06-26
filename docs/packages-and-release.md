# Packages and release process

HtmlDom publishes split Maven modules and an all-in-one executable package.

## Release identity

| Field | Value |
| --- | --- |
| Version | `1.0.2` |
| Release name | `CSS Length Tolerance Hotfix` |
| Authors | `Take Some()` |
| Group | `dev.takesome` |

## Version

Current documentary release version:

```text
1.0.2
```

## Maven coordinates

```gradle
dependencies {
    implementation 'dev.takesome:html-dom-aio:1.0.2'

    implementation 'dev.takesome:html-dom-core:1.0.2'
    implementation 'dev.takesome:html-dom-desktop:1.0.2'
    implementation 'dev.takesome:html-dom-fonts:1.0.2'
    implementation 'dev.takesome:html-dom-icons-fontawesome:1.0.2'
    implementation 'dev.takesome:html-dom-scripting-lua:1.0.2'
    implementation 'dev.takesome:html-dom-devtools:1.0.2'
}
```

## GitHub Packages repository

```gradle
repositories {
    maven {
        url = uri('https://maven.pkg.github.com/Take-Some/JavaDOM')
        credentials {
            username = findProperty('gpr.user') ?: System.getenv('GITHUB_ACTOR')
            password = findProperty('gpr.key') ?: System.getenv('GITHUB_TOKEN')
        }
    }
}
```

## Local verification

```bat
gradlew.bat clean test packageRelease publishToMavenLocal --console=plain --no-daemon
```

## Release trigger

GitHub release and package publication are triggered by tags:

```bat
git tag -a v1.0.2 -m "HtmlDom 1.0.2 — CSS Length Tolerance Hotfix"
git push origin main v1.0.2
```

GitHub Packages are immutable per package/version. The release script skips existing package versions that return `409 Conflict` and continues publishing missing artifacts.


## GitHub release body

The release workflow uses `RELEASE_NOTES.md` as the authored release body. Keep that file aligned with this document and the changelog before creating a new version tag.
