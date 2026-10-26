# Oil-price updates

The scheduled workflow continues checking every 30 minutes. A successful check
only writes `oil-prices.json` when source data changes. The comparison excludes
the generated top-level `fetchedAt` and generated Shell/PT/Caltex `updatedAt`
values. All other fields, including PTT's source effective date, Bangchak's
announcement dates and text, grades, and source URLs remain significant.

`fetchedAt` records the fetch that produced the saved snapshot; it is not a
heartbeat. For Shell/PT/Caltex, `updatedAt` records when changed content was
observed, not a source-provided effective date. An unchanged brand retains its
timestamp even when another brand changes. Existing JSON fields are preserved.

Each run logs its actual check time and source success/fallback status in the
GitHub Actions step summary. Failed comparison sources retain their saved data;
if the primary source fails, the existing valid snapshot is retained. A green
workflow therefore does not guarantee that every provider returned fresh data.

Run the offline regression tests with:

```sh
python -m unittest discover -s scripts -p "test_update_oil_price.py"
```

Changes land through `develop` and a reviewed release PR to `main`. Scheduled
production behavior changes only after release to `main`. Roll back by reverting
the script/workflow change; no client migration is needed.
