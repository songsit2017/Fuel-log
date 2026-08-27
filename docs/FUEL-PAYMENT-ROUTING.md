# Fuel payment routing (2026-08-27)

Release pair: PU Pocket 0.2.34 / Fuel Log 1.3.5. Deployment and signed-artifact
evidence is recorded in the coordinated GitHub releases, not inferred from source.

## Before and after

Before: Fuel Log v1.3.3 stores `paymentMethod` in Room but omits it from
Firestore encoding/decoding. The bridge assigns every entry to the account in
its vehicle link, including soft-deleted accounts. Such transactions are hidden
by PU Pocket's active-account queries.

After: Firestore entries optionally carry `paymentMethod` (unchanged free-text
source value). The bridge adds optional `external_metadata.payment`:
`{version:1, method, label, source, reason?}`.
Methods are CASH, BANK, CREDIT_CARD, E_WALLET, or UNKNOWN. Sources are fuel_log,
receipt, no_receipt, or unresolved. Existing IDs, baht-to-minor-unit rounding,
Thailand dates, receipts, and sharing ownership do not change.

Resolution order:
1. A nonblank Fuel Log payment method wins, including custom labels.
2. Without it, inspect attached images using the existing server-side OCR
   provider. Read only payer/card evidence, not a merchant's receiving bank.
3. No attachments, or all images confidently identified as non-receipts, means
   cash. Failed downloads, unreadable receipts, unsupported documents, and
   contradictory evidence mean unresolved, never cash.

A VISA/Mastercard label alone cannot distinguish credit from debit. Receipt OCR
must provide a literal CREDIT/เครดิต marker before classifying CREDIT_CARD.
Network-only evidence remains unresolved. Cache schema v2 invalidates older
classifications. Automatic fallback sends images to the existing server-side
Anthropic provider; it stores only bounded payment evidence, not raw OCR text.

Match only active THB accounts owned by the linked PU Pocket user. A unique
matching name/type or recognized institution is eligible. A generic method can
match a single active account of its type. Never choose the first of multiple
matches, a different named card, another user's account, or a deleted account.
No accounts are created automatically.

The append-only migration keeps the existing import RPC signature. Legacy
bridge calls without payment metadata retain the active link destination;
deleted/invalid destinations are held for review. New calls without a safe
account/category match go to an owner-readable pending-import table, with their
original payload and source identity preserved. PU Pocket shows pending entries
inside the existing Fuel Log dialog and can retry routing after account/category
configuration is corrected. Correct an ambiguous method in Fuel Log itself.
Retries upsert the original transaction, never a second expense.

Versioned source events carry `sourceUpdatedAt`. A per-user/source lock and
retained watermark protect edits/deletes against late arrivals, including an
entry deleted before its first successful import. Once a source has a watermark,
unversioned legacy calls cannot overwrite it. An unresolved update preserves the
previous finance row and stores the latest payload in pending, not a second row.

Fresh OCR is capped at six images per live entry and ten requests per pairing
backfill; validated evidence is cached by private image fingerprint. Unreadable,
over-budget, or unsupported PDF evidence remains pending. Retry in PU Pocket
only retries account/category routing of the stored evidence; it does not run
OCR again. Set the payment method in Fuel Log to resolve such cases, or approve
a bounded server replay after reviewing the historical selection. The pairing
backfill still iterates history and is not a resumable bulk-import job.
The native pairing timeout is 540 seconds, matching the callable's upper bound.

## Options and trade-offs

- Device-side inference could reuse UI OCR, but would require each device to
  interpret receipts and could produce different destinations.
- Server-side inference (chosen) gives one canonical projection and no APK
  secrets. It adds bounded OCR work for legacy records; do not run an unbounded
  historical backfill without a reviewed dry run.

## Affected components

Fuel Log: Firestore encoder/decoder and payment-only legacy-field enrichment,
bridge payment resolver, private receipt loading, OCR fallback and tests.
Existing Room/domain payment fields stay unchanged. Firestore membership rules
remain unchanged; backend OCR cache is outside client-readable vehicle paths.

PU Pocket: new migration/RPC/pending table and RLS, pending-import reader/retry
UI, protocol tests, both architecture records. Existing Room schema and external
metadata readers already tolerate additional JSON fields.

## Rollout, verification and rollback

1. Test migration on an isolated database, including cross-user denial,
   deleted accounts, retries, and legacy calls.
2. Apply the new migration only after reviewing remote migration history.
3. Deploy the bridge, with its existing OCR secret bound server-side.
4. Release both signed Android updates through develop -> production PRs.
5. Dry-run historical entries, review unresolved cases, then authorize replay
   of the bounded selection using unchanged source IDs.

Verify explicit cash/card/bank, custom labels, old missing fields, no images,
non-receipt photos, unreadable/conflicting receipts, source edits, source deletes,
duplicate delivery, pending retry, receipt privacy, login, language, and both
family-sharing flows. Do not uninstall production apps or replace release keys.

Rollback: revert the feature in new commits, retaining the additive pending and
watermark tables/data. If returning to the unversioned bridge, deploy a reviewed
new RPC rollback migration as well; simply reverting the bridge would leave
watermarked entries protected from its writes. Never hard-delete finance history.
Companion PRs: linked in the draft PR descriptions.

## Verification performed (2026-08-27)

- PU Pocket: `:domain:test :data:testDebugUnitTest :app:assembleDebug` passed
  (20 existing unit tests; Gradle reused unchanged test outputs).
- Fuel Log: `testDebugUnitTest assembleDebug` passed (25 tests, including three
  new payment encoder/legacy-enrichment tests).
- Fuel Log Node 20 `functions` tests: 30 tests passed, including actual trigger/
  callable wiring with in-memory service doubles, receipt decision reuse,
  authentication/member denial, and bounded historical OCR.
- PU Pocket `supabase/tests/npm test`: 21 tests passed against all repository
  migrations in isolated PGlite/Postgres, using synthetic auth/storage scaffolds.
  Includes hidden legacy-row repair, unchanged identity/money/date/receipt,
  stale events/deletes, deleted/ambiguous destinations, owner-only pending retry,
  and unchanged family transaction/private-receipt visibility. No live SQL writes.
- SM-S948B: both `.dev` APKs updated in place successfully, with production APKs
  untouched. Both launch. PU Pocket's Fuel Log dialog renders and safely reports
  the not-yet-deployed backend. Fuel Log's new-fill form/payment dropdown opens
  with cash/bank/card choices; exited without saving or pairing. Local screenshots
  are ignored, not committed.

Initial debug validation did not deploy a backend or replay history. The release
gate additionally checks signed updates, deployed pending access and source/finance
round-trip behavior; consult release records for those results. A real network-only
VISA receipt was verified to remain unresolved rather than guessed credit. Bulk
historical replay is separate from deploying this release and needs a reviewed
selection; never re-pair simply to force an unbounded replay.

Dependency audit: existing Google SDK transitives report a moderate uuid buffer
bounds advisory for v3/v5/v6. Inspected Google callers use v4, and the bridge uses
crypto SHA-256/source IDs, not those APIs. The suggested forced SDK downgrade was
not applied as part of this release. No new runtime package was introduced.

Fuel Log prerequisite: `develop` lacked released native payment support. The
isolated, user-approved worktree merges released `origin/main` at `362d2fc` into
the feature branch (`f0c3d28`) before the feature changes. Review that branch
alignment separately; the original dirty Fuel Log checkout is untouched.
