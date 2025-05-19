create schema config;
grant select on all tables in schema config to scanner_rw;
alter default privileges in schema config grant select on tables to scanner_rw;