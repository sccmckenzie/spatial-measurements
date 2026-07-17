import duckdb

# Connect duckdb to postgres
con = 'host=localhost port=5432 dbname=postgres connect_timeout=10 user=duckdb_ro password=applesauce'
duckdb.sql(f"ATTACH '{con}' AS postgres_db (TYPE postgres)")

# batch load measurement records into parquet file
copy_statement = "copy (select * from postgres_db.raw.measurement limit 5) TO 'lineitem.parquet' (FORMAT parquet, APPEND true)"
duckdb.sql(copy_statement)