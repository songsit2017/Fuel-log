# Fuelio .fuelio/CSV export format — reference notes

This documents the **real** Fuelio backup format, reverse-engineered from actual user-exported
`.csv` files (not from Fuelio's own docs, which don't cover this). It exists because the format
has several non-obvious quirks that broke the importer three separate times before real sample
files were available to check against. Parsing logic lives in
`app/src/main/java/com/songsit/fuellogpro/data/FuelioImportRepository.kt`
(`parseFuelioCsv()`); regression tests using real excerpts are in
`app/src/test/java/com/songsit/fuellogpro/data/FuelioImportRepositoryTest.kt`.

If this format changes again in the future (new Fuelio version, different locale, etc.), get a
real sample file from the user first — every past regression here came from guessing at column
names instead of checking real data.

## Container

A `.fuelio` backup is a **zip** containing one **quoted, comma-or-semicolon-delimited pseudo-CSV
per vehicle** (delimiter is auto-detected per line by counting `;` vs `,`). Each such CSV bundles
multiple sections for that one vehicle, marked by a line whose first field starts with `##`,
e.g. `"## Log"`. Sections seen in practice, always in this order: `## Vehicle`, `## Log`,
`## CostCategories`, `## Costs`, `## FavStations`, `## Pictures`.

Every section (except free-text vehicle-metadata lines) is **header row + data rows**, not
key/value pairs — do not assume a `"Car","MyCar"`-style layout.

Additionally, the zip contains a nested zip named `pictures.data` holding every attached photo
as its own file, referenced by filename from the `## Pictures` section (see below).

## `## Vehicle`

Header (real column order):
```
"Name","Description","DistUnit","FuelUnit","ConsumptionUnit","ImportCSVDateFormat","VIN","Insurance","Plate","Make","Model","Year","TankCount","Tank1Type","Tank2Type","Active","Tank1Capacity","Tank2Capacity","FuelUnitTank2","FuelConsumptionTank2","guid","lastupdated"
```
Followed by exactly one data row. `Name` is the vehicle's display name, `Plate` is registration.
There is no reliable free-text fuel-type field at the vehicle level (`FuelType` only exists
per-`## Log`-row as a numeric code).

## `## Log` (fuel fill-ups)

Header (real column order):
```
"Data","Odo (km)","Fuel (litres)","Full","Price (optional)","km/l (optional)","latitude (optional)","longitude (optional)","City (optional)","Notes (optional)","Missed","TankNumber","FuelType","VolumePrice","StationID (optional)","ExcludeDistance","UniqueId","TankCalc","Weather","guid","lastupdated"
```

Quirks that are easy to get wrong:
- **The date column is spelled "Data", not "Date"** — a real quirk of the app, not a typo in
  this doc. Match both.
- **"Data" packs date *and* time together** as `"yyyy-MM-dd HH:mm"` (e.g. `"2026-05-26 11:20"`).
  Split on the first space; there is no separate time column in real exports.
- **"Price (optional)" is the gross total, not the per-liter price.** The real per-liter price
  is the separate `"VolumePrice"` column. Both header strings contain the substring `"price"`,
  so `VolumePrice` must be matched and excluded *first*, or the total gets misread as a
  per-liter price (and vice versa when computing `amount`). Verified against real rows: e.g.
  `Fuel(litres)=72.604`, `Price(optional)=3000.0`, `VolumePrice=41.32`, and
  `3000.0 / 72.604 ≈ 41.32`.
- **The station/place name is under `"City (optional)"`**, not a column containing "station" —
  `"StationID (optional)"` is a separate *numeric* reference id (matches `## FavStations`'
  `StationID`), not a display name.
- `"Full"` isn't strictly `0`/`1` — values of `2` are also seen in real data (dual-tank / other
  fill variants). Treat anything other than `"0"` as a full tank.
- `"UniqueId"` is a small integer, **not globally unique on its own** — see the Pictures section
  below.
- `"Missed"` (previous-fill-missed) rows should be imported and flagged
  (`FuelEntryEntity.missedPreviousFillUp`), not silently dropped — a missed-fill row is still a
  real fill-up, it just shouldn't be used to compute the km/L interval against the prior row.

## `## CostCategories` / `## Costs` (expenses)

`## CostCategories` header: `"CostTypeID","Name","priority","color","guid","lastupdated"` — a
simple id→name lookup table consumed while parsing `## Costs` below.

`## Costs` header (real column order):
```
"CostTitle","Date","Odo","CostTypeID","Notes","Cost","flag","idR","read","RemindOdo","RemindDate","isTemplate","RepeatOdo","RepeatMonths","isIncome","UniqueId","guid","lastupdated"
```
- Unlike `## Log`, this section's date column really is spelled `"Date"` — but it's **still a
  combined `"yyyy-MM-dd HH:mm"` value**, same split-on-space treatment as `## Log`'s `"Data"`.
- **The money column is literally named `"Cost"`**, not `"Price"`/`"Amount"`. Match it with an
  *exact* normalized comparison (`cost`/`amount`/`price`), not a loose substring check — a loose
  `contains("cost")` would also match the `"CostTitle"` column and pick the wrong field.
- `"CostTypeID"` is a numeric id resolved through `## CostCategories`, not a text category.
- Rows where `Cost <= 0` (e.g. template/recurring placeholder rows) are intentionally skipped.

## `## Pictures`

Header: `"Filename","Note","Type","target_id","guid","lastupdated"`.

- `target_id` matches a `## Log` or `## Costs` row's `UniqueId`.
- **`## Log` and `## Costs` draw from two separate auto-increment `UniqueId` sequences that are
  each shared globally across every vehicle in the same backup** — so a `## Log` row and a
  `## Costs` row can legitimately have the *same* `UniqueId` (confirmed in real data: `UniqueId
  204` exists in both one vehicle's Log and another vehicle's Costs). `Type` disambiguates which
  sequence a picture's `target_id` belongs to: **`Type = "1"` → `## Log` row, `Type = "2"` →
  `## Costs` row.** Always scope the lookup by `(Type, target_id)`, never by `target_id` alone,
  or photos will attach to the wrong record (or none at all).
- A picture's `## Pictures` entry can live in a **different vehicle's CSV file** than the row it
  belongs to (confirmed in real data) — when importing a whole zip, collect every `.csv` entry's
  `## Pictures` table into one combined map *before* linking any row to a photo, not per-file.
- Filenames reference entries inside the nested `pictures.data` zip by basename
  (case-insensitive).

## `## FavStations`

Header: `"NameBrand","Latitude","Longitude","StationID","Description","CountryCode","guid","lastupdated"`
— a saved-places lookup table; `StationID` here is what `## Log`'s `"StationID (optional)"`
column references. Not currently consumed by the importer (station display name comes from
`## Log`'s own `"City (optional)"` column instead).
