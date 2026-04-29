# vendor

Download these two files into this directory before running the app.

**Important:** use the UMD build of Plot (`plot.umd.min.js`), not `plot.min.js`. The latter
is an ES module that won't work as a classic `<script>` and won't define a global `Plot`.

- https://cdn.jsdelivr.net/npm/@observablehq/plot/dist/plot.umd.min.js
- https://cdn.jsdelivr.net/npm/d3/dist/d3.min.js

```bash
curl -o d3.min.js https://cdn.jsdelivr.net/npm/d3/dist/d3.min.js
curl -o plot.umd.min.js https://cdn.jsdelivr.net/npm/@observablehq/plot/dist/plot.umd.min.js
```
