# DH Workspace for Android

The Android app of DH Workspace (Disciple Hub), for learners, group leaders and facilitators first.
Package `com.workspace.disciplehub`. Kotlin + Jetpack Compose, min Android 7 (API 24), target 36, compiled against 37.

## How it fits together

```
Android app  --HTTPS + token-->  wp/api/v1/*.php  -->  the same MySQL as the website
```

The app never connects to MySQL and never holds a database password. The API lives in the web project
(`C:\xampp\htdocs\wp\api\v1`, repo spearpower/DHWorkspace) and runs every call as the signed-in person with their
role's rights from the Roles & access matrix. See [docs/API.md](docs/API.md).

## Build variants

| Variant | Server | App name | Package |
|---|---|---|---|
| `devDebug` | this PC's XAMPP: `http://10.0.2.2/wp/api/v1/` (emulator) | DH Workspace (dev) | `com.workspace.disciplehub.dev` |
| `prodDebug` / `prodRelease` | `https://workspace.disciplehub.org/api/v1/` | DH Workspace | `com.workspace.disciplehub` |

A real phone on the same Wi-Fi reaches the PC with a line in `local.properties` (not committed):
`dev.apiBase=http://192.168.x.x/wp/api/v1/`. Only the dev variant may use plain HTTP.

## Build

Android Studio, or from the command line with Android Studio's JDK:

```
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew assembleDevDebug        (app/build/outputs/apk/dev/debug)
gradlew bundleProdRelease       (Play Store bundle; needs the upload key, never committed)
```

## Code map

- `WpApp` creates the token store, API client and sign-in repository.
- `data/` – `ApiClient` (envelope, automatic token renewal), `AuthRepository`, `TokenStore` (Keystore-encrypted), `Models`.
- `ui/` – `AppRoot` (sign-in or app), `login/`, `main/` (bottom tabs by role), `home/` (dashboard cards), `me/`.

## Phases

0. Foundations: API sign-in, app shell, theme, variants (done 2026-11-20).
1. Learner MVP: dashboard data, Self Study, Live Sessions, Library, Fun Quiz, sign-up, push notifications (FCM).
2. Group leaders and facilitators: my group, learners' progress, QR attendance, reports.
3. Community and offline: Rhema AI, Forum, Prayer wall, Bible, reading plans, offline mode, app languages.
4. Google Play release.
