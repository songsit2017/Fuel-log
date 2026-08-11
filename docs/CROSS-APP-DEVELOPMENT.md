# PU Pocket and Fuel Log development workflow

Last updated: 2026-08-11 (Asia/Bangkok)

This is the shared operating agreement for `songsit2017/PUPU-Pocket` and `songsit2017/Fuel-log`. Keep a copy in both repositories and update both copies in the same change when the process or integration contract changes.

Read the repository-root [`ARCHITECTURE.md`](../ARCHITECTURE.md) before changing any sync service, adapter, model, database contract, or sharing rule. This file defines the Git workflow; `ARCHITECTURE.md` defines how the production data flow works.

## Branch map

| Repository | Production | Integration | Feature examples |
| --- | --- | --- | --- |
| PU Pocket | `master` | `develop` | `agent/fuel-bridge-v2`, `feature/fuel-bridge-v2` |
| Fuel Log | `main` | `develop` | `agent/fuel-bridge-v2`, `feature/fuel-bridge-v2` |

Production branches contain only code intended for real users. Do not develop or push directly to them. `develop` is allowed to contain unreleased work but must remain buildable.

For every feature:

1. Fetch and verify a clean worktree with `git status -sb`.
2. Create paired feature worktrees from the latest `origin/develop` by following [`WORKTREE-WORKFLOW.md`](WORKTREE-WORKFLOW.md).
3. If both apps change, use the same feature slug in both repositories.
4. Open a Draft PR to `develop`; link the companion PR when there is one.
5. Require CI, code review, and the relevant local/ADB checks before merge.
6. Release only through a reviewed PR from `develop` to the production branch.
7. Tag and publish only from the production commit.

Example (run from either clean control checkout):

```powershell
.\scripts\New-CrossAppWorktree.ps1 -FeatureSlug fuel-bridge-v2 -PlanOnly
.\scripts\New-CrossAppWorktree.ps1 -FeatureSlug fuel-bridge-v2
```

## Ownership and integration contract

- Fuel Log owns vehicles, fill-ups, vehicle-family sharing, and their Firebase records.
- PU Pocket owns finance accounts, categories, budgets, transactions, finance-family sharing, and Supabase records.
- The financial bridge is one-way: Fuel Log produces saved fuel expenses; PU Pocket consumes them.
- PU Pocket identifies imported records with `externalSource = "fuel_log"` and `externalSourceId = <Fuel Log entry UUID>`.
- Upserts must stay idempotent on `(user_id, external_source, external_source_id)`.
- Receipt metadata and images remain private and must follow the existing authenticated storage policies.
- Do not move or remove either app's family-sharing UI or change ownership rules without an explicit request and migration plan.

## Safe rollout order

For a cross-app contract change, keep old and new versions compatible during rollout:

1. Add backend/database fields or policies without removing old fields.
2. Update the consumer (PU Pocket) so it accepts both old and new payloads.
3. Update the producer (Fuel Log) to send the new payload.
4. Verify production-like signed builds on a physical device.
5. Remove an old contract only after at least two stable releases and a confirmed migration.

Never rewrite an applied Supabase migration. Add a new timestamped migration. Do not mutate production user data for a test unless the user explicitly authorizes it and a restoration check is included.

## Pull request and release gate

Each PR description records:

- repository and companion PR, if any
- user-visible behavior and files/contracts changed
- database or security impact
- automated checks run
- ADB device and scenarios checked
- rollback plan

Before a coordinated stable release, verify:

- both apps build and their unit tests pass
- login and persisted language selection
- Fuel Log vehicle-family sharing
- PU Pocket account/family sharing
- pairing/re-pairing without duplicate transactions
- add and edit a fill-up update the same PU Pocket transaction
- receipt images remain accessible only to authorized users
- update install succeeds without uninstalling or replacing the signing key

Versions remain independent. Record the compatible pair in both release notes, for example `Fuel Log 1.0.x <-> PU Pocket 0.1.x`.

## CI and automation exception

- Feature CI builds/tests debug artifacts only; it does not publish a stable release.
- Stable release workflows must run from the production branch and use the existing release key.
- Fuel Log's scheduled oil-price workflow currently commits `oil-prices.json` directly to `main`. Account for this automation before enabling strict branch protection; later it can be changed to open an automated PR.

## Recovery

If a release regresses, stop the rollout, preserve logs, and revert the release PR with a new commit. Do not force-push, rewrite shared history, generate a replacement signing key, uninstall user apps, or delete production data as a shortcut.
