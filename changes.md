# Changes

## 2026-09-28

- Added `CLAUDE.md` with workflow rules: use plan mode, get a Fable 5.1 second opinion on plans, log changes in `changes.md`, and report files and commit messages after each change.
- Added `changes.md` to track changes.
- Removed Firebase: google-services and Crashlytics Gradle plugins, `firebase` catalog entries and bundle, `app/google-services.json`, and `temp/legacy-code/.../utils/FirebaseExt.kt`. `RootActivity.openUrlInBrowser` no longer reports to Crashlytics.
- Removed the poll feature: `feature/poll/{public,impl}` modules, their `settings.gradle.kts` includes and app/home dependencies, `PollScreen` in `Screens.kt` and `IvyNavGraph.kt`, and the Home "vote" customer-journey card (`CustomerJourneyDeps` no longer has `pollRepository`).
- Changed `applicationId` from `com.ivy.wallet` to `com.wallet` (debug build: `com.wallet.debug`). `namespace` is unchanged. App name is now "Wallet" (debug: "Wallet Debug").
- Renamed the add-transaction shortcut action to `wallet.intent.action.add_transaction` (manifest + `shortcuts.xml`) so launcher shortcuts cannot open the official Ivy Wallet app.
- Verified: `assembleDebug`, `testDebugUnitTest` and `detekt` pass; APK package is `com.wallet.debug`, label "Wallet Debug".
- CI cleanup: deleted workflows that need upstream secrets or bots (`internal_release`, `automatic_release`, `apk`, `issue_assign`, `issue_created`, `pr_description`, `stale`) and `.github/CODEOWNERS`. Deleted the `ci-actions` bot modules (`issue-assign`, `issue-create-comment`, `pr-description-check`) and their includes. Kept `ci-actions/base` and `ci-actions/compose-stability` because the `compose_stability` workflow uses them, and trimmed `ci_actions_test.yml` to those two.
- Removed the `:shared:common-ui` include from `settings.gradle.kts`; its folder doesn't exist.
- Verified: `:ci-actions:base:test` and `:ci-actions:compose-stability:test` pass.
