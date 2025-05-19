create user scanner_rw with password 'brownies';
create schema raw_measurements;
create schema raw_measurements_test;
grant all privileges on schema raw_measurements to scanner_rw;
grant all privileges on schema raw_measurements_test to scanner_rw;
alter default privileges in schema raw_measurements grant all privileges on tables to scanner_rw;
alter default privileges in schema raw_measurements_test grant all privileges on tables to scanner_rw;
