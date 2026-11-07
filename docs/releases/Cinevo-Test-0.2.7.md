# Cinevo Test 0.2.7 — profile ages and optional PINs

Approved by Matthew for commit and a signed GitHub test APK build.

Each profile keeps its age group and gains a separate **PIN to open profile: ON/OFF** choice. Child age filtering works with the PIN switched off. Adult profiles open directly unless explicitly locked. This uses the existing shared parent PIN for whichever profiles are locked.

- Keep the age choices Under 12, 12–14, 15–17 and Adult, with the existing UK classification limits and blocking of unverified under-18 content.
- Add an explicit Continue button to the age/PIN step so both choices can be reviewed before advancing. Complete the avatar/theme steps to save the profile.
- Default the optional PIN to off for new and existing profiles. Preserve each existing profile's age group, name, layout and saved data. Migration 45 to 46 only adds `pinProtected INTEGER NOT NULL DEFAULT 0`.
- Remove automatic PIN prompts when opening adult profiles, creating profiles or editing profiles while no locks are in use.
- When any profile has a PIN lock, require the parent PIN for creating, editing or deleting profiles. This prevents removing a lock or changing an age limit through an unlocked profile.
- Verify or set the parent PIN before saving the first enabled lock. Cancelling verification leaves the profile unchanged. Turning the final lock off requires the existing PIN; subsequent profile access and management need no PIN.
- Keep the existing parent PIN stored when all locks are off, for reuse if a lock is enabled later.
- Allow optional UK ratings key setup within the under-18 age step without enabling a PIN. When a lock is in use, the same profile-edit authorization protects key changes. A missing/unavailable rating still blocks restricted content.

## Validation

Eight Python checks pass, including SQLite preservation of child ages and saved layouts with optional locks off. Ten new Android regressions cover unlocked child/adult login, correct/cancelled locked login, optional profile management, saving a child age without a PIN, first-lock authorization/cancellation, last-lock removal and ratings setup without a PIN. Android debug source/test compilation and all 67 Android unit tests pass locally, including the ten new profile regressions and a new ratings-storage test confirming that no PIN is created. Source whitespace checks pass. No installable APK or remote build has been produced for this patch. Physical Shield/phone verification remains pending.

Proposed identity: 0.2.7-cinevo-test, code 18, debug package com.cinevo.tv.test, existing Cinevo signing certificate. No remote commit, signed build, main merge or installation has taken place for this patch.

Device checks: existing child age preserved; direct adult/child opening with PIN off; age/PIN selection by Shield remote and phone touch; complete wizard/save; enable a lock and reject incorrect/cancelled PINs; disable the last lock with the correct PIN; relaunch persistence; under-18 content filtering with PIN off; existing home refresh, seeking, genre rows, Cinevo splash and Mattflix theme smoke tests.
