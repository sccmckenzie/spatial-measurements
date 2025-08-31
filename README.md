# Spatial Measurement Simulator + Integration Case Study

This is a companion repository to my blog article: *Addressing Fragmented Data Integration associated with Spatial Measurements*.

Prerequisites:

* JDK 24
* maven (I used 3.9.11 when writing this)
* Docker
* poetry configured on system

With the prerequisites fulfilled, this repo contains everything you need to fully reproduce this case study end-to-end,
including data generation, batch loading, and cooldown.

The Spatial Measurement Simulator itself is a java app - src located in `scanner` dir. This app expects preconfigured
postgres instance at `jdbc:postgresql://localhost:5432/postgres`. Included makefile will provision this database in 
a docker container, using dbt (python) to populate configuration tables, then build `scanner` jar from source.

```bash
# clone repo, cd into spatial-measurements
make
```
You should expect to see similar output as below.

```bash
Running docker compose
docker compose up -d
[+] Running 2/2
 ✔ Network spatial-measurements_default  Created                                                                                                                                                                                                                                         0.0s 
 ✔ Container spatial-measurements-db-1   Started                                                                                                                                                                                                                                         0.2s 
Waiting for Postgres to be ready...
Postgres is ready.
Generating grid file
writing grid file to postgres config db
01:04:29  Running with dbt=1.9.4
01:04:29  Registered adapter: postgres=1.9.0
01:04:30  Found 1 seed, 433 macros
01:04:30  
01:04:30  Concurrency: 1 threads (target='dev')
01:04:30  
01:04:30  1 of 1 START seed file config.grid ............................................. [RUN]
01:04:30  1 of 1 OK loaded seed file config.grid ......................................... [INSERT 315 in 0.12s]
01:04:30  
01:04:30  Finished running 1 seed in 0 hours 0 minutes and 0.22 seconds (0.22s).
01:04:30  
01:04:30  Completed successfully
01:04:30  
01:04:30  Done. PASS=1 WARN=0 ERROR=0 SKIP=0 TOTAL=1
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
[INFO] 
[INFO] --- compiler:3.13.0:compile (default-compile) @ scanner ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] 
[INFO] --- resources:3.3.1:testResources (default-testResources) @ scanner ---
[INFO] skip non existing resourceDirectory /Users/scottmckenzie/repo/spatial-measurements/scanner/src/test/resources
[INFO] 
[INFO] --- compiler:3.13.0:testCompile (default-testCompile) @ scanner ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] 
[INFO] --- surefire:3.5.3:test (default-test) @ scanner ---
[INFO] Tests are skipped.
[INFO] 
[INFO] --- jar:3.4.2:jar (default-jar) @ scanner ---
[INFO] Building jar: /Users/scottmckenzie/repo/spatial-measurements/scanner/target/scanner-0.0.1-SNAPSHOT.jar
[INFO] 
[INFO] --- spring-boot:3.4.5:repackage (repackage) @ scanner ---
[INFO] Replacing main artifact /Users/scottmckenzie/repo/spatial-measurements/scanner/target/scanner-0.0.1-SNAPSHOT.jar with repackaged archive, adding nested dependencies in BOOT-INF/.
[INFO] The original artifact has been renamed to /Users/scottmckenzie/repo/spatial-measurements/scanner/target/scanner-0.0.1-SNAPSHOT.jar.original
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  0.882 s
[INFO] Finished at: 2025-08-30T19:39:04-05:00
[INFO] ------------------------------------------------------------------------
```

You can adjust the grid size by changing `scale` (default `10`) in `provision-grid.py`. To apply changes:

```bash
poetry run dbt seed --profile spatial_config --project-dir dbt_spatial_config
```

`scanner` app can be configured in `application.yml` found in project root. Notable parameters include:

* `scan-pool-size`: how many scans can be generated concurrently
* `scan-count`: total number of scans to generate during app runtime

To launch app, run:

```bash
java -jar scanner/target/scanner-0.0.1-SNAPSHOT.jar
```
