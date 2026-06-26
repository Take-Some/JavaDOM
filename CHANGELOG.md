# Changelog

## 1.0.1 — Documentary Release

This release focuses on documentation and release hygiene for the standalone HtmlDom repository.

**Authors:** Take Some()

### Added

- Documentation index in the README.
- Architecture overview for the retained DOM, CSS, layout, paint and desktop runtime layers.
- Desktop runtime documentation for the Swing/JFrame renderer, input routing, transforms, transitions and overlay painting.
- CSS/layout/paint pipeline documentation.
- DevTools documentation covering the custom Java2D Elements viewer, runtime editing, HTML editing, box-model overlay and event log.
- Lua scripting ABI notes.
- Package and release process documentation, including split Maven modules and the AIO Maven package.
- Release notes for `1.0.1 Documentary Release`.
- Repository authors file with `Take Some()` as the release author.

### Release

- Version bumped to `1.0.1`.
- Release workflow artifact names are version-tag aware.
- GitHub Packages publishing remains idempotent for immutable package versions.

## 1.0.0

Initial standalone HtmlDom desktop runtime release.
