DROP INDEX IF EXISTS solardatum.da_datum_x_acc_idx;

CREATE INDEX da_datum_x_acc_idx ON solardatum.da_datum (node_id, source_id, ts DESC, jdata_a)
    WHERE jdata_a IS NOT NULL;
