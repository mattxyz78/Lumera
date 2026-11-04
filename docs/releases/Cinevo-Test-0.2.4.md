# Cinevo Test 0.2.4 — Shield seeking and custom genre rows (draft)

Status: approved for committing and a signed test build. Full Android and physical Shield validation remain pending.

## Report

Seeking to a chosen point returns to the beginning with ERROR_CODE_IO_BAD_HTTP_STATUS. Device, source and HTTP response number are not yet confirmed.

## Confirmed code defects

The backend explicitly restarts the same source at zero after a direct HTTP 416 response. Absolute seeks have no duration bound; relative seeks allow exactly duration, which can lead to an end-of-file range request. The background lifecycle save can persist a failed seek's position. Generic IO recovery retries permanent HTTP failures and only examines the unwrapped error name.

## Patch

- Remove the HTTP 416 restart-at-zero fallback.
- Clamp absolute, relative and deferred resume seeks inside a known duration, retaining the requested timestamp when duration is unknown.
- Handle overflow/negative timestamps safely.
- Read HTTP response status through nested exception causes and show a short source error without exposing URLs or credentials.
- Retain one bounded automatic IO retry for transient HTTP 408, 429 and 5xx failures; stop retrying rejected ranges and access failures.
- Preserve the pending timestamp during reconnect preparation and prevent an older delayed retry overriding a newer seek.
- Skip background progress persistence when playback has errored or is not ready.

## Validation

- Eight targeted Kotlin/JUnit seek-policy tests compiled and passed.
- Five existing APK certificate-parser tests passed.
- Whitespace checks passed.
- Two Android backend regressions added for direct and wrapped HTTP 416 responses, asserting no restart, prepare or seek to zero and retention of the requested position. These Android tests have not yet run.
- Full Android compilation and the existing regression suite remain pending the approved GitHub workflow; the fresh local environment lacks the previous Android SDK/dependency cache.
- No device testing or successful playback against the affected source is claimed.

## Release identity

Proposed version 0.2.4-cinevo-test, code 15. Debug package com.cinevo.tv.test and the persistent signing configuration are unchanged. No commit, upload build, main-branch change or installation has taken place for this patch.

## Acceptance

Recheck the original failing video/source on the affected device: forward and backward seeks in the middle, a near-end seek, pause/resume, leaving after a failed seek and reopening Continue Watching. Repeat on a second legitimate source and both phone and Shield when available. A provider can still reject range requests or expired access; this patch does not claim to make such a source seekable. The displayed response code will distinguish these cases.

## Custom home-screen genre rows

User request: choose arbitrary genre rows for the home screen, rather than only the preconfigured popular film/TV catalogues.

The existing Home Screen Editor gains **Add Row → Add genre row**. Choose an enabled source/catalogue and its film/TV type, then an advertised genre. If a source declares genre support without a fixed option list, a supported genre name can be entered. The app does not invent a catalogue for sources that offer no genre filter.

Each genre is stored as a separate catalogue configuration, so its title, order, visibility and Home/Movies/Series placement use the existing editor controls. Identical genre/source/type choices reuse the same configuration instead of generating duplicate rows. Genre values travel with the profile snapshot. The new write is guarded by the active profile-session token to prevent a late write reaching another profile.

Dashboard loads and category previews include the genre filter. Pagination combines genre and skip within the same Stremio extras path component, preserving the genre on later pages. Existing unfiltered rows retain their original URLs.

Database upgrade 43 → 44 adds one nullable genre column without dropping tables or replacing rows. Existing snapshots without a genre remain valid.

Validation: seven Kotlin/JUnit catalogue URL tests passed, including encoded genres, filtered pagination, replacement of an existing skip and unchanged legacy URLs. Six Python checks passed, comprising the five certificate-parser tests and a SQLite upgrade/data-retention test. Together with the eight earlier seek-policy tests this is 21 passed targeted checks. Full Android compilation, backend regressions, Room schema validation and physical Shield testing remain pending the approved signed build.

Additional acceptance: create Action films and Crime TV rows, reorder/rename/hide them, check later pages stay in the chosen genre, restart and switch profiles, verify existing rows/watchlists/history, and confirm a 0.2.3 update retains stored data. Test remote focus in both catalogue and genre pickers and the free-entry keyboard for unrestricted sources.
