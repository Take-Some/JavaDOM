# Changelog

## 1.0.2 — CSS Length Tolerance Hotfix

Hotfix release for malformed CSS length handling in the layout pipeline.

**Authors:** Take Some()

### Fixed

- Invalid CSS length values now emit a warning and fall back instead of aborting layout.
- Multi-token mistakes such as `gap: 5px 10` no longer propagate `IllegalArgumentException` out of layout.
- Computed length accessors now return their provided fallback for malformed length values.

### Tests

- Added regression coverage for invalid `gap` length input in `html-dom-core`.

### Release

- Version bumped to `1.0.2`.
- Release notes for `1.0.2 CSS Length Tolerance Hotfix`.

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
