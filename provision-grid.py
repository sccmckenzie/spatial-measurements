import polars as pl

scale = 10

v = pl.Series("v", range(-scale, scale))

lf0 = pl.LazyFrame({"x": v})

out = (lf0
       .join(lf0.rename({"x": "y"}), how="cross")
       .filter(pl.col("x").pow(2) + pl.col("y").pow(2) <= scale**2)
       .with_row_index(name="grid_id", offset=1)
       .collect())

out.write_csv("dbt_spatial_config/seeds/grid.csv")