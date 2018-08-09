INSERT INTO solaragg.agg_datum_monthly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-01-01 00:00:00.000Z', '2017-01-01 00:00:00.000', -100, 'test.source', '{"foo":101}'::jsonb);
INSERT INTO solaragg.agg_datum_monthly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-02-01 00:00:00.000Z', '2017-01-01 00:00:00.000', -100, 'test.source', '{"foo":102}'::jsonb);
INSERT INTO solaragg.agg_datum_monthly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-03-01 00:00:00.000Z', '2017-01-01 00:00:00.000', -100, 'test.source', '{"foo":103}'::jsonb);

INSERT INTO solaragg.agg_datum_daily (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-01 00:00:00.000Z', '2017-01-01 00:00:00.000', -100, 'test.source', '{"foo":104}'::jsonb);
INSERT INTO solaragg.agg_datum_daily (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-02 00:00:00.000Z', '2017-01-01 00:00:00.000', -100, 'test.source', '{"foo":105}'::jsonb);
INSERT INTO solaragg.agg_datum_daily (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-03 00:00:00.000Z', '2017-01-01 00:00:00.000', -100, 'test.source', '{"foo":106}'::jsonb);

INSERT INTO solaragg.agg_datum_hourly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 00:00:00.000Z', '2017-01-01 00:00:00.000', -100, 'test.source', '{"foo":107}'::jsonb);
INSERT INTO solaragg.agg_datum_hourly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 01:00:00.000Z', '2017-01-01 00:00:00.000', -100, 'test.source', '{"foo":108}'::jsonb);
INSERT INTO solaragg.agg_datum_hourly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 02:00:00.000Z', '2017-01-01 00:00:00.000', -100, 'test.source', '{"foo":109}'::jsonb);

INSERT INTO solardatum.da_datum (ts, posted, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 03:01:00.000Z', CURRENT_TIMESTAMP, -100, 'test.source', '{"foo":110}'::jsonb);
INSERT INTO solardatum.da_datum (ts, posted, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 03:02:00.000Z', CURRENT_TIMESTAMP, -100, 'test.source', '{"foo":111}'::jsonb);
INSERT INTO solardatum.da_datum (ts, posted, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 03:03:00.000Z', CURRENT_TIMESTAMP, -100, 'test.source', '{"foo":112}'::jsonb);
