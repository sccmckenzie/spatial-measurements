# Fragmented Spatial Scans with Batch Data Processing

## Background

This article is inspired by a real data engineering scenario encountered in semiconductor manufacturing. Before diving into the main topic, a few basics for those unfamiliar with the industry:

* The main manufacturing "unit" is the wafer - a circular disc comprised of an array of identical chips. The process involves building up the wafer (and chips) layer-by-layer. 
* Without getting into a broad overview of semiconductor process, it's worth noting that chip manufacturing generates massive amounts of structured data. One of the principle challenge facing semi engineers is effectively *harvesting* this data to drive intelligent decision-making.
* Prior to shipment to customer, each chip is electrically tested to ensure quality control. Additionally, the electrical test data is a critical ingredient used for **yield analysis** - identifying targeted improvements throughout the fab process which ultimately reduce end of line chip failures.

Perhaps the most quintessential method for visualizing electrical test performance is the **wafer map**, example shown below.

<picture>
    <source media="(prefers-color-scheme: dark)" srcset="plot/case-study/single-wafer/single-wafer-annotated-excalidraw-dark.svg">
    <img src="plot/case-study/single-wafer/single-wafer-annotated-excalidraw-light.svg">
</picture>

<picture>
    <source media="(prefers-color-scheme: dark)" srcset="plot/case-study/probe-arch/probe-arch-binary-dark.svg">
    <img src="plot/case-study/probe-arch/probe-arch-binary-light.svg">
</picture>
