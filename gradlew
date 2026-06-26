#!/usr/bin/env sh
set -eu
GRADLE_VERSION="9.5.1"
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BOOT_DIR="$BASE_DIR/.gradle/bootstrap"
GRADLE_BIN="$BOOT_DIR/gradle-$GRADLE_VERSION/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
  echo "[bootstrap] Gradle $GRADLE_VERSION not found. Downloading local distribution..."
  mkdir -p "$BOOT_DIR"
  ZIP="$BOOT_DIR/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then curl -L -o "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"; elif command -v wget >/dev/null 2>&1; then wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"; else echo "curl or wget is required to bootstrap Gradle." >&2; exit 1; fi
  unzip -oq "$ZIP" -d "$BOOT_DIR"
fi
exec "$GRADLE_BIN" "$@"
