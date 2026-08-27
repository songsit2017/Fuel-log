# Cross-app sync architecture

Status: production architecture, last reviewed 2026-08-11.

Payment-method routing changes under development are specified in
[`docs/FUEL-PAYMENT-ROUTING.md`](docs/FUEL-PAYMENT-ROUTING.md). The current
production behavior below remains in force until the coordinated rollout.

This document explains how Fuel Log and PU Pocket exchange data. Read it before changing sync code, database models, Firebase paths/rules, Supabase migrations/RPCs, receipt storage, or account/vehicle sharing.

## Non-negotiable invariants

1. Fuel Log is the source of truth for vehicles and fuel entries. PU Pocket stores a finance-facing projection, not a second editable copy of a fuel entry.
2. The cross-app flow is one-way: **Fuel Log -> PU Pocket**. PU Pocket's own Room/Supabase sync remains bidirectional.
3. A Fuel Log entry ID is stable. PU Pocket identifies it with `external_source = "fuel_log"` and `external_source_id = <entryId>`.
4. `(user_id, external_source, external_source_id)` stays unique. Retrying create/update events must update one transaction, never create duplicates.
5. Backend secrets never enter either APK. Only Firebase Cloud Functions may use the Supabase service-role key.
6. Imported receipts stay private. Persist Supabase object paths, not expiring signed URLs.
7. Deletion is propagated as a tombstone/soft delete; do not hard-delete user finance history from a client.
8. Firebase and Supabase are intentionally different schemas. When a **cross-app contract field** changes, update every affected model, encoder/decoder, adapter, RPC/schema, policy, and test. Do not try to mirror the entire databases.

## Topology

```mermaid
flowchart LR
    A["Fuel Log Room"] -->|save/edit| B["Firestore vehicles/{vehicleId}/entries/{entryId}"]
    B -->|onDocumentWritten| C["Firebase Function syncFuelEntryToPupu"]
    C -->|service role + RPC| D["Supabase upsert_fuel_log_transaction"]
    C -->|copy authorized receipt| E["Private pupu-receipts bucket"]
    D --> F["Supabase transactions projection"]
    F -->|authenticated pull + RLS| G["PU Pocket SyncEngine"]
    G --> H["Encrypted PU Pocket Room"]
```

There are three related but separate sync mechanisms:

- Fuel Log device sync: Room <-> Firestore/Storage through `FirestoreSyncRepository`.
- Cross-app bridge: Firestore -> Firebase Function -> controlled Supabase RPC.
- PU Pocket device sync: encrypted Room <-> Supabase through an outbox and incremental pull.

Do not bypass these boundaries by calling Supabase directly from Fuel Log or Firebase directly from PU Pocket.

## Pairing and authorization

1. A signed-in PU Pocket user selects the destination account/category.
2. `create-fuel-log-link` creates a 10-minute, 10-character code. Supabase stores only its SHA-256 hash.
3. A signed-in Fuel Log user submits that code and selected vehicle IDs to the Firebase callable `redeemPupuLink`.
4. The callable verifies Firebase vehicle membership before using the server-only Supabase credential.
5. Supabase `redeem_fuel_log_link` locks and consumes the code once, then creates one active link per selected vehicle.
6. The callable backfills existing entries. Later Firestore writes use the document trigger.

The link owns the PU Pocket destination account/category. It does not transfer ownership of a Fuel Log vehicle or merge the two family-sharing systems.

## Entry contract

| Fuel Log / Firestore | Bridge transformation | PU Pocket / Supabase |
| --- | --- | --- |
| Firestore document ID | stable source identity | `external_source_id` |
| constant source | `fuel_log` | `external_source` |
| `total` in baht | round once at boundary: `total * 100` | `amount_minor` as integer |
| `date` + `time` | interpret in Thailand (`+07:00`) | `transaction_date` |
| vehicle/station/liters/price/odometer/full | JSON projection | `external_metadata` |
| HTTPS receipt URLs | validate, copy server-side | private `receiptPaths` |
| document deletion | RPC with `p_deleted = true` | `deleted_at` soft delete |

Adding an optional metadata field should be additive and old readers must tolerate its absence. Renaming/removing a field, changing units, changing ID composition, changing time-zone handling, or changing money rounding is a breaking contract change.

## Conflict, ordering, and retry behavior

- Fuel Log's normal full device sync treats different local/cloud copies as a conflict. The save path uses `pushFuelEntry(entryId)` because the just-saved local record is authoritative and must trigger the bridge immediately.
- Firebase document triggers may retry. The Supabase uniqueness constraint and RPC make those retries idempotent.
- PU Pocket records a local mutation and its outbox row in one Room transaction. It pushes the outbox before pulling server rows.
- PU Pocket pulls parents before transactions, uses an overlapping incremental cursor, and does not overwrite a row with a pending local mutation.
- Supabase server timestamps and soft-delete tombstones are canonical. Device clocks are not conflict authority.
- A failed receipt copy may produce a partial receipt list while the financial transaction still imports. A later Firestore update can retry the copy/upsert.

## Critical implementation map

Fuel Log repository:

- `native-kotlin/.../data/firebase/FirestoreSyncRepository.kt`: Room/Firestore sync and immediate saved-entry push.
- `native-kotlin/.../data/firebase/PupuPocketLinkRepository.kt`: client side of pairing.
- `functions/index.js`: membership checks, mapping, receipt copy, backfill, Firestore trigger, Supabase calls.
- `firestore.rules`: vehicle membership and write authorization.

PU Pocket repository:

- `supabase/functions/create-fuel-log-link/index.ts`: authenticated pairing-code creation.
- `supabase/migrations/*fuel_log*`: append-only schema, RPC, uniqueness, and RLS contract.
- `data/.../sync/SyncOutbox.kt`: atomic local mutation/outbox recording.
- `data/.../sync/SyncEngine.kt`: push-first incremental Room/Supabase sync and external-field decoding.
- `data/.../local/entity/TransactionEntity.kt` and `domain/.../Transaction.kt`: local projection contract.

## Change gate

Before changing any item above:

1. Write the before/after contract and list both repositories affected.
2. Prefer additive backend changes first; keep old producer and consumer versions working.
3. Never edit an applied Supabase migration. Add a new timestamped migration.
4. Update both copies of this document and link companion PRs with the same feature slug.
5. Test create, edit, delete, retry/idempotency, backfill, receipts, login, language persistence, and both family-sharing systems.
6. Verify signed update installation without uninstalling either production app.

If the impact cannot be explained, stop and do not modify the sync contract.
