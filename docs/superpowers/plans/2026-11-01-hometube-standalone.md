# HomeTube standalone Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans; implement inline, then one fresh whole-project review. Track steps below and progress in docs/PROGRESS.md.

**Goal:** Deliver a signed standalone HomeTube APK with Vietnamese TV UI, Vân Trường attribution and a launcher target bindable in HomeBox Lite.

**Architecture:** Reuse the existing HomeKara TLS/extraction/H.264 playback foundation, packaged as vn.hometube.app. Pure Kotlin core, thin framework Views activities, independent SQLite data, paired phone remote on 8687. No direct sharing of private HomeKara state.

**Tech Stack:** Kotlin 2.0.21, AGP 8.5.2, compile 34/min 19/target 28, Media3 1.4.1, NewPipeExtractor baseline v0.26.5, Conscrypt, NanoHTTPD, ZXing, SQLite, JUnit.

**Spec:** docs/superpowers/specs/2026-11-01-hometube-standalone-design.md (approved by user: “triển khai đi”).

## Global Constraints

- Separate applicationId vn.hometube.app; stable exported launcher vn.hometube.app.ui.HomeActivity. No HOME category. Both LAUNCHER and LEANBACK_LAUNCHER.
- minSdk 19, targetSdk 28, compileSdk 34, v1 signing; only H.264 and AAC playback, no livestream.
- Default quality 720p; 360/480/720/1080 choices. All network/database work off UI thread.
- Copyright © 2026 Vân Trường; retain licenses/attribution for reused code and dependencies.
- Phone port 8687, token pairing, request-size limits, same-origin writes; available while HomeTube is foreground.
- No push/publish or modification of old projects/launcher; standalone workspace D:/APPSMARTTUBE.

## Review Focus

1. Shared text and lookalike hosts: accept valid YouTube video/playlist/channel URLs and reject unrelated hosts; tests in core.
2. Near-end, short and unknown-duration resume; tests in core.
3. Missing kiosks, failed subscriptions and stale background results; fallback feeds and explicit status, test feed isolation.
4. SponsorBlock errors and deliberate user seeks into skipped segments must not interrupt playback; parser/skipper tests.
5. Separate-app audio focus, lifecycle, phone server foreground state and launcher resolution; manifest verification plus hardware checklist.

### Task 1: Foundation and compatibility preflight

Files: build.gradle.kts, app/build.gradle.kts, tools/build.ps1, baseline tests, docs/PROGRESS.md.

Interfaces: existing vn.homekara.box net/player/domain foundation preserved as internal implementation; new vn.hometube.app package owns app/UI.

- [x] Copy known foundation into empty workspace, initialize feature branch, run baseline testDebugUnitTest.
- [x] Run network extraction smoke against actual public video/search before full UI. Record network restriction separately from extractor errors.
- [x] If pinned extractor demonstrably fails, investigate primary upstream evidence and implement compatible fallback/update, recording change.

### Task 2: Core behavior and persistence

Files: app/src/main/java/vn/hometube/app/core/{Models,Links,Policies,Sponsors,Feeds}.kt; data/{TubeStore,TubeSource}.kt; tests in corresponding core package.

Interfaces: Video(id,title,channel,channelUrl,duration,thumb,views,uploaded), Entry.VideoItem/ChannelItem/PlaylistItem; Page(items,next); Source.search/shelf/detail/channel/playlist/more/suggest/resolve; Store.history/continueWatching/watchLater/subscriptions/savePosition.

- [x] Write and run failing JVM tests for valid/invalid links, resume, SponsorBlock, feed isolation and format.
- [x] Implement and run full unit suite green.
- [x] Implement Android SQLite store (own hometube.db), bounded cached source; no main-thread DB.

### Task 3: TV interface and playback

Files: vn/hometube/app/{TubeApp,ui/HomeActivity,ui/BrowserActivity,ui/PlayerActivity,ui/SettingsActivity,ui/Views}.kt; AndroidManifest.xml; res/drawable resources.

Interfaces: App.source/store/io/main; Nav.play(Video,startSec)/openPage; PlayerActivity owns release/audio focus; every action focusable.

- [x] Implement sidebar, four-column cards, search keyboard/suggestions/filters, channel/playlist pages, local library and subscription recommendations, empty/error/retry states.
- [x] Implement playback/resume/quality/HUD/related/autoplay/SponsorBlock/watch-later/subscriptions. Release video decoder on leaving player.
- [x] Add settings/diagnostics/phone QR, safe inset, copyright and original launcher icon.
- [x] Build debug; inspect manifest and API compatibility.

### Task 4: Phone remote and standalone integration

Files: vn/hometube/app/remote/PhoneServer.kt, assets/remote/index.html, docs/INSTALL.md.

Interfaces: paired GET search/state, POST play/control; foreground lifecycle; localhost intent target stable; HomeKara opens by package only.

- [x] Implement 8687 pairing, search/link play, progress controls and errors, responsive Vietnamese UI.
- [x] Verify actual HTTP auth/size/origin controls through JVM tests where possible and document foreground availability.
- [x] Document selecting HomeTube for HomeBox YouTube tile; do not claim physical-box validation.

### Task 5: Verification and delivery

Files: outputs/HomeTube.apk, outputs/SHA256SUMS.txt, docs/TEST-REPORT.md, README.md, source archive.

- [x] Run unit tests, release build, lint/API check, apksigner verify min-sdk 19, aapt launcher/package check.
- [x] Fresh reviewer checks whole project against spec and Review Focus; fix important defects, rerun checks.
- [x] Render/inspect UI using emulator or preview if available; mark absent hardware checks explicitly.
- [x] Package signed APK and source with install/shortcut instructions, checksums and reproducible build.
