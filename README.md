# NotiOff

A simple tool to get rid of notifications 🔔

[![GitHub Actions status](https://github.com/Code0987/Notioff/workflows/android%20ci/badge.svg)](https://github.com/Code0987/NotiOff/actions)

## Status

**2.0 rewrite in progress** — modern Kotlin / Jetpack Compose stack at the **repo root**.

The legacy Java/Realm project under `src/` is kept as an **inert reference** (behavior oracle) until cutover. **Do not** wire it into the new Gradle build.

Design notes: see [`DESIGN-rebuild.md`](./DESIGN-rebuild.md).

### Screenshots (legacy 1.x UI)

<img src="timeline/screenshots/1b.png?raw=true" alt="1b" title="1b" width=256>
<img src="timeline/screenshots/2b.png?raw=true" alt="2b" title="2b" width=256>
<img src="timeline/screenshots/3a.png?raw=true" alt="3a" title="3a" width=256>
<img src="timeline/screenshots/4a.png?raw=true" alt="4a" title="4a" width=256>

## Build (modern app — only supported path)

Requirements:

- **JDK 17**
- Android SDK with **compileSdk 35** (Android Studio Ladybug+ or command-line tools)

```bash
# From the repository root (not ./src)
./gradlew :app:assembleDebug
./gradlew check
```

Open the **repository root** in Android Studio (not the legacy `src/` folder).

| Property | Value |
|----------|--------|
| applicationId / namespace | `com.ilusons.notioff` |
| minSdk | 26 |
| targetSdk / compileSdk | 35 |
| versionName / versionCode | `2.0.0` / `100000009` (see root `gradle.properties`) |

## Project layout

```text
/                 modern Gradle root (build from here)
  app/            application module (Kotlin + Compose)
  gradle/         wrapper
  src/            LEGACY inert reference — not in settings.gradle.kts
  DESIGN-rebuild.md
```

## Legacy tree

`src/` is the 1.x Java/Realm app (AGP 3.4, SDK 28). It may not resolve dependencies on modern machines (`jcenter` is gone). Use it only for reading old behavior while implementing 2.x.
