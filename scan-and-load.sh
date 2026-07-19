#!/usr/bin/env bash
set -euo pipefail

# Run relative to this script's location, not the caller's working directory,
# so it behaves the same no matter where it's invoked from.
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Invoke dbt without changing our working directory: point it at the project
# (and its co-located profiles.yml) explicitly instead of cd-ing into dbt/.
run_dbt() {
  dbt run --project-dir "$script_dir/dbt" --profiles-dir "$script_dir/dbt"
}

# Start the scanner in the background.
java -jar "$script_dir/scanner/target/scanner-0.0.1-SNAPSHOT.jar" &
java_pid=$!

# If we exit for any reason (error, Ctrl-C), don't leave the scanner orphaned.
cleanup() {
  kill "$java_pid" 2>/dev/null || true
}
trap cleanup EXIT

# Wait until the scanner has written at least one row to raw.measurement.
# If dbt executes too early, the incremental model will get stuck at 0 records.
until [ "$(docker exec -e PGPASSWORD=applesauce spatial-measurements-db-1 psql -U scanner_ro -d postgres -tAc 'SELECT count(*) FROM raw.measurement' 2>/dev/null)" -gt 0 ] 2>/dev/null; do
  sleep 1
done

# Loop dbt run while the scanner is still alive.
while kill -0 "$java_pid" 2>/dev/null; do
  run_dbt
done

# Reap the scanner and propagate its exit status.
wait "$java_pid"

# One last dbt run to pick up whatever landed after the final loop iteration.
run_dbt
