create user scanner_ro with password 'applesauce';
grant usage on schema raw to scanner_ro;
grant select on table raw.measurement to scanner_ro;
