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
  const labels = scans.map((id, i) => ({
    scan_id: id,
    _col: i % COLS,
    _row: Math.floor(i / COLS),
  }));

  const chart = Plot.plot({
    width: COLS * FACET_PX + 60,
    aspectRatio: 1,
    style: { background: "transparent", color: "var(--fg)" },
    fx: { axis: null, domain: COL_DOMAIN },
    fy: { axis: null, domain: ROW_DOMAIN },
    x: { axis: null, domain: WAFER_RANGE },
    y: { axis: null, reverse: true, domain: WAFER_RANGE },
    color: { scheme: "magma", legend: true, label: "measurement_value" },
    marks: [
      Plot.cell(data, {
        x: "x", y: "y",
        fill: "measurement_value",
        fx: "_col", fy: "_row",
        inset: 0.5,
      }),
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