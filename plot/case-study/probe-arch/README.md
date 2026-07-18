# Restoring the dashboard inside the "Application" window (probe-arch SVGs)

Reference for Claude (and humans) if the wafer heatmap + bar chart ever go missing
from `probe-arch-light.svg` / `probe-arch-dark.svg` again.

## The two files

- `probe-arch-light.svg` and `probe-arch-dark.svg` are **Excalidraw SVG exports**
  (`<!-- svg-source:excalidraw -->`), each written as one giant single line.
- They are **identical in geometry** — every `transform="translate(x y)"` matches.
  They differ **only in color** (light uses `#1e1e1e`/`#fff`/`#ced4da`; dark uses
  `#d3d3d3`/`#121212`/`#33383d`, etc.). So whatever you do to one, do to the other,
  but pull colors from each file's own source.

## What "the dashboard" is

The right-hand node is a browser/`Application` window (rounded window frame, a
`#ced4da` title bar, three traffic-light dots `#fa5252`/`#fab005`/`#40c057`). Layered
on top of a hidden "Google" search mockup + a white cover rectangle, HEAD also drew a
small data dashboard. That dashboard is what tends to get dropped:

1. **Wafer heatmap** — a base64-embedded Observable Plot SVG of tomato/deepskyblue
   cells. Lives as a `<symbol id="image-a462ec2b7da67a2c8bc02c4bdaf098aa14c1034e">`
   in `<defs>`, referenced by a `<use href="#image-...">` group.
2. **Tomato bar** — a `<g>` whose fill is `fill="tomato"` (tall bar).
3. **Deepskyblue bar** — a `<g>` whose fill is `fill="deepskyblue"` (shorter bar).
4. **Axis** — a single horizontal baseline under the bars
   (`<g stroke-linecap="round"><g transform="translate(...)"><path .../></g></g>`).

Quick check for whether they're present:

```bash
grep -oE 'fill="tomato"|fill="deepskyblue"|href="#image' probe-arch-light.svg | sort | uniq -c
# healthy output: 1 of each. Zero = dashboard is missing.
```

## The catch: the window may have moved

The diagram gets re-laid-out over time (e.g. "Reporting DB + App" was once one node,
later split into separate "Reporting DB" and "Application" nodes), which **shifts the
Application window**. You cannot paste HEAD's dashboard markup verbatim — you must
translate it by the same delta the window moved.

Find the delta from an element that exists in **both** the source (e.g. `git show
HEAD:...`) and the current file — the window body or a traffic-light dot works well:

```
delta = current_translate − source_translate
```

Last time this was done the delta was `dx = -8.613281250`, `dy = +3.515625000`
(verified against both the window-body group and the first traffic-light dot — they
agreed exactly). **Recompute it; don't assume this value.**

## Procedure

1. Pretty-print the source (HEAD or wherever the good copy is) so you can grab blocks:
   ```bash
   git show HEAD:plot/case-study/probe-arch/probe-arch-light.svg \
     | python3 -c "import sys,re;print(re.sub(r'>\s*<','>\n<',sys.stdin.read()))" > /tmp/head-light.svg
   ```
2. Locate the five blocks by their markers (`<symbol`, `href="#image`,
   `fill="tomato"`, `fill="deepskyblue"`, and the axis `translate(...)`), grab each
   complete `<g>…</g>` (and the `<symbol>…</symbol>`).
3. Shift the **outermost** `translate()` of the wafer/tomato/deepskyblue groups and
   the **inner** `translate()` of the axis group by the delta. (Bar/wafer cell colors
   and the `tomato`/`deepskyblue` fills stay as-is; the axis `stroke` should already be
   the right per-theme color because you pulled it from that theme's own source.)
4. Inject the `<symbol>` right after `<defs>`, and the four groups right before
   `</svg>`. SVG ignores whitespace between tags, so inserting newline-joined blocks
   into the single-line file is fine.
5. Do the same for the **dark** file, pulling the `<symbol>` and axis stroke color from
   `probe-arch-dark.svg`'s own HEAD (dark wafer image / axis color differ from light).
6. No `viewBox`/`width` change is needed — the dashboard fits inside the existing
   canvas. Only widen if content actually clips.

## Verifying (no Inkscape/rsvg on this machine — use qlmanage)

```bash
# zoom into just the app window: rewrite viewBox to the window region, then rasterize
python3 -c "
import re
s=open('probe-arch-light.svg').read()
s=re.sub(r'viewBox=\"[^\"]*\"','viewBox=\"675 60 165 130\"',s,1)
s=re.sub(r'width=\"[0-9.]+\" height=\"[0-9.]+\"','width=\"660\" height=\"520\"',s,1)
open('/tmp/zoom.svg','w').write(s)"
qlmanage -t -s 1200 -o /tmp /tmp/zoom.svg   # produces /tmp/zoom.svg.png
```

Expected result: wafer heatmap on the left, a tall tomato bar + shorter deepskyblue bar
on the right, and a horizontal axis line under the bars — all inside the window frame,
not overlapping the title bar or spilling outside. Check the dark file too (axis line
should be light gray on the dark background).

## Related

- The `excalidraw-rework` skill covers dark-mode color regressions in these exports
  (inverted embedded plots, mis-mapped native-shape fills) and recoloring wafer cells.