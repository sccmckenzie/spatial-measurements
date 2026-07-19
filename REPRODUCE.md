# Overcoming Fragmented Spatial Maps

> Before proceeding here, recommend reading through the [case study](README.md) to understand the full context.

## Setup

Prerequisites:

* JDK 24
* maven (I used 3.9.11 when writing this)
* Docker
* uv configured on system

With the prerequisites fulfilled, this repo contains everything you need to fully reproduce this case study end-to-end,
including data generation and batch loading.

The Spatial Measurement Simulator itself is a java app - src located in `scanner` dir. This app writes to a preconfigured
postgres instance at `jdbc:postgresql://localhost:5432/postgres`. Included makefile will provision this database in 
a docker container, using python to populate configuration tables, then build `scanner` jar from source.

```bash
# clone repo, cd into spatial-measurements
make
```
You should expect to see similar output as below.

```bash
Running docker compose
docker compose up -d
[+] up 1/1
 ✔ Container spatial-measurements-db-1 Running
Waiting for Postgres to be ready...
Postgres is ready.
Generating grid file and writing to postgres config db
uv run provision-grid.py
build scanner jar from source
[INFO] Scanning for projects...
[INFO] 
[INFO] ------------------------< com.example:scanner >-------------------------
[INFO] Building scanner 0.0.1-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- resources:3.3.1:resources (default-resources) @ scanner ---
[INFO] Copying 1 resource from src/main/resources to target/classes
[INFO] Copying 1 resource from src/main/resources to target/classes
#### abridged ####
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.879 s
[INFO] Finished at: 2026-07-18T18:37:31-05:00
[INFO] ------------------------------------------------------------------------
```

To change the size of the wafer maps, adjust the `radii` variable on line 4. Then, reapply configuration to db by running:

```bash
uv run provision-grid.py
```

`scanner` app can be configured in `application.yml` found in project root. Notable parameters include:

```yml
scanner:
  pool-size: 4 # how many scans can be generated concurrently
  count: 100 # total number of scans to generate during app runtime
  write-delay-ms-min: 1 # minimum pause (ms) inserted before each individual measurement write
  write-delay-ms-max: 1 # maximum pause (ms) inserted before each individual measurement write — each write sleeps a random duration in `[min, max]`, spacing out `modified_at` timestamps so concurrent scans interleave (set max to 0 to disable)
```

## Running the workflow

There are 2 dbt models:

* `measurement_unsafe` - the incremental model described in the [Draft Model](README.md#draft-model) section of the case study. Its incremental filter (`modified_at > max(modified_at)`) advances the watermark naively, so measurements from a still-settling scan can be permanently swept under the rug on subsequent runs — leaving incomplete wafer maps in the warehouse.
* `measurement_safe` - the corrected incremental model from the [Refined Model](README.md#refined-model) section. It over-extracts from the source by a time margin (larger than the maximum known scan duration) so late-arriving measurements are re-captured rather than skipped, guaranteeing each wafer map ends up complete.

Please note the settle & over-extraction parameters have been fine-tuned for the `application.yml` parameters as version-controlled.

The dbt models are meant to be executed *while* the scanner app is running. For convenience, I have preprovisioned a script that orchestrates this for you.

```bash
source scan-and-load.sh
```

You can run below query during & after execution has finished to see the difference between the raw data, the unsafe naive model, and the safe refined model with overextraction.

Connect using the read-only `scanner_ro` role:

```
jdbc:postgresql://localhost:5432/postgres?user=scanner_ro&password=applesauce
```

```sql
with a as(
    select
        wafer_id,
        count(1) as cnt_raw
    from
        raw.measurement_stretched
    group by wafer_id
), b as (
    select
        wafer_id,
        count(1) as cnt_safe
    from
        reporting.measurement_safe
    group by wafer_id
), c as (
    select
        wafer_id,
        count(1) as cnt_unsafe
    from
        reporting.measurement_unsafe
    group by wafer_id
)
select
    a.wafer_id,
    a.cnt_raw,
    b.cnt_safe,
    c.cnt_unsafe
from
    a
left join b
    on a.wafer_id = b.wafer_id
left join c
    on a.wafer_id = c.wafer_id
-- where
--     (cnt_raw != cnt_safe)
--     or (cnt_raw != cnt_unsafe)
--     or cnt_safe is null
--     or cnt_unsafe is null
order by a.wafer_id
```