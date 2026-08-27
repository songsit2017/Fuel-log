# Fuel Log instructions for coding agents

Before changing this repository, read [`ARCHITECTURE.md`](ARCHITECTURE.md) and [`docs/CROSS-APP-DEVELOPMENT.md`](docs/CROSS-APP-DEVELOPMENT.md). They define the production sync design and the operating agreement shared with PU Pocket.

## Working rules

- Preserve vehicle-family sharing, login, saved language, and the PU Pocket bridge unless the user explicitly requests a change.
- `main` is production and must match code released to real users. Never develop, commit, or push directly to it.
- `develop` is the integration branch. Start new work from the latest `origin/develop`.
- Use `agent/<feature-slug>` for Codex work and `feature/<feature-slug>` for human-created feature branches.
- Feature PRs target `develop`; only coordinated release PRs go from `develop` to `main`.
- If PU Pocket also changes, use the same feature slug in both repositories and link both PRs.
- Work in the normal checkout on a dedicated feature branch created from the latest `origin/develop`. Worktrees are optional and must be used only when the user explicitly requests parallel work.
- Do not publish stable tags/releases from a feature branch or from `develop`.
- Before switching branches, run `git status -sb`; never discard or stage unrelated user changes.
- Never commit Firebase configuration, `local.properties`, signing properties, keystores, credentials, access tokens, or build output.
- Do not replace the existing release key or uninstall the installed app to work around a signature problem.

## Required checks

Run from `native-kotlin`:

```powershell
gradle testDebugUnitTest assembleDebug --no-daemon
```

When a physical device is connected, also run the relevant ADB smoke/E2E scenarios. Stable releases must use the existing Fuel Log signing key and may run only from `main`.

The scheduled oil-price workflow currently writes `oil-prices.json` directly to `main`. Do not enable strict branch protection until that automation is converted to a PR or explicitly allowed.

## Architecture gate

- Treat `FirestoreSyncRepository`, `PupuPocketLinkRepository`, `functions/index.js`, the Firestore entry model/rules, and PU Pocket's Supabase import RPC as one cross-app contract.
- Do not modify sync services/adapters, ID composition, money units, date/time-zone conversion, deletion semantics, or receipt metadata until the impact is traced through [`ARCHITECTURE.md`](ARCHITECTURE.md).
- Do not rename or remove a cross-app model field. Add optional fields compatibly and keep old producer/consumer versions working during rollout.
- Firebase and Supabase do not have identical full schemas. For a contract change, update every affected Firebase field/rule, adapter mapping, Supabase migration/RPC/RLS policy, Room/domain model, and test.
- A cross-app PR must state the before/after contract, companion PR, rollout order, idempotency effect, security impact, and rollback plan.
