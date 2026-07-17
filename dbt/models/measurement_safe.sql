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
        scan_id,
        x,
        y,
        modified_at
    from raw.measurement_stretched
    {% if is_incremental() %}
      -- only pull rows newer than what we've already landed
      where modified_at > (select max(modified_at) from {{ this }})
    {% endif %}
),

-- a scan is "safe" once its latest measurement is more than 120 seconds old
-- behind the latest modified_at anywhere in the source (i.e. it has settled)
safe_scans as (
    select scan_id
    from source
    group by scan_id
    having max(modified_at) < (select max(modified_at) from source) - interval '120 seconds'
)

select source.*
from source
where scan_id in (select scan_id from safe_scans)

