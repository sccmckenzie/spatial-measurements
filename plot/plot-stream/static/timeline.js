const container = document.getElementById("chart");

function render(rows) {
  const data = rows.map(r => ({ ...r, modified_at: new Date(r.modified_at) }));

  const chart = Plot.plot({
    width: container.clientWidth,
    height: 400,
    marginLeft: 80,
    style: { background: "transparent", color: "var(--fg)" },
    x: { type: "time", label: "modified_at" },
    y: { label: "scan_id", reverse: true },
  //  color: { scheme: "viridis", legend: false, reverse: true },
    marks: [
      Plot.barX(data, Plot.groupY({x1: "min", x2: "max", fill: "count"}, { x: "modified_at", y: "scan_id" })),
    ],
  });


  container.replaceChildren(chart);
}

const source = new EventSource("/stream");
source.onmessage = (event) => {
  const payload = JSON.parse(event.data);
  render(payload.data);
};
source.onerror = (err) => {
  console.error("SSE error", err);
};
