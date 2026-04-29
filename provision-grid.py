import polars as pl
from sqlalchemy import create_engine, text

radii = range(10, 20)

frames = []
for r in radii:
    v = pl.Series("v", range(-r, r + 1))
    lf0 = pl.LazyFrame({"x": v})
    grid_r = (lf0
              .join(lf0.rename({"x": "y"}), how="cross")
              .filter(pl.col("x").pow(2) + pl.col("y").pow(2) <= r**2)
              .filter(~(
                  ((pl.col("x") == 0) & (pl.col("y").abs() == r))
                  | ((pl.col("y") == 0) & (pl.col("x").abs() == r))
              ))
              .with_columns(template_id=r)
              .collect())
    frames.append(grid_r)

out = (pl.concat(frames, how="vertical")
       .with_row_index(name="id", offset=1)
       .select("id", "template_id", "x", "y")
       )
out.write_csv("grid.csv")

engine = create_engine("postgresql+psycopg2://postgres:gradient@localhost:5432/postgres")
with engine.begin() as conn:
    conn.execute(text("create schema if not exists config"))
    conn.execute(text("drop table if exists config.grid"))

out.write_database(
    table_name="config.grid",
    connection=engine,
    if_table_exists="replace",
)
