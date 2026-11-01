# SDD ledger — plan: docs/superpowers/plans/2026-11-01-hometube-standalone.md

Design approved: user “triển khai đi”, 2026-11-01 Asia/Saigon.
Execution: inline; one independent final code review.
Workspace: new standalone project at D:/APPSMARTTUBE, old projects remain read-only inputs.
Pre-flight: Task 2 models/source/store are consumed by TV UI and phone server; signatures fixed in plan. Player uses existing internal H.264 stream models. Phone lifecycle follows application activity count.
Ruling: Use new feature repository in user's empty target folder, not a worktree of the old HomeKara project — independent APK and no current branch to protect — cost: duplicate foundation source retained with provenance.
Task 1: extraction preflight passed 2026-11-01: actual search 19 videos, resolve 20 candidates, H.264 selected (LiveSmokeTest, 0 skipped). No ADB device attached; runtime playback on KitKat unverified.
Task 2 core: missing-symbol RED, then display format regression RED; implemented and verified 64 tests passing including live smoke. Source/store compiled in preflight. Exact evidence docs/preflight.log and test XML.
Ruling: Preserve extractor v0.26.5 — live smoke succeeded, so no speculative dependency upgrade — cost: future YouTube changes still require maintenance.
Task 3: TV home/search/browser/player/settings implemented; API19 Home/search keyboard smoke passed in 80-test run (docs/delivery-checks.log). New manifest exports only stable HomeTube launcher; independent package confirmed in source.
Task 4: paired phone server/UI on 8687 implemented, token/origin/length policy tests passing; foreground lifecycle explicit. Actual LAN browser interoperability still needs box test.
Final review: independent read-only reviewer reported four Important defects. Fixed 403 readiness with LoadReadiness, media ownership with stop-before-open and guard, bounded intent queue with QueueWindow, and playlist continuation with PlaylistQueue + cursor/recovery. Regression core tests added; full suite passed before native render test addition.
Ruling: Missing/empty kiosks use explicitly labelled search recommendations for the same topic — improves first-run usefulness without claiming results are regional rankings — cost: recommendations differ from a YouTube ranking.
Ruling: Keep targetSdk 28 for the approved sideloaded KitKat app and disable only Play publishing target expiry lint — not a Google Play release — cost: Play publishing would require a separate target/API migration. API compatibility lint remains enabled and aborts on real errors.
Ruling: Conscrypt Android remains in APK, Conscrypt OpenJDK is used only in JVM test runtime to avoid loading Android .so files on Windows — cost: host tests do not prove the box TLS installation.
Polish: transparent focus ring drawn over cards, primary-button focus contrast, QR shortcut scroll, and reapply safe inset on resume.
Final review unverified boundaries: actual KitKat decode/TLS, HomeBox physical tile/remote interaction, real-TV clipping, LAN phone behavior, cross-app decoder contention. These are listed as hardware checks in delivery report, not considered passed by host tests.
Native UI QA: RenderPreviewTest initially failed on actual card recycling because Thumbs.load replaces ImageView.tag. Fixed Cards with a stable ViewHolder and added recycling assertion in API19 UI smoke; full 81-test render-enabled run passed in docs/render-verified.log. Native screenshots visually inspected; a narrow HomeKara button was adjusted, then final verification requested.
Polish follow-up: stale phone-search errors ignored; no deliberately deferred minor findings. Runtime/UI/device limits remain explicitly unverified in TEST-REPORT.md.
Task 5: final-verification.log BUILD SUCCESSFUL; native screenshots inspected; signature v1/v2 and manifest verified. Packaging via tools/package.py, no signing files in source. Hardware checklist remains unverified explicitly, as approved.
