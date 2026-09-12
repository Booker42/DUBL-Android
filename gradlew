#!/usr/bin/env sh
set -eu

# Lightweight bootstrap for this generated project. Android Studio can use
# gradle/wrapper/gradle-wrapper.properties directly; this script keeps CLI use simple.
VERSION="9.6.0"
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/dubl-bootstrap"
HOME_DIR="$BASE/gradle-$VERSION"
ZIP="$BASE/gradle-$VERSION-bin.zip"
URL="https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"

if [ ! -x "$HOME_DIR/bin/gradle" ]; then
  mkdir -p "$BASE"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "$URL"
  else
    echo "curl or wget is required to bootstrap Gradle." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP" -d "$BASE"
  else
    echo "unzip is required to bootstrap Gradle." >&2
    exit 1
  fi
fi

exec "$HOME_DIR/bin/gradle" "$@"
