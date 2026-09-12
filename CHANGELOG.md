# Changelog

## 0.1.3 — Native rebuild

- Replaced the WebView prototype with a real Kotlin + Jetpack Compose application.
- Raised compile/target SDK to Android API 36.
- Switched to AGP 9.4 and Gradle 9.6.
- Added native local character persistence.
- Added multiple-character creation, selection, and deletion.
- Added all eight primary DUBL attributes.
- Added character size and size-based Strength/Speed adjustment.
- Added DUBL-derived Defense, Health, Reflexes, Initiative, Fortitude, Run, and Ability Points.
- Added Health, Endurance, and optional Mana controls.
- Added responsive phone/tablet stat and attribute grids.
- Added unit tests for core derived-stat formulas.
- No Android permissions are requested.
- Added standalone GitHub Actions CI for automatic development APK builds.
- Added tag-driven signed GitHub Releases for Android only.
- Added separate `.dev` application ID for CI/test builds.
- Added one-command GitHub release-signing setup and a small `dubl-android` workflow helper.
