# Push notifications: setup

Push is built in but stays off until Firebase is set up. Nothing breaks without it.

## 1. Firebase project (Gerald, once)

1. Sign in to https://console.firebase.google.com as **workspace@disciplehub.org** and click **Create a project**,
   for example "DH Workspace". Google Analytics can be off.
2. **Add app → Android** twice:
   - package `com.workspace.disciplehub` (the Play Store app)
   - package `com.workspace.disciplehub.dev` (the test app)
   You can skip downloading `google-services.json`; the app does not use the file.
3. **Project settings → General** shows, for each Android app, the **App ID** (`1:…:android:…`); at the top the
   **Project ID**, and under **Your apps** the **Web API key**. **Cloud Messaging** shows the **Sender ID**.

## 2. The app (this PC)

Add to `C:\xampp\htdocs\WPAndroid\local.properties` (never committed):

```
fcm.projectId=your-project-id
fcm.senderId=123456789012
fcm.apiKey=AIza...
fcm.appId.dev=1:123456789012:android:aaaa...
fcm.appId.prod=1:123456789012:android:bbbb...
```

Rebuild. After sign-in the app asks for notification permission once and registers the phone (`app_devices`).

## 3. The server (local and live)

**Project settings → Service accounts → Generate new private key** gives a JSON file. Put it, renamed
`service-account.json`, in the private storage folder `wp-fcm`:

- local: `C:\xampp\storage\wp-fcm\service-account.json` (check `dh_private_storage()` for the exact root)
- live: `/var/www/storage/wp-fcm/service-account.json`, readable by www-data, never in the website folder or git

## What sends

- Every bell notice (`dh_notify`, cfg/notify.php) also goes to the person's phones (cfg/app_push.php).
- Live Sessions: a phone reminder about 15 minutes before each session, once per person (the Live Sessions job).
- Global Settings' message **test mode** stops all pushes, like SMS and email.
- Channels on the phone: Live sessions, Learning, Community, Rewards, General. Each can be turned off in Android
  settings → Apps → DH Workspace → Notifications.
