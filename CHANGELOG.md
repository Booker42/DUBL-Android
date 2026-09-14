# Changelog

## 0.5 — Rule-aware rolls, Chi techniques, and sheet integration

- Added a reusable roll-context layer for attributes, Reflexes, Initiative, Dodge, raw attacks, Parry, Feint, Grapple, Disarm, Trip, Push, Knockdown, and Break Item checks without introducing combat-state tracking.
- Added optional DC/opponent-result comparison to the shared roll sheet and preserved the existing advantage, hindrance, doubles, and critical follow-up rules.
- Added a data-driven 283-entry skill-effect catalog based on the approved 0.5 review, with automatic bonuses, named situational toggles, advantage/hindrance options, and linked-rule reminders.
- Wired passive development effects into real character math, including Incredible Health, Stalwart, Enduring, Quick Reflexes, Improved Initiative, Runner, Hauler, Self-Taught, and selected unambiguous Chi-school passives.
- Scoped contextual bonuses correctly: Fencer applies to Parry rather than ordinary melee attacks, and Feinter applies only to Feint.
- Expanded Chi from a resource-only feature into a rules domain with 27 purchasable Chi developments, 9 schools, and 68 techniques imported deterministically from Masters of Melee.
- Chi techniques now show requirements, availability, action/cost text, and can spend Chi directly when usable; Internal Chi automatically activates the resource and Chi progression can raise its maximum.
- Added acquired Martial Arts and Chi development as dedicated Character-sheet groups using the same compact interaction pattern as special development.
- Added deterministic 0.5 import tests and pure-model coverage for derived effects, Chi access, roll contexts, target comparison, and skill-effect resolution.
- Kept ambiguous Block calculation and stateful combat effects out of automatic resolution until their rule/state dependencies are explicit.
- Bumped the default Android version to `0.5`.

## 0.4 — Martial arts, Chi, and Archmage expansion

- Added Martial Arts as a dedicated Development tab and XP-only progression domain, separate from ordinary and special development.
- Imported 19 approved unarmed/weapon martial styles and 103 unique techniques from the Master of Melee rulebook, including table-based stances and Gun Kata techniques.
- Added martial-style prerequisite handling for named alternatives and “any martial art” requirements without consuming Ability Points.
- Added persistent Chi as an optional character resource with a base pool of `max(3, Will + 1)` and up to 10 bonus ranks at 50 XP each.
- Added a dedicated Chi tab with current/max controls, bonus-rank purchasing, full restore, and short/long-rest rule reminders.
- Added enabled Chi to the main Character resource strip for fast in-play spending and recovery.
- Added Chi purchases to the shared character XP economy and bumped character persistence to schema 7.
- Imported 54 additional Archmage spells only for already-supported schools: Warding, Divination, Prayer, and Mind.
- Kept Illusion, Elementalistics, Mana Well, Wild Magic, and other experimental Archmage systems out of this release.
- Added deterministic 0.4 content import tooling for repeatable rulebook-to-catalog updates without duplicate entries.
- Bumped the default Android version to `0.4`.

## 0.3.2.1 — Bottom-sheet gesture and toggle visibility hotfix

- Restored normal drag gestures for modal bottom sheets.
- Contained scroll overshoot inside long sheet content so reaching the end of a list no longer hands uncontrolled drag momentum to the parent sheet.
- Added a shared high-contrast switch style with a clearly visible light thumb in the off state and readable disabled states.
- Added release-tag/versionCode support for four-part hotfix versions such as `v0.3.2.1`.
- Bumped the default Android version to `0.3.2.1`.

## 0.3.2 — Compact development sheet and interaction polish

- Grouped acquired development on the Character sheet into ordinary and special sections.
- Ordered acquired development by prerequisites, placing dependent entries under their first owned prerequisite without duplicating them.
- Reduced Character-sheet development rows to name and rank only; tapping opens full details.
- Made the full spell catalog row tappable for learning instead of requiring the small add button.
- Made the full equipment catalog row tappable; tapping an owned catalog item adds one more.
- Added a toggle to hide or reveal unlearned magic schools.
- Disabled bottom-sheet drag gestures so scrolling long sheet content cannot pull or jitter the whole sheet at scroll boundaries.
- Bumped the default Android version to `0.3.2`.

## 0.3.1 — Custom resources and school magic

- Added manual maximum overrides for Health, Endurance, and Mana with reset-to-formula behavior.
- Added persistent custom resources with editable name, current value, and maximum.
- Reworked Magic Power into separate canonical school values at 25 XP per level.
- Added the rulebook school catalog, canonical aliases, consistent school ordering, and school filters for spells.
- Spell usability now checks mana cost against the matching school power; spells can still be learned when currently unusable and are clearly marked.
- Added school Magic Power XP to the shared character XP economy.
- Preserved legacy global Magic Power only as a migration fallback until school power is configured.
- Compressed favorite skill rows on the Character sheet to name, rank, and total bonus.
- Added acquired Development/Feat entries with descriptions below favorite skills on the Character sheet.
- Bumped the default Android version to `0.3.1`.

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
