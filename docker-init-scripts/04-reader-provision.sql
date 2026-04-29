create user scanner_ro with password 'applesauce';
grant usage on schema raw_measurements to scanner_ro;
grant select on table raw_measurements.measurement to scanner_ro;
