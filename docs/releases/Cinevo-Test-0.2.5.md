# Cinevo Test 0.2.5 — Cinevo splash and Mattflix theme

Approved for committing and a signed test build. Physical device testing remains pending.

- Replace Lumera's bundled splash video with an animated Cinevo wordmark: cyan, violet and pink over a dark violet glow, diagonal light strokes and “Your cinema. Your way.”
- Render natively at the device resolution, with no media decoder, audio, network request or bitmap download.
- Brief 1.6-second intro, fade out once app composition is ready, with a six-second maximum loading hold plus 280ms fade. Preserve the profile splash switch and recreation handling.
- Remove the obsolete Lumera splash MP4 from Cinevo's resources.
- Add the selectable built-in **Mattflix** preset: red #E50914, background #141414, surfaces #181818, white text and muted grey. Use white content on the preset's primary red surfaces.
- Preserve existing theme selections and resolve Mattflix through the existing per-profile theme system. This is a colour/contrast theme; screen structure and navigation are retained.

Proposed identity: 0.2.5-cinevo-test, version code 16. Debug package and persistent signing configuration unchanged.

Validation: source whitespace/reference checks passed. Two Robolectric splash lifecycle regressions prepared (normal intro completion and startup timeout, exactly-once callback). Full Android compilation and test execution remain pending CI; no physical phone/Shield test is claimed.

Device checks: splash enabled/disabled, cold launch, app resume/recreation, portrait/landscape framing, slow startup, theme picker visibility, per-profile Mattflix selection and persistence after restart. Playback and genre-row behaviour should receive a brief smoke test.
