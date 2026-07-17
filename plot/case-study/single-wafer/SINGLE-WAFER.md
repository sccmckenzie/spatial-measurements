# case-study

Static HTML pages that render Observable Plot figures from CSV data.

## Exporting a rendered SVG

Once a page (e.g. `single-wafer.html`) is open in the browser and the plot has rendered:

1. Open DevTools (Cmd+Opt+J on macOS).
2. Paste the snippet below into the console and press Enter — the browser will download the SVG.

```js
const svg = document.querySelector("#myplot svg");
const blob = new Blob(
  ['<?xml version="1.0" encoding="UTF-8"?>\n', new XMLSerializer().serializeToString(svg)],
  {type: "image/svg+xml"}
);
Object.assign(document.createElement("a"), {
  href: URL.createObjectURL(blob),
  download: "wafer.svg"
}).click();
```

Change the `download` filename to suit. To pretty-print the resulting file:

```sh
xmllint --format wafer.svg -o wafer.svg
```