# Cinevo Test 0.2.6 — refresh and profile age limits

Approved by Matthew for commit and a signed GitHub test APK build. No device installation or data reset is part of this patch.

## Behaviour

- Reload Home, Movies and Series when the activity returns after being stopped. A cold launch uses the normal fresh load.
- Require an age-group choice when creating a profile; allow an adult to change it by editing an existing profile.
- Ask an adult to set a private 4–8 digit parent PIN. Require that PIN to create, edit or delete profiles and to open an adult profile. Under-18 profile entry does not require the PIN once configured.
- Apply UK rating checks to catalogue/search results, watchlists, continue watching, next-up entries, recommendation/collection/person-credit lists, grids, detail access, stream retrieval and playback. Restrict younger profiles to the internal player.
- Keep Home, Search, Watchlist and Details caches separated by profile/policy or active session; recheck permissions after asynchronous rating responses and before playback.
- Show an explanatory empty state when a restricted profile has no approved titles.

| Profile age | Permitted UK classifications |
| --- | --- |
| Under 12 | U, PG |
| 12–14 | U, PG, 12, 12A |
| 15–17 | U, PG, 12, 12A, 15 |
| 18+ Adult | Unrestricted, including unrated/custom content |

For restricted profiles, missing/unknown UK ratings, ambiguous content IDs and rating-service failures block access. US classifications and review scores never substitute for UK certifications. Where multiple UK certifications exist, all must fit the age limit.

## Setup and limitations

Existing profiles migrate to Adult because their owners' ages are unknown. An adult must edit each child's profile and select its age group before relying on restrictions. Profile names, themes, layouts and saved data are preserved by the additive database migration from 44 to 45.

Rating lookup uses TMDB's GB movie release certifications and TV content ratings, resolving IMDb IDs where necessary. The current workflow does not supply a TMDB key. An adult can enter or replace their own key in the masked optional field on the parent PIN dialog. Without a working key, under-18 profiles hide/block all titles whose ratings cannot be verified. Never send a key or PIN in chat.

The PIN is stored as a salted PBKDF2 hash in private app preferences, with a temporary block after five incorrect attempts. There is no silent PIN reset or recovery bypass. Ratings rely on third-party metadata and correct catalogue identifiers; this is not a device-wide parental control or a guarantee about a provider's actual video contents.

## Validation

Android debug source compilation and all 56 unit tests pass locally, including 19 new age-policy, rating-access and PIN checks. Seven Python checks pass, including real SQLite migration/data-retention checks for profile ages and existing genre-row layouts. Source whitespace checks pass. Physical Shield and phone checks remain pending. No installable APK or remote workflow has been produced for this patch.

Device checks: return from Android Home/recent apps; create/edit all age groups; restart persistence; wrong/cancelled PIN; adult-profile switching; browsing/search/watchlist/history restrictions; direct and resumed playback; offline/invalid-key behaviour; phone touch and Shield D-pad entry; existing seeking, genre rows, Cinevo splash and Mattflix theme smoke tests.

Proposed identity: 0.2.6-cinevo-test, code 17, package com.cinevo.tv.test. Preserve the persistent Cinevo signing certificate. Commit only to development/cinevo-branding; no main merge or installation is requested.
