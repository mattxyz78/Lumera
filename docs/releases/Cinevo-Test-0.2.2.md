# Cinevo Test 0.2.2 — proposed stability update

Status: local, uncommitted review draft. No release, remote build or installation authorised by this file.

Base: development/cinevo-branding at 5b3c349efdda359c3ecde845b2c9652a41e1b0d1.
Proposed identity: com.cinevo.tv.test, version 0.2.2-cinevo-test, version code 13.
Keep the existing persistent signing secrets and certificate.

## Changes

- Disable in-app update checks/downloads/installation until a Cinevo channel is approved. Do not offer upstream Lumera APKs. Workflow APK delivery remains the test-update route.
- Serialise profile transitions, snapshot saves and profile-bound viewing mutations. Cancel and join old periodic sync/connection collectors before switching.
- Give playback operations an activation token. Reject queued saves from an old profile, including an old activation of the same profile.
- Guard watchlist/history/Next Up edits and asynchronous poster enrichment against profile switches.
- Write profile snapshots atomically. Normalise missing arrays in older snapshots; preserve unreadable files and report a profile-open error.
- Record an interrupted-switch marker before replacing runtime tables; replay the incoming snapshot on restart before attributing live rows to a profile.
- Never inherit personal viewing tables or hub rows when a profile snapshot is missing.
- Keep audio, subtitle, subtitle delay and remembered source choices under profile-specific keys.
- Clear profile-owned Trakt credentials and playback preferences when an inactive profile is deliberately deleted.
- Treat unknown duration as unknown, rather than completed. Apply the profile's watched threshold to local progress and the returned playback-session result.
- Add regression tests and require the workflow to verify exactly one APK, package/version and the 0.2.1 signing-certificate fingerprint before uploading.

## Data and compatibility

No package or signing identity change. No database filename or Room version change. No uninstall, data clear or automatic removal of an existing Lumera application.

Legacy audio/source preferences lack an owner. They are retained, but no longer applied to every profile. Users may need to choose their preferred tracks/source once per profile.

Local profile completion preferences do not change the external Trakt service's own scrobbling rules. No new streaming services or content sources are introduced.

The APK certificate fingerprint recorded from 0.2.1 is:
c9334a9e5eb9d2254dafa2662b3062ff82396672bd1369f91b7486b87b63ed3c.

## Verification

Local validation completed: app Kotlin/Java compilation passed and all 16 regression tests passed (0 failures/errors). The local runner needed environment-only proxy and Java-agent configuration; those local paths are not part of the repository patch. Workflow YAML and the unchanged persistent-signing step were checked. The artifact gate rejected the actual older 0.2.1 APK and duplicate APK output. Android apksigner verified the baseline 0.2.1 APK and its certificate fingerprint.

No signed 0.2.2 APK exists yet. The changed GitHub workflow has not run. Actual Shield tests and installed-certificate comparison remain pending.

Regression scope: same-content profile preference isolation and persistence, legacy preference ownership, unknown duration and threshold boundaries, late queued writes, rapid/reactivated sessions, snapshot recovery, missing/corrupt snapshots, A→B→A watchlist/history/Next Up restoration, interrupted switch replay, inactive deletion constraints, and disabled updater network/install entry points.

## Review and release steps

1. Review the local diff and final validation result.
2. Obtain approval to commit this patch to the existing development branch and run its signed workflow.
3. Verify GitHub test/build results and inspect the actual resulting APK identity/certificate/version.
4. Deliver one Cinevo-Test-v0.2.2.apk with its checksum.
5. Test an in-place update on Shield, retaining all profiles and settings.
6. Confirm profile switching, resume/completion/Next Up, subtitles, audio and failure recovery using legitimate test media.

## Remaining work

This is the first stability patch, not full release readiness. Full Room migration tests, external-account login races, every add-on/settings background operation, device performance measurements, all playback format combinations and physical remote-control navigation still need coverage. Do not re-enable in-app updates without package, version, certificate and channel validation.
