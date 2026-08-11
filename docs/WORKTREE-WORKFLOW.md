# Cross-app worktree workflow

Use paired Git worktrees for every new feature so PU Pocket and Fuel Log can change together without switching branches or touching the clean `develop` control checkouts.

## Directory and branch convention

```text
D:\App Projects\worktrees\<feature-slug>\
  PU-Pocket\  -> agent/<feature-slug> or feature/<feature-slug>
  Fuel-log\   -> agent/<feature-slug> or feature/<feature-slug>
```

The control checkouts stay here and remain clean:

- `D:\App Projects\PU Pocket` -> `develop`
- `D:\App Projects\Fuel-log` -> `develop`

Do not develop in the control checkouts. Never put one repository's worktree inside the other repository.

## Create a paired worktree

From either control checkout, run its tracked helper script:

```powershell
.\scripts\New-CrossAppWorktree.ps1 -FeatureSlug fuel-bridge-v2 -PlanOnly
.\scripts\New-CrossAppWorktree.ps1 -FeatureSlug fuel-bridge-v2
```

Use `-BranchPrefix feature` for a human-created branch. Codex uses the default `agent` prefix.

The script refuses to proceed when a control checkout is dirty or not on `develop`, `origin/develop` is missing, a local/remote branch already exists, the slug is unsafe, or a target path already exists. It validates both repositories before creating either worktree.

It also copies only the required, Git-ignored local build configuration into the new worktrees: PU Pocket's `local.properties` and Fuel Log's `native-kotlin/app/google-services.json`. It verifies that Git ignores each source before copying. Release keystores, signing properties, and passwords are never copied.

Manual equivalent:

```powershell
git -C "D:\App Projects\PU Pocket" fetch origin --prune
git -C "D:\App Projects\PU Pocket" worktree add -b agent/fuel-bridge-v2 "D:\App Projects\worktrees\fuel-bridge-v2\PU-Pocket" origin/develop

git -C "D:\App Projects\Fuel-log" fetch origin --prune
git -C "D:\App Projects\Fuel-log" worktree add -b agent/fuel-bridge-v2 "D:\App Projects\worktrees\fuel-bridge-v2\Fuel-log" origin/develop
```

## Work and review

1. Open only the two feature worktree folders in the editor/agent.
2. Read `AGENTS.md` and `ARCHITECTURE.md` in each worktree.
3. Commit each repository independently with a focused message.
4. Push the matching branch from each worktree.
5. Open companion Draft PRs into `develop` and link them to each other.
6. Require local build/tests and relevant ADB E2E checks before merge. GitHub cloud build is an optional manual clean-room check, normally used before a release or when local results are suspicious.

One branch can be checked out in only one worktree. If a branch already exists, resume its registered worktree instead of creating a duplicate.

## Finish safely

After both PRs are merged into `develop`:

1. Fetch and fast-forward both control checkouts.
2. Verify each feature worktree with `git status -sb`.
3. Remove only clean worktrees with `git worktree remove <exact-path>`.
4. Delete the merged local feature branches with `git branch -d <branch>` if no longer needed.
5. Run `git worktree prune` only to clean stale registration records.

Never use `git worktree remove --force`, `git branch -D`, or recursive filesystem deletion to bypass uncommitted changes. Stop and inspect the exact path instead.
