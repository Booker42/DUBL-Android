# Changelog

## 0.2 — Integration, stabilization, and polish

- Integrated Skills, Development, Magic, and Equipment with shared character persistence and connected them to the Character sheet.
- Added the full mobile development workflow with prerequisites, branches, abilities, and ability-point budgeting.
- Added native magic, schools, spellbook management, mana formulas, spell-learning XP, and the local spell catalog.
- Added native equipment, quantities, carried state, custom items, automatic/manual load, and burden penalties.
- Corrected catalog equipment load to use item weight instead of the item requirement field.
- Prevented duplicate catalog equipment rows by increasing the existing item quantity.
- Added confirmation before deleting spells, magic schools, and equipment.
- Added actionable empty states and reset actions for filtered Skills/Development/Magic/Equipment screens.
- Improved long-name and large-font handling across the new screens.
- Increased bottom-navigation icon size.
- Extracted the dice engine into pure model code and added deterministic tests for doubles, critical failures, critical successes, superiority dice, and hindrance follow-ups.
- Added normalization and magic/equipment rule tests for 0.2 data.
- Added `v0.2` release-tag support and set the default app version to `0.2`.

## 0.1.4 — Skills

- Replaced the skills placeholder with a complete native mobile skills screen.
- Ported the 27-entry desktop DUBL base skill catalog and rank XP table.
- Added search, category filtering, trained-only filtering, and skill summary statistics.
- Added rank editing from 0 to 10 and a calculation breakdown for every skill.
- Added multiple selected attributes per skill.
- Added untrained, untrained −2, and unavailable-without-training handling.
- Added per-skill modifiers and calculation notes.
- Added hide/restore behavior that preserves skill data.
- Added Knowledge, Performance, Profession, and Craft specializations.
- Added custom user-defined skills.
- Persisted skill state in the local character snapshot with backward-compatible 0.1.3 loading.
- Added pure-model tests for multi-attribute totals, untrained rules, and XP costs.
- Removed the day-to-day `dubl-android` Git helper from the project workflow; normal Git commands are now documented.

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
- Pin Compose BOM to 2026.06.00 to keep compileSdk 36 compatibility in CI.
