-- Dedicated user for dbt: reads from raw, owns its own reporting schema.
create user dbt_rw with password 'compost';

-- read access to the raw source schema
grant usage on schema raw to dbt_rw;
grant select on all tables in schema raw to dbt_rw;
alter default privileges in schema raw grant select on tables to dbt_rw;

-- dbt materializes models here; owning the schema lets it create/drop freely
create schema reporting authorization dbt_rw;