create schema config;
grant usage on schema config to scanner_rw;
grant select on all tables in schema config to scanner_rw;
alter default privileges in schema config grant select on tables to scanner_rw;