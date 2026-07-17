#!/usr/bin/env python3
"""Generate six-pack.svg from real measurement data.

Queries 6 wafers (2 fragmented) from Postgres, one cell per measurement,
colored by pass (measurement_value > 0). Wafers are plotted on a fixed
33x33 grid (x,y in -16..16) so fragmented wafers render visibly clipped.
Arranged in a 3 x 2 (L x W) grid to match the reference single-wafer style.
"""
import os
import psycopg2

DSN = "postgresql://scanner_ro:applesauce@localhost:5432/postgres"

QUERY = """
select
    scan_id, -- wafer identifier
    x, -- intra-wafer x coordinate
    y,-- intra-wafer y coordinate
--     measurement_value,
    measurement_value > -0.5 as pass -- color should source from here
from
    raw.measurement_stretched
where measurement_id < 1480 -- adjust so this produces a total of 6 wafers, 2 of which are fragmented, which is the purpose of the case study
order by scan_id, x, y
"""

# bleeding-edge records: the 10 most recent measurements, showing wafers
# 5 & 6 interleaved (both on the tester at once)
QUERY_TABLE = """
select
    measurement_id,
    scan_id as wafer_id,
    measurement_value > -0.5 as pass,
    x,
    y,
    modified_at
from raw.measurement_stretched
where measurement_id < 1480 -- adjust so this produces a total of 6 wafers, 2 of which are fragmented, which is the purpose of the case study
order by measurement_id desc
limit 10
"""

# --- grid geometry (matches reference single-wafer.svg) ---
PAD, PITCH, SIZE = 2, 9, 8          # cell offset, pitch, rect size
XMIN, XMAX, YMAX = -10, 10, 10     # fixed full-wafer frame -> 21x21 grid
TILE = PAD * 2 + (XMAX - XMIN) * PITCH + SIZE   # tile fits the frame (192)
PASS_FILL, FAIL_FILL = "deepskyblue", "tomato"

# --- six-pack layout ---
COLS, ROWS, GAP = 3, 2, 30
LABEL_H = 16                       # vertical room for the scan-id label above each map

# --- themes: transparent background; cells stay constant, only text varies ---
# c1/c2 = per-wafer band colors, alternating shades of grey @ 15% opacity.
# The bands alone signal which wafer a row belongs to; all text uses theme["text"].
THEMES = {
    "light": {"text": "#1e1e1e", "c1": "#334155", "c2": "#94a3b8"},
    "dark":  {"text": "#d3d3d3", "c1": "#e2e8f0", "c2": "#94a3b8"},
}

STYLE = """<style>:where(.plot-d6a7b5) {
  --plot-background: white;
  display: block;
  height: auto;
  height: intrinsic;
  max-width: 100%;
}
:where(.plot-d6a7b5 text),
:where(.plot-d6a7b5 tspan) {
  white-space: pre;
}</style>"""


def fetch():
    con = psycopg2.connect(DSN)
    rows = con.cursor()
    rows.execute(QUERY)
    wafers = {}
    for scan_id, x, y, passed in rows.fetchall():
        wafers.setdefault(scan_id, []).append((x, y, passed))
    con.close()
    return [(k, wafers[k]) for k in sorted(wafers)]


def wafer_group(cells):
    out = ['<g aria-label="cell">']
    for x, y, passed in cells:
        px = PAD + (x - XMIN) * PITCH
        py = PAD + (YMAX - y) * PITCH          # flip y so +y is up
        fill = PASS_FILL if passed else FAIL_FILL
        out.append(f'<rect x="{px}" width="{SIZE}" y="{py}" height="{SIZE}" fill="{fill}"/>')
    out.append("</g>")
    return "\n    ".join(out)


def build_svg(wafers, theme):
    row_h = LABEL_H + TILE                          # each tile: scan-id label + wafer
    W = COLS * TILE + (COLS - 1) * GAP
    H = ROWS * row_h + (ROWS - 1) * GAP
    parts = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        f'<svg xmlns="http://www.w3.org/2000/svg" class="plot-d6a7b5" fill="currentColor" '
        f'font-family="system-ui, sans-serif" font-size="10" text-anchor="middle" '
        f'width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
        "  " + STYLE,
    ]
    for i, (scan_id, cells) in enumerate(wafers):
        c, r = i % COLS, i // COLS
        dx, dy = c * (TILE + GAP), r * (row_h + GAP)
        label = (f'<text x="{dx + TILE / 2:g}" y="{dy + LABEL_H - 4}" '
                 f'font-size="12" font-weight="bold" fill="{theme["text"]}">{scan_id}</text>')
        parts += [
            "  " + label,
            f'  <g transform="translate({dx},{dy + LABEL_H})">',
            "  " + wafer_group(cells),
            "  </g>",
        ]
    parts.append("</svg>")
    return "\n".join(parts) + "\n", W, H


def fetch_table():
    con = psycopg2.connect(DSN)
    cur = con.cursor()
    cur.execute(QUERY_TABLE)
    rows = cur.fetchall()          # (measurement_id, wafer_id, pass, x, y, modified_at)
    con.close()
    return rows


def build_table_svg(rows, theme):
    ROWH, TOP, HDR_Y, RULE_Y = 26, 36, 22, 30
    BAND_X, BAND_W = 6, 548
    BRACE_X, SERIF, NOTE_X = 568, 8, 596
    W = 760
    H = TOP + ROWH * len(rows) + 8
    text = theme["text"]

    # assign a band color per wafer id, in order of first appearance
    palette = [theme["c1"], theme["c2"]]
    seen = []
    for _, wid, *_ in rows:
        if wid not in seen:
            seen.append(wid)
    color = {wid: palette[i % len(palette)] for i, wid in enumerate(seen)}

    p = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        f'<svg xmlns="http://www.w3.org/2000/svg" '
        f'font-family="ui-monospace, SFMono-Regular, Menlo, monospace" font-size="13" '
        f'width="{W}" height="{H}" viewBox="0 0 {W} {H}">',
        # header
        f'  <g font-weight="bold" fill="{text}">',
        f'    <text x="12" y="{HDR_Y}">measurement_id</text>',
        f'    <text x="150" y="{HDR_Y}">wafer_id</text>',
        f'    <text x="240" y="{HDR_Y}">pass</text>',
        f'    <text x="312" y="{HDR_Y}" text-anchor="end">x</text>',
        f'    <text x="350" y="{HDR_Y}" text-anchor="end">y</text>',
        f'    <text x="380" y="{HDR_Y}">modified_at</text>',
        '  </g>',
        f'  <line x1="8" y1="{RULE_Y}" x2="560" y2="{RULE_Y}" stroke="{text}" stroke-opacity="0.25"/>',
    ]

    # rows: band, then cells
    for i, (mid, wid, passed, x, y, mod) in enumerate(rows):
        by = TOP + ROWH * i
        ty = by + 17
        wc = color[wid]
        pf = PASS_FILL if passed else FAIL_FILL
        pl = "true" if passed else "false"
        ts = f"{mod:%H:%M:%S}"
        p += [
            f'  <rect x="{BAND_X}" y="{by}" width="{BAND_W}" height="{ROWH}" fill="{wc}" fill-opacity="0.15"/>',
            f'  <text x="12" y="{ty}" fill="{text}">{mid}</text>',
            f'  <text x="150" y="{ty}" fill="{text}">{wid}</text>',
            f'  <text x="240" y="{ty}" fill="{pf}" font-weight="bold">{pl}</text>',
            f'  <text x="312" y="{ty}" text-anchor="end" fill="{text}">{x}</text>',
            f'  <text x="350" y="{ty}" text-anchor="end" fill="{text}">{y}</text>',
            f'  <text x="380" y="{ty}" fill="{text}">{ts}</text>',
        ]

    # brace + annotation spanning all rows
    y0, y1 = TOP, TOP + ROWH * len(rows)
    mid_y = (y0 + y1) / 2
    lo, hi = min(seen), max(seen)
    note = [f"wafers {lo} &amp; {hi} interleaved —",
            "both on the tester at once,",
            "rows land within seconds",
            "of each other"]
    p.append(f'  <path d="M{BRACE_X - SERIF} {y0} H{BRACE_X} V{y1} H{BRACE_X - SERIF}" '
             f'fill="none" stroke="{text}" stroke-width="1.5"/>')
    p.append(f'  <g font-family="system-ui, sans-serif" fill="{text}" fill-opacity="0.7">')
    line_h = 18
    for i, line in enumerate(note):                 # vertical-center the note block on the brace
        dy = (i - (len(note) - 1) / 2) * line_h
        p.append(f'    <text x="{NOTE_X}" y="{mid_y + dy:g}">{line}</text>')
    p.append('  </g>')
    p.append('</svg>')
    return "\n".join(p) + "\n", W, H


def main():
    wafers = fetch()
    table = fetch_table()
    for name, theme in THEMES.items():
        svg, W, H = build_svg(wafers, theme)
        path = f"plot/case-study/six-pack/six-pack-{name}.svg"
        with open(path, "w") as f:
            f.write(svg)
        print(f"wrote {path}  {W}x{H}  wafers={len(wafers)}")

        tsvg, tW, tH = build_table_svg(table, theme)
        tdir = "plot/case-study/six-pack/interleaved"
        os.makedirs(tdir, exist_ok=True)
        tpath = f"{tdir}/interleaved-{name}.svg"
        with open(tpath, "w") as f:
            f.write(tsvg)
        print(f"wrote {tpath}  {tW}x{tH}  rows={len(table)}")


if __name__ == "__main__":
    main()
