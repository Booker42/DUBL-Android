# DUBL Android

Standalone native Android companion for DUBL. Android remains a separate repository and release line from the Windows/Linux desktop app.

## Current version

**0.2** — integrated character sheet, skills, development, magic, and equipment on Kotlin + Jetpack Compose, API 36.

## What 0.2 includes

- Character dashboard with resources, conditions, derived-stat breakdowns, favorite skill checks, and quick rolls.
- Full native skills workflow with rank XP costs, custom/specialized skills, hiding/restoring, favorites, and multi-attribute choices.
- Native development catalog with skills/abilities, prerequisites, branch access, ranks, ability-point budgeting, and requirement validation.
- Native magic with mana progression, power, schools, spellbook, learning XP, custom spells, and the desktop spell catalog.
- Native equipment with the desktop catalog, quantities, carried state, custom items, load, capacity, and burden penalties wired into the character sheet.
- Shared local persistence for all of those systems with backward-compatible loading of older Android snapshots.
- Stable roll engine for normal rolls, multiple advantages/hindrances, situational modifiers, doubles, critical failure confirmation, and superiority dice.
- Mobile UI polish for long names, larger font scales, empty states, destructive-action confirmation, and the compact six-section bottom navigation.

## Everyday Git workflow

No project helper command is required.

After ChatGPT changes the project and you have checked the build on your device:

```bash
git status
git add -A
git commit -m "android: describe the update"
git push
```

GitHub Actions automatically tests the project and builds `DUBL-Android-dev.apk` for every push to `main`.

The development package is `com.dubl.character.android.dev`, so it installs next to the production app and uses the stable repository debug key.

## Release

For the 0.2 release:

```bash
git status
git tag -a v0.2 -m "DUBL Android 0.2"
git push origin v0.2
```

The tag triggers the signed production release workflow and creates:

```text
DUBL-Android-0.2.apk
```

Release signing uses GitHub Actions secrets. The private production key is never committed.

## One-time repository setup

If this is a fresh clone/repository:

```bash
git init -b main
git add .
git commit -m "android: start native app"
gh repo create DUBL-Android --public --source=. --remote=origin --push
```

Release signing can be configured once with `tools/setup-signing`. That script only provisions the release key/secrets; it is not part of the day-to-day Git workflow.

## Native stack

- Kotlin
- Jetpack Compose + Material 3
- Android API 36 target
- AGP 9.4
- Gradle 9.6
- Java 17 bytecode
- no WebView
- no requested Android permissions

## Local build

If the Gradle wrapper JAR has not been generated yet, run:

```bash
./bootstrap-wrapper.sh
```

Then:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

For routine testing, the GitHub Actions development APK is also available under **Actions → Android CI → Artifacts**.
