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
- Added `CreditCardRepository` (memo-cached card rows, `findAllIds()` for "is this account a card?"), `CreditCardSecretsRepository` (decrypts on demand; errors `Missing`/`DecryptFailed`/`EncryptFailed`) and `CreditCardMapper`; `DataWriteEvent.CreditCardChange` (`SaveCreditCards`, `DeleteCreditCards`; `AllDataChange` implements it).
- `TransactionRepository`: added `findAllByAccount` and `deleteAllByToAccountId` (plus the DAO query and fake), so deleting a card also removes the repayment transfers that point at it.
- Added use cases in `shared/domain/.../usecase/creditcard/`: `SaveCreditCardUseCase` (validates the draft, creates/updates the backing account with `includeInBalance = false` and the card row — card first — writes secrets, and turns opening due/unbilled into "Opening balance" expenses), `RecordCreditCardPaymentUseCase` (repayment = transfer from the paid-from account into the card, titled "<Issuer> Credit card repayment"; posts `AllDataChange`), `DeleteCreditCardUseCase` (transactions both ways, planned payments, account, card row, secrets), `CreditCardsOverviewUseCase` (joins cards with accounts and transactions, computes statements, total due and due-card count; skips cards whose account is missing) and `CreditCardPaymentsUseCase` (repayment history).
- Backup: `creditCards` list added to the backup JSON; import goes through `CreditCardRepository.saveMany` so the memo sees restored cards. Secrets are never exported.
- `LogoutLogic.deleteAllData()` wipes card rows and secrets. `TransactionsViewModel` now knows whether the account is a card (`TransactionsState.isCreditCard`) and deletes a card account through `DeleteCreditCardUseCase`.

### Credit cards feature, phase 1 (UI)

- New module `feature/credit-cards` (`com.ivy.creditcards`): the Credit Cards segment of the Accounts tab (`CreditCardsContent` with a card face, a stacked card layout where tapping the active card expands the list and tapping another card makes it active, a quick-action row with Mark as paid / Recent spends / Edit / Delete, an empty state), the pay sheet (amount prefilled with the due, required "Paid from" preselected with the repayment account, note; Pay now also opens the UPI app chooser, prefilled when the card has a UPI payee id), the add/edit card screen (card number with live network and issuer detection and override, cardholder, name, expiry, CVV, credit limit, statement and due days, opening due/unbilled amounts on create, colour, repayment account, UPI payee id, delete) and the card details screen (face, show/hide full number and CVV with a 30 s auto-hide, statement figures and dates, repayment history, edit, delete). Session state (open segment, active card) lives in `AccountsSegmentSession`.
- New navigation routes `EditCreditCardScreen(cardId?)` and `CreditCardDetailsScreen(cardId)`; `currencySymbol()` helper in `shared/ui/core` (₹ instead of INR); ~70 new strings and 3 plurals.
- Accounts tab: CRED-style sticky header with the reorder button (left, accounts only), an Accounts | Credit cards pill toggle and a settings button (right), then a centred caption and amount ("Total balance ₹x" with the excluded total underneath, or "Statement due for N cards ₹x" / "No statement due"). The last opened segment is remembered for the app session. Card accounts are hidden from the accounts list and from both totals (totals are now summed from the listed accounts, with the same exchange fallback as before). The horizontal swipe to Home is only active on the Accounts segment.
- Main screen: the "+" button adds a credit card when the Credit cards segment is open.
- Transactions screen: for a card's account, "edit" opens the card editor instead of the account modal (currency and include-in-balance must not change for a card).
- Paparazzi: new `CreditCardsPaparazziTest` (16 snapshots); `AccountsTabPaparazziTest` re-recorded with two extra cards-segment snapshots; the two orphan "accountTab composable" PNGs removed.
- Deferred to phase 2: swipe-up/swipe-left gestures, the bottom card carousel, the due-count badge on the toggle, the expandable breakdown drawer, a full payment history screen, card reorder, online BIN lookup and card skins.

## 2026-09-30

### Credit cards feature, phase 2

- Swipe gestures on card faces: swipe left slides the face away to reveal a 2×3 grid of quick actions (Mark as paid, Payment history, Recent spends, View details, Edit, Delete); swipe right, tap outside or back press closes it; only one card is revealed at a time. Swipe up on the active card expands the stack (tap still works). Built on Foundation's `anchoredDraggable` plus a small upward-only drag detector so the list keeps scrolling on downward and horizontal-ish drags. The quick-action row under the active card is gone. "Payment history" opens the card details until the dedicated screen lands.
- `CreditCardUiMapper` replaces the duplicated card-to-UI mapping in the cards and details ViewModels.
- Accounts header: a red badge on the "Credit cards" toggle shows how many cards have a statement due; a chevron next to the amount opens an inline breakdown drawer (accounts: each account with its balance and an "Excluded" tag, plus the excluded total; cards: each card with its due and status). The drawer's open state is remembered for the session.
