-- View over raw.measurement that stretches the timeline:
-- each millisecond of real elapsed time becomes 5 seconds  (5000x) relative
-- to the earliest modified_at, so a tightly-packed scan spreads out over a
-- much longer synthetic timeline.
create view raw.measurement_stretched as
select
    m.measurement_id,
    m.measurement_value,
    m.scan_id as wafer_id,
    m.x,
    m.y,
    m.modified_at as modified_at_raw,
    min(m.modified_at) over ()
        + (m.modified_at - min(m.modified_at) over ()) * 5000 as modified_at
from raw.measurement m;

grant select on raw.measurement_stretched to scanner_ro;
