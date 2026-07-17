# Six-pack wafer SVG

Two deliverables, in light + dark variants:

1. **Six-pack maps** — `six-pack-light.svg` / `six-pack-dark.svg`: 6 real wafers in
   a 3 × 2 (L × W) grid — 3 across, 2 down — colored by pass/fail. Two of the
   wafers are fragmented (clipped), which is the point of the case study.
2. **Interleaved table** — `interleaved/interleaved-light.svg` /
   `interleaved-dark.svg`: the 10 most-recent measurements, showing wafers 5 & 6
   interleaved on the tester (see [Interleaved table](#interleaved-table) below).

Both are produced by one run of `generate-six-pack.py`.

## Source

Postgres, queried live:

```
postgresql://scanner_ro:applesauce@localhost:5432/postgres
```

```sql
select
    scan_id,               -- wafer identifier
    x,                     -- intra-wafer x coordinate
    y,                     -- intra-wafer y coordinate
    measurement_value > -0.5 as pass   -- color sources from here
from
    raw.measurement_stretched
where measurement_id < 1650    -- 6 wafers (scan_id 1-6), 2 fragmented, human to adjust as necessary
order by scan_id, x, y
```

`measurement_id < 1650` (human to adjust as necessary) yields exactly 6 wafers. Wafers 5 and 6 have clipped
x-ranges — those are the two fragmented ones. 

## Approach

One `<rect>` cell per measurement, colored by `pass`:

- `pass = true`  → `deepskyblue`
- `pass = false` → `tomato`

Every wafer is drawn on the **same fixed 21 × 21 coordinate frame** (x, y ∈
−10..10 — the full-wafer extent), with cell geometry from the reference
`single-wafer/single-wafer.svg` (cell offset `PAD=2`, pitch `PITCH=9`, rect
`SIZE=8`). The tile size is derived so it fits the frame exactly with no dead
margin: `TILE = PAD*2 + (XMAX - XMIN)*PITCH + SIZE` (= 192). Because the frame is
fixed, fragmented wafers simply have missing coordinates and render visibly cut —
no special handling needed. y is flipped (`PAD + (YMAX - y)*PITCH`) so positive y
is up. If the wafer radius changes, adjust `XMIN`/`XMAX`/`YMAX` to the new extent
and the tile resizes automatically.

Each wafer gets a `LABEL_H=16` px band above it holding its numeric `scan_id`
(bold, centered). Wafers are placed left-to-right, top-to-bottom into a 3 × 2 grid
with a `GAP=30` px gutter (larger = more space; 0 = flush). Canvas grows to fit
(636 × 446).

## Light / dark exports

Each deliverable is emitted once per entry in `THEMES`. The background is left
**transparent** (never painted) so each sits on whatever surface hosts it, and the
wafer/pass-fail cell colors (`deepskyblue` / `tomato`) are identical across themes.
Per-theme, `THEMES` defines:

- `text` — default text color: `#1e1e1e` (light) / `#d3d3d3` (dark).
- `c1` / `c2` — the two per-wafer band colors used by the interleaved table,
  alternating shades of grey, applied at 15% opacity. The bands alone signal which
  wafer a row belongs to; all table text uses `text`.

## Interleaved table

`interleaved/interleaved-{light,dark}.svg` is a monospace-styled table (rendered as
plain SVG `<text>`, not from a plotting lib) built by `build_table_svg()` from a
second query, `QUERY_TABLE`:

```sql
select measurement_id, scan_id as wafer_id, measurement_value > -0.5 as pass,
       x, y, modified_at
from raw.measurement_stretched
where measurement_id < 1650 -- 6 wafers (scan_id 1-6), 2 fragmented, human to adjust as necessary
order by measurement_id desc
limit 10
```

**Why it exists:** ordering by `measurement_id desc` surfaces the most-recent 10
rows, where wafers 5 & 6 are **interleaved** — both were on the tester at once, so
their measurements land intermixed (~2 ms apart by `modified_at`) rather than one
wafer completing before the next starts. The table makes that concrete.

Rendering details: each row gets a full-width band in its wafer's grey shade
(`c1`/`c2`, assigned in order of first appearance) so the interleaving is visible
at a glance. `pass` is the only colored text (`deepskyblue` / `tomato`) — everything
else, `wafer_id` included, uses the theme text color; `modified_at` prints as
`HH:MM:SS`. A brace spans all rows with a note naming the two interleaved wafers
(the wafer numbers are derived dynamically from the rows, so they track whatever
`QUERY_TABLE` returns). Output lands in the `interleaved/` subdirectory (created if
absent).

Note: the brace note's timing phrase ("rows land ~2 ms apart") is hardcoded in
`build_table_svg()` and is not derived from the data — revisit it if the
`modified_at` timescale changes.

## Regenerate

Everything lives in `generate-six-pack.py` (embeds the DSN, SQL, and layout
params). From the repo root:

```
python3 plot/case-study/six-pack/generate-six-pack.py
```

This writes four files: `six-pack-{light,dark}.svg` and
`interleaved/interleaved-{light,dark}.svg`.

Preview on macOS: `qlmanage -t -s 900 -o . six-pack-light.svg` (writes a `.png`).

To retune, edit the constants at the top of the script: `QUERY` /`QUERY_TABLE`
(data/threshold for the maps and the table), `PASS_FILL`/`FAIL_FILL` (cell colors),
`THEMES` (`text` + `c1`/`c2` band colors per theme), `COLS`/`ROWS`/`GAP`/`LABEL_H`
(map layout), `XMIN`/`XMAX`/`YMAX` (coordinate frame; tile size follows). The table
layout constants (row height, column x-offsets, brace/note geometry) are local to
`build_table_svg()`. Requires `psycopg2` and a reachable Postgres.