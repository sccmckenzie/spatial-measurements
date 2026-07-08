# Overcoming Fragmented Spatial Mapscc

## Background

Semiconductor manufacturing generates massive amounts of structured data. Harvesting insights from this data to drive better decision-making is one of the principal competencies of a semi engineer. But those insights rest on a long chain of data movement — and as we'll see, that path from tester to analyst is deceptively hard to engineer.

A few basics for those unfamiliar with the industry:

* The main manufacturing "unit" is the wafer - a circular disc comprised of an array of identical chips. The process involves building up the wafer (and chips) layer-by-layer. 
* Prior to shipment to customer, each chip is electrically tested to ensure quality control. Additionally, the electrical test data is a critical ingredient used for **yield analysis** - identifying targeted improvements throughout the fab process which ultimately reduce end of line chip failures. 
* Perhaps the most quintessential method for visualizing electrical test performance is the **wafer map**, shown below.

<p align="center">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="plot/case-study/single-wafer/single-wafer-annotated-dark.svg">
        <img src="plot/case-study/single-wafer/single-wafer-annotated-light.svg" alt="A wafer map: a grid of chips colored by electrical test result.">
    </picture>
</p>
<p align="center"><sub><em>Figure 1: A wafer map. Each cell is a single chip, colored by its electrical test result.</em></sub></p>

Wafer maps help visualize potential spatial patterns. Random failures tend to reflect baseline process noise, but *spatially correlated* failures often betray a specific root cause - ring patterns from non-uniform deposition, edge effects from a misaligned etch tool, or scratches from handling.

<p align="center">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="plot/case-study/signature/signatures-dark.svg">
        <img src="plot/case-study/signature/signatures-light.svg" alt="Common spatial failure signatures on wafer maps: rings, edge effects, and scratches.">
    </picture>
</p>
<p align="center"><sub><em>Figure 2: Spatially correlated failure signatures — rings, edge effects, and scratches — each pointing to a distinct root cause.</em></sub></p>

## The Data Engineering Challenge

Below diagram shows the journey from **measurement** to **analysis application**. Note the separation between the Production and Reporting DB - this is vital to ensure Production DB remains available to support processing & movement of material (i.e. if the electrical testers cannot run, the line stops). 

<p align="center">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="plot/case-study/probe-arch/probe-arch-dark.svg">
        <img src="plot/case-study/probe-arch/probe-arch-light.svg" alt="Data journey from measurement to analysis application, with separate Production and Reporting databases.">
    </picture>
</p>
<p align="center"><sub><em>Figure 3: The journey of electrical test data, from measurement to analysis application. The Production and Reporting DBs are deliberately kept separate.</em></sub></p>

For the purposes of this case study, we'll assume both databases live in the same environment, so mirroring Production into Reporting is straightforward.

The challenge lies in what that faithful mirror hands us. Because we have no control over how the Production DB is written, the electrical test data lands in the Reporting DB exactly as production produced it — and as we'll see, that's where things get interesting.

Suppose the factory is running at full capacity: our testers are continuously measuring wafers and writing to the Production DB, which is natively replicated to the Reporting DB with minimal latency. Now imagine someone opens the Application to plot the six most recent wafer maps. As the figure below shows, some of them come back **incomplete**:

<p align="center">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="plot/case-study/six-pack/six-pack-dark.svg">
        <img src="plot/case-study/six-pack/six-pack-light.svg" alt="The six most recent wafer maps; wafers 5 and 6 are incomplete.">
    </picture>
</p>
<p align="center"><sub><em>Figure 4: The six most recent wafer maps. Wafers 5 and 6 come back incomplete — their measurements are still being written to the Production DB.</em></sub></p>

From an end-user perspective, this is unacceptable. Not only would incomplete maps undermine trust in the Application, but the gaps are silent — anyone aggregating statistics in the data layer would fold the missing measurements into invalid results without ever noticing (see records 5 & 6 below).

| wafer\_id | num\_passing | num\_total |
| :------- | :---------- | :-------- |
| 1        | 272         | 373       |
| 2        | 244         | 313       |
| 3        | 240         | 313       |
| 4        | 290         | 373       |
| 5        | 195 ⚠️      | 249 ⚠️    |
| 6        | 136 ⚠️      | 193 ⚠️    |

Some important observations:

* **Timing offers no escape.** It doesn't matter when the consumer runs their query — fragmentation is inevitable.
  * Filtering out the most recent "bleeding-edge" records won't save you either — at full capacity there's always a fresh cohort still being written, so you just shift the problem instead of solving it.
* **Transactions aren't a reliable fix.** One obvious idea is to have the tester equipment wrap all of a wafer's measurements in a single SQL transaction, but in the real world that isn't always possible.
* **A fixed-count filter won't work either.** Wafers vary in total chip count, so we can't simply filter the query to expose only wafers that have hit some fixed number of chips.

## The Solution

To untangle this, let's start at the source. In the bleeding-edge records below, notice that wafers 5 and 6 are **interleaved** — the testers are probing both at once, so their measurements arrive row-by-row, side by side. This is why simply dropping the latest wafer doesn't work: at any given moment, *several* wafers sit unfinished at the bleeding edge, not just the single most recent one. So the real task is to reliably tell a wafer that's fully written apart from one whose measurements are still streaming in.

```sql
select
  measurement_id, -- autoincrement identity
  wafer_id, -- corresponds to Figure 4 above
  pass,
  x,
  y,
  modified_at -- prod db insertion timestamp
from raw_measurements.measurement
order by measurement_id desc
limit 10
```

<p align="center">
    <picture>
        <source media="(prefers-color-scheme: dark)" srcset="plot/case-study/interleaved/interleaved-dark.svg">
        <img src="plot/case-study/interleaved/interleaved-light.svg" alt="The ten most recent measurement records, with rows for wafers 5 and 6 interleaved and color-coded by wafer.">
    </picture>
</p>
<p align="center"><sub><em>Figure 5: The ten most recent measurement records. Rows for wafers 5 and 6 alternate — the two are on the tester at once, so their measurements land in the Reporting DB interleaved, milliseconds apart.</em></sub></p>
