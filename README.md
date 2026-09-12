# DUBL Android

Standalone native Android companion for DUBL. This repository is intentionally separate from the Windows/Linux desktop app while Android has its own `0.1.x` development line.

## Current version

**0.1.3** — native Kotlin + Jetpack Compose rebuild, API 36.

## The easy workflow

You do **not** need to compile APKs locally for normal testing.

### Every push

GitHub Actions automatically:

1. sets up Java, Android SDK 36 and Gradle;
2. runs unit tests;
3. builds `DUBL-Android-dev.apk`;
4. exposes it under **Actions → Android CI → Artifacts**.

The development package is `com.dubl.character.android.dev`, so it is separate from release installs. A stable public debug key is committed only for this `.dev` package, allowing one CI build to update the previous one.

### Releases

A tag such as `v0.1.3` automatically builds a signed production APK and creates a GitHub Release containing:

```text
DUBL-Android-0.1.3.apk
```

Release signing uses GitHub Actions secrets. The private release key is never committed.

## One-time GitHub setup

From this directory:

```bash
git init -b main
git add .
git commit -m "android: start native 0.1.3"
gh repo create DUBL-Android --public --source=. --remote=origin --push
```

Then configure release signing once:

```bash
./tools/setup-signing
```

Back up `.private/dubl-android-release.jks` and `.private/signing.env` somewhere safe. They are ignored by Git.

Optionally install the small helper command:

```bash
./tools/dubl-android install
```

Then everyday use is just:

```bash
dubl-android push "android: skills screen work"
```

To publish a release:

```bash
dubl-android release 0.1.3
```

## Native stack

- Kotlin
- Jetpack Compose + Material 3
- Android API 36 target
- AGP 9.4
- Gradle 9.6
- Java 17 bytecode
- no WebView
- no requested Android permissions

## Local build (optional)

Local Android Studio builds still work. If the Gradle wrapper JAR has not been generated yet, run:

```bash
./bootstrap-wrapper.sh
```

Then:

```bash
./gradlew assembleDebug
```

But for routine APK testing, GitHub Actions is the intended path.
