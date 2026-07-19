truncate table raw.measurement;
SELECT setval('raw.measurement_measurement_id_seq', 1, false);
SELECT setval('raw.scan_id_sequence', 1, false);
drop table reporting.measurement_safe;
drop table reporting.measurement_unsafe;

SELECT
    pid,
    usename,
    application_name,
    client_addr,
    backend_start,
    xact_start,
    now() - xact_start AS transaction_duration,
    state,
    query
FROM pg_stat_activity
where application_name = 'PostgreSQL JDBC Driver';

SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_table_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_table_size(schemaname||'.'||tablename)) as index_size
FROM pg_tables
WHERE schemaname NOT IN ('information_schema', 'pg_catalog')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

SHOW data_directory;

SELECT
    d.oid as database_oid,
    d.datname as database_name,
    t.relfilenode,
    t.relname as table_name
FROM pg_class t
         JOIN pg_database d ON d.datname = current_database()
WHERE t.relname = 'measurement';


select
    wafer_id,
    min(modified_at)::time,
    max(modified_at)::time,
    extract(epoch from max(modified_at) - min(modified_at))::integer * 1.0 / 60 as write_duration
from
    raw.measurement_stretched
group by wafer_id
order by wafer_id;

-- finds the largest successive gap in modified_at between records within a specific scan_id
with ordered as (
    select
        wafer_id,
        modified_at,
        lag(modified_at) over (partition by wafer_id order by modified_at) as prev_modified_at
    from raw.measurement_stretched
)
select distinct on (wafer_id)
    wafer_id,
    prev_modified_at as gap_start,
    modified_at      as gap_end,
    extract(epoch from modified_at - prev_modified_at)::int as gap_seconds
from ordered
where prev_modified_at is not null
order by wafer_id, gap_seconds desc;

