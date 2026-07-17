create user scanner_rw with password 'brownies';
create schema raw;
create sequence raw.scan_id_sequence START WITH 1 INCREMENT BY 1;
grant usage on sequence raw.scan_id_sequence to scanner_rw;
grant all privileges on schema raw to scanner_rw;
alter default privileges in schema raw grant all privileges on tables to scanner_rw;
