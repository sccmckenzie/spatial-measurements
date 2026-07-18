{{
  config(
    materialized = 'incremental',
    unique_key = 'measurement_id'
  )
}}

with source as (
    select
        measurement_id,
        measurement_value,
        wafer_id,
        x,
        y,
        modified_at
    from raw.measurement_stretched
    {% if is_incremental() %}
      -- simple incrementalization - creates risk of sweeping records under the rug
      where modified_at > (select max(modified_at) from {{ this }})
    {% endif %}
),

safe_scans as (
    select wafer_id
    from source
    group by wafer_id
    -- a scan is "safe" once its latest measurement is more than 1000 seconds old
    -- behind the latest modified_at anywhere in the source (i.e. it has settled)
    having max(modified_at) < (select max(modified_at) from source) - interval '1000 seconds'
)

select source.*
from source
where wafer_id in (select wafer_id from safe_scans)

