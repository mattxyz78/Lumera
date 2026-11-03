# Cinevo Test 0.2.3 — phone compatibility draft

Status: approved for committing and a signed GitHub test build. Physical phone and Shield testing remain pending.

## Purpose

0.2.2 was installable on ARM Android phones, but its television controls did not provide usable touch navigation. This patch adds a handheld presentation of the existing app, retaining its repositories, profile isolation, playback backend and data formats.

## Changes

- Add pointer clicks to shared TV Material surfaces on handheld devices. Television surfaces retain their original D-pad behaviour.
- Detect television mode from Android configuration, independently of display width. Do not change saved navigation preferences.
- Provide a compact navigation strip, profile selection shortcut and settings shortcut on handhelds.
- Render existing home rows, category hubs and Continue Watching without a television hero or focus-driven scrolling.
- Provide touch watchlist rows and a system-keyboard search field with film and series results.
- Replace the settings sidebar with horizontally scrollable categories and a full-width content pane on handhelds.
- Allow horizontal scrolling of profile avatars and expose the edit action without focus.
- Fit profile dialogs and detail sidebars to narrow displays. Make detail actions readable and horizontally scrollable.
- Use an adaptive phone content grid instead of six fixed columns.
- Add tap-to-show video controls, touch playback buttons and a draggable seek slider.
- Skip television-only focus restoration where handheld layouts replace the target controls.

## Release identity

- Proposed version: 0.2.3-cinevo-test (version code 14).
- Package remains com.cinevo.tv.test for the debug build.
- Persistent signing configuration and expected certificate are unchanged.
- Existing build workflow still produces one verified APK, now named Cinevo-Test-v0.2.3.apk.
- Main branch, Lumera installation and existing on-device data are untouched.

## Validation

- Complete Android app compilation passed in a clean local checkout.
- All 18 Android regression tests passed, including two physical pointer-event tests proving enabled handheld cards respond exactly once and disabled cards reject taps.
- All five APK certificate-parser tests passed (23 automated tests total).
- Source whitespace validation passed.
- No APK was built or signed for delivery; the approved GitHub workflow is still required.
- Device acceptance testing remains required; compilation cannot establish phone usability or Shield compatibility.

## Phone acceptance checklist

1. Install as an update over the phone's Cinevo Test 0.2.2, preserving profiles.
2. Select and edit profiles; check all three new-profile setup choices.
3. Tap every navigation category, including repeated taps on the current category.
4. Scroll home rows and tap posters and hubs; open See all and use Android Back.
5. Search using the normal phone keyboard, open a result and return.
6. Open detail actions and sources; play a legitimate configured stream.
7. Tap the video to reveal controls, pause/resume, drag the seek slider, and select audio/subtitles.
8. Test portrait, landscape, screen rotation, the keyboard and system navigation insets.
9. Add/remove watchlist items, switch profiles and restart; verify separation remains intact.
10. Review each settings category and its dialogs for clipping and usable touch targets.

## Scope and follow-up

This is an initial phone compatibility patch, not a claim of a finished mobile product. The phone search initially exposes text search; TV Discover filters remain on the television layout. Advanced integration, theme/dashboard editors, audio/subtitle panels and media-specific playback still require physical phone testing. There is no emulator or connected phone in this session. Shield remote navigation also needs a smoke test before calling this a dual-device release.
