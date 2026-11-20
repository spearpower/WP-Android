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
| `home.php` | GET | Home dashboard: `learning` (continue, stats), `sessions` (next 3), `points` |
| `study.php` | GET/POST | Self Study: `bootstrap`, `catalogue`, `course`, `progress`, `start_attempt`, `submit_attempt` |
| `live.php` | GET | Live Sessions for 5 weeks (one row per session) |
| `library.php` | GET/POST | `home`, `book`, `progress` (page, pages, seen[]), `bookmark_add`, `bookmark_delete` |
| `files.php` | GET | `kind=book|cover&b=n`, `kind=certificate&code=` (PDF/JPEG with the token) |
| `join.php` | POST (no sign-in) | self sign-up: `check_username`, `submit` |
| `welcome.php` | GET/POST | first sign-in setup: `password`, `privacy`, `setup` |
| `lead.php` | GET/POST | `learners`, `members`, `gatherings`, `roster`, `mark`, `qr`, `checkin` |
| `web_code.php` | POST | one-time link that opens a website section signed in (`to`) → `web_login.php` |
| `account.php` | POST | `action=delete_request` (to the System Admins; nothing deleted automatically) |

`me`: `user_id`, `username`, `name`, `first_name`, `role`, `role_label`, `center {id, name, country}`, `photo_url`,
`language`, `setup_step` (`''` or `password` / `privacy` / `setup` for unfinished self sign-ups), `features`
(feature → true/false from the role matrix: self_study, my_progress, live_sessions, live_sessions_host, library,
fun_quiz, bible, reading_plans, forum, prayer_wall, rhema_ai, rewards, learners_progress, teach, members, centre_reports).

Error codes: `unauthorized`, `token_expired` (401: renew with the refresh token), `refresh_invalid` (401: sign in
again), `invalid_credentials` (401, with `attempts_left`), `too_many_attempts` (429: 5 wrong in 10 minutes per
username and IP), `account_blocked` (403), `maintenance` (503), `missing_fields` / `device_required` (422).

Tables (created on first use): `app_tokens`, `app_devices`, `app_attempts` (`dh_schema_done('app_api', 1)`),
`app_web_codes`, `app_push_sent`, `account_deletion_requests`.

Web files outside api/v1 the app depends on: `cfg/app_push.php` (+ hooks in `cfg/notify.php`,
`php/liveclasses/lc_helpers.php`), `cfg/account_deletion.php`, `privacy.php`, `delete-account.php`.
