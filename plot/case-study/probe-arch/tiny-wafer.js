const rows = await d3.csv("tiny-wafer.csv", d => {
    d3.autoType(d);
    return d;
});

const plot = Plot.plot({
  width: 300,
  aspectRatio: 1,
  x: {axis: null},
  y: {axis: null},
  color: {
    scheme: "viridis"
  },
  marks: [
    Plot.cell(rows, {x: "x", y: "y", fill: "measurement_value" }) //inset: 0.5})
  ]
});

// const legend = plot.legend("color", {columns: "1fr"});  // single column

const el = document.querySelector("#myplot");
el.append(plot);
// el.append(plot, legend);