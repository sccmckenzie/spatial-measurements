# Generating per-wafer SVGs (guide for Claude)

How to batch-render standalone wafer SVGs from a multi-wafer CSV, matching the
visual output of `arch/tiny-wafer.js`.

## Constraints

- **Do this ad hoc.** Do not create or edit the `.js` / `.html` files already in
  place (e.g. `arch/tiny-wafer.js`, `arch/tiny-wafer.html`). They are
  browser-only and load a single-wafer CSV. The generation is a one-off script
  that lives in `/tmp`, not in the repo.
- Reuse the **same Plot config** as `arch/tiny-wafer.js` so output matches what
  renders in the browser (width 300, `aspectRatio: 1`, axes off,
  `color.scheme: "viridis"`, a single `Plot.cell` mark).

## Input

A CSV like `arch/tiny-wafers.csv` with columns
`measurement_id,measurement_value,scan_id,x,y,modified_at`. Multiple wafers are
distinguished by `scan_id`. Each wafer is a square grid of cells.

## Circular pruning strategy

The raw grid is a square with clipped single corners. To get a clean
circular/octagonal footprint, keep only cells within a radius:

```
keep row  ⟺  x*x + y*y <= 20
```

(20 is the threshold tuned for the ±4 grid: it drops the 8 stub cells at
`±4,±3` and `±3,±4` while keeping `±4,±2` etc. Adjust if the grid extent
changes.)

## Procedure

1. Make a temp workspace and install `jsdom` there (keeps the repo and global
   `node_modules` clean):

   ```sh
   mkdir -p /tmp/wafer-gen && cd /tmp/wafer-gen
   npm init -y >/dev/null
   npm install --no-audit --no-fund --silent jsdom
   ```

2. Write a script that loads the **local vendored** `d3.min.js` and
   `plot.umd.min.js` (from `plot/vendor/`) into a jsdom window — do not fetch
   from a CDN. Parse the CSV with `d3.csvParse(csv, d3.autoType)`, group by
   `scan_id`, apply the pruning filter, and render each wafer with
   `Plot.plot({document: window.document, ...sameConfig})`.

3. **Serialize with `XMLSerializer`, not `.outerHTML`.** This is the critical
   gotcha: jsdom's HTML serializer (`.outerHTML`) omits the
   `xmlns="http://www.w3.org/2000/svg"` namespace. A standalone `.svg` file
   without that namespace renders the `<style>` block as **visible text**
   instead of applying it. Use:

   ```js
   const xml = '<?xml version="1.0" encoding="UTF-8"?>\n'
     + new window.XMLSerializer().serializeToString(svg);
   ```

4. Write each wafer to `arch/tiny-wafer-<scan_id>.svg`.

5. Pretty-print the results so they are diffable:

   ```sh
   cd plot/case-study/arch
   for f in tiny-wafer-*.svg; do xmllint --format "$f" -o "$f"; done
   ```

## Verification

- The root element must be
  `<svg xmlns="http://www.w3.org/2000/svg" class="plot-..." ...>`.
- Open one SVG; it should show a colored wafer grid, **not** raw CSS text.
- Cell count per wafer should be the pruned count (e.g. 69 from an original 77).

## Reference implementation

See the working script that produced the current outputs (recreate in `/tmp` if
gone — `/tmp` is auto-cleaned by macOS after ~3 days or on reboot):

```js
import {readFileSync, writeFileSync} from "node:fs";
import {JSDOM} from "jsdom";

const REPO = "/Users/scottmckenzie/repo/spatial-measurements";
const VENDOR = `${REPO}/plot/vendor`;
const ARCH = `${REPO}/plot/case-study/arch`;

const {window} = new JSDOM("<!doctype html><html><body></body></html>",
  {runScripts: "outside-only"});
window.eval(readFileSync(`${VENDOR}/d3.min.js`, "utf8"));
window.eval(readFileSync(`${VENDOR}/plot.umd.min.js`, "utf8"));
const {d3, Plot} = window;

const allRows = d3.csvParse(readFileSync(`${ARCH}/tiny-wafers.csv`, "utf8"),
  d3.autoType);
const scanIds = Array.from(new Set(allRows.map(d => d.scan_id)))
  .sort(d3.ascending);

for (const id of scanIds) {
  const rows = allRows.filter(d =>
    d.scan_id === id && d.x * d.x + d.y * d.y <= 20);
  const plot = Plot.plot({
    document: window.document,
    width: 300, aspectRatio: 1,
    x: {axis: null}, y: {axis: null},
    color: {scheme: "viridis"},
    marks: [Plot.cell(rows, {x: "x", y: "y", fill: "measurement_value"})]
  });
  const svg = plot.tagName.toLowerCase() === "svg"
    ? plot : plot.querySelector("svg");
  writeFileSync(`${ARCH}/tiny-wafer-${id}.svg`,
    '<?xml version="1.0" encoding="UTF-8"?>\n'
    + new window.XMLSerializer().serializeToString(svg));
}
```