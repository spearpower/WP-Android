# Releasing to Google Play

## Before the first release

1. **Play Console account** recovered (Gerald). Package: `com.workspace.disciplehub` (fixed forever after upload).
2. **Upload key** (once, keep 2 copies offline, never in git):
   `keytool -genkeypair -v -keystore dh-upload.jks -alias dhupload -keyalg RSA -keysize 2048 -validity 10000`
   then `keystore.properties` in the project root:
   ```
   storeFile=dh-upload.jks
   storePassword=...
   keyAlias=dhupload
   keyPassword=...
   ```
   Use **Play App Signing** (Google keeps the app key; you keep the upload key).
3. **Website pages live** (upload the web files): `privacy.php` (privacy policy URL) and `delete-account.php`
   (account deletion URL). In the app: Me > Privacy policy, Me > Delete my account.
4. **Push** (optional for the first release): docs/PUSH.md.

## Each release

1. Raise `versionCode` (+1) and `versionName` in app/build.gradle.kts. If the server needs the new version, raise
   `API_MIN_APP_VERSION` in wp/api/v1/api.php after the release is out.
2. Upload the web API first (wp/api/v1 and any changed helpers), then build:
   `gradlew bundleProdRelease` → `app/build/outputs/bundle/prodRelease/app-prod-release.aab`
3. Play Console → Internal testing → upload the .aab → test on real phones → promote to Closed/Production.

## Store listing checklist

- App name "DH Workspace", short and full description, icon 512×512, feature graphic 1024×500,
  at least 2 phone screenshots (Home, Learn, Live, Library).
- Content rating questionnaire; Target audience (13+; not for children); Ads: none.
- **Data safety**: collected = name, email, phone, user ID, app activity (learning progress), photos (profile, optional);
  purpose = app functionality, account management; encrypted in transit = yes; deletion = yes (link above);
  shared with third parties = no (Google Firebase only delivers notifications; YouTube plays course videos).
- Permissions used: Internet, notifications (Android 13+). Camera is not requested (Google's code scanner).
