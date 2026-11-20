# App API (v1)

Base: `https://workspace.disciplehub.org/api/v1/` (dev: `http://10.0.2.2/wp/api/v1/`). Code: `wp/api/v1/`.

Every reply is JSON:

- success: `{"ok": true, "data": {...}}`
- failure: `{"ok": false, "error": {"code": "...", "message": "..."}}` with an HTTP status. `message` is ready to show.

Signed-in calls send `Authorization: Bearer <access_token>`. Access tokens last 1 hour, refresh tokens 180 days
and rotate on every refresh (the old pair stops working). Tokens are stored on the server as SHA-256 only.

| Endpoint | Method | Body / result |
|---|---|---|
| `ping.php` | GET | `api_version`, `min_app_version`, `maintenance`, `join_url`, `reset_url` |
| `auth.php` | POST | `action=login`: `username`, `password`, `device_id`, `device_name`, `app_version`, `os_version` → tokens + `me` |
| | | `action=refresh`: `refresh_token` → new tokens |
| | | `action=logout`: `refresh_token` → this device signed out, its push token removed |
| `me.php` | GET | the person (below) |
| `devices.php` | POST | `action=register`: `push_token` (FCM) · `action=unregister` |

`me`: `user_id`, `username`, `name`, `first_name`, `role`, `role_label`, `center {id, name, country}`, `photo_url`,
`language`, `setup_step` (`''` or `password` / `privacy` / `setup` for unfinished self sign-ups), `features`
(feature → true/false from the role matrix: self_study, my_progress, live_sessions, live_sessions_host, library,
fun_quiz, bible, reading_plans, forum, prayer_wall, rhema_ai, rewards, learners_progress, teach, members, centre_reports).

Error codes: `unauthorized`, `token_expired` (401: renew with the refresh token), `refresh_invalid` (401: sign in
again), `invalid_credentials` (401, with `attempts_left`), `too_many_attempts` (429: 5 wrong in 10 minutes per
username and IP), `account_blocked` (403), `maintenance` (503), `missing_fields` / `device_required` (422).

Tables (created on first use, `dh_schema_done('app_api', 1)`): `app_tokens`, `app_devices`, `app_attempts`.
