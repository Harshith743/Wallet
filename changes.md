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

## 2026-09-29

Branching: development happens on `dev`; `main` stays stable and receives merges from `dev`.

### Credit cards feature, phase 1 (data foundation)

- Added credit card domain models in `shared/data/model`: `CreditCard` (backed by an `Account` with the same id; holds cardholder, issuer, network, last 4, BIN, expiry, credit limit, statement day, due day, repayment account, UPI payee id), `CardNetwork`, `CardSecrets`, and primitives `DayOfMonth`, `CardPan`, `CardCvv`, `CardLast4`, `CardBin` (PAN/CVV print masked). Arb generators in `shared/data/model-testing/.../ArbCreditCard.kt`.
- Added card number utilities in `shared/domain/.../creditcard/`: `normalizeCardNumber`, `isValidLuhn`, `CardNetworkDetector` (longest prefix wins; `65…` resolves to RuPay except `6011…`), and `IndianIssuerBinTable` (small, unverified seed of Indian issuer BIN prefixes; user override wins).
- Added `CreditCardStatementCalculator` and `CreditCardStatement`/`StatementStatus` models: statement, due and next-statement dates from the day-of-month settings (month-end clamping), and due/unbilled/outstanding/available limit computed from the card account's settled transactions (spends before the statement date are due, repayments are transfers into the card, partial repayments reduce the due).
- Added Room table `credit_cards` (migration 130→131, schema `131.json`, DAOs, fake DAO, migration androidTest). No amounts and no secrets are stored in the table.
- Added card secrets storage: `CardSecretsCipher` (Android Keystore AES/GCM, key `wallet_card_secrets`, created lazily) and `CardSecretsStore` (private `card_secrets` SharedPreferences keyed by card id), with fakes for tests and Hilt bindings. Secrets never enter the database or backups.
