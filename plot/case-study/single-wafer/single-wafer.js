const threshold = -0.7;

const rows = await d3.csv("single-wafer.csv", d => {
    d3.autoType(d);
    d.pass = d.measurement_value > threshold ? "pass" : "fail"
    return d;
});

const plot = Plot.plot({
  width: 300,
  aspectRatio: 1,
  x: {axis: null},
  y: {axis: null},
  color: {
    domain: ["pass", "fail"],
    range: ["deepskyblue", "tomato"]
  },
  marks: [
    Plot.cell(rows, {x: "x", y: "y", fill: "pass" }) //inset: 0.5})
  ]
});

// const legend = plot.legend("color", {columns: "1fr"});  // single column

const el = document.querySelector("#myplot");
el.append(plot);
// el.append(plot, legend);