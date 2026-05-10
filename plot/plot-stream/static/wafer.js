(() => {
const container = document.getElementById("wafer-grid");
const COLS = 5;
const ROWS = 2;
const FACET_PX = 160;
const WAFER_RANGE = Array.from({ length: 37 }, (_, i) => i - 18); // -18..18
const COL_DOMAIN = Array.from({ length: COLS }, (_, i) => i);
const ROW_DOMAIN = Array.from({ length: ROWS }, (_, i) => i);

function render(rows) {
  const scans = [...new Set(rows.map(r => r.scan_id))].sort((a, b) => a - b);
  const idxByScan = new Map(scans.map((id, i) => [id, i]));
  const data = rows.map(r => {
    const i = idxByScan.get(r.scan_id);
    return { ...r, _col: i % COLS, _row: Math.floor(i / COLS) };
  });
  const extents = new Map();
  for (const r of rows) {
    const e = Math.max(Math.abs(r.x), Math.abs(r.y));
    extents.set(r.scan_id, Math.max(extents.get(r.scan_id) ?? 0, e));
  }
  const maxExtent = Math.max(0, ...extents.values());
  const bandPx = FACET_PX / Math.max(1, 2 * maxExtent + 1);

  const labels = scans.map((id, i) => ({
    scan_id: id,
    _col: i % COLS,
    _row: Math.floor(i / COLS),
    r_px: 1.05 * (extents.get(id) ?? 0) * bandPx,
  }));

  const chart = Plot.plot({
    width: COLS * FACET_PX + 60,
    aspectRatio: 1,
    style: { background: "transparent", color: "var(--fg)" },
    fx: { axis: null, domain: COL_DOMAIN },
    fy: { axis: null, domain: ROW_DOMAIN },
    x: { axis: null },
    y: { axis: null },
    color: { scheme: "magma", legend: true, label: "measurement_value" },
    marks: [
      Plot.cell(data, {
        x: "x", y: "y",
        fill: "measurement_value",
        fx: "_col", fy: "_row",
        inset: 0.5,
      }),
      /*
      Plot.dot(labels, {
        x: 0, y: 0,
        fx: "_col", fy: "_row",
        r: "r_px",
        fill: "none",
        stroke: "currentColor",
        strokeOpacity: 0.5,
      }),
      */
      Plot.text(labels, {
        fx: "_col", fy: "_row",
        text: d => `scan ${d.scan_id}`,
        frameAnchor: "top-left",
        dx: 4, dy: 4,
        fontSize: 11,
      }),
    ],
  });

  const old = container.querySelector("figure, svg");
  if (old) old.remove();
  container.appendChild(chart);
}

const source = new EventSource("/stream");
source.onmessage = (event) => {
  const payload = JSON.parse(event.data);
  render(payload.data);
};
source.onerror = (err) => {
  console.error("SSE error", err);
};
})();