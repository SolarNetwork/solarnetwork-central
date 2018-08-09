INSERT INTO solaragg.agg_datum_monthly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-01-01 00:00:00.000Z', '2017-01-01 00:00:00.000', -101, 'test.source', '{"foo":1010}'::jsonb);
INSERT INTO solaragg.agg_datum_monthly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-02-01 00:00:00.000Z', '2017-01-01 00:00:00.000', -101, 'test.source', '{"foo":1020}'::jsonb);
INSERT INTO solaragg.agg_datum_monthly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-03-01 00:00:00.000Z', '2017-01-01 00:00:00.000', -101, 'test.source', '{"foo":1030}'::jsonb);

INSERT INTO solaragg.agg_datum_daily (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-01 00:00:00.000Z', '2017-01-01 00:00:00.000', -101, 'test.source', '{"foo":1040}'::jsonb);
INSERT INTO solaragg.agg_datum_daily (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-02 00:00:00.000Z', '2017-01-01 00:00:00.000', -101, 'test.source', '{"foo":1050}'::jsonb);
INSERT INTO solaragg.agg_datum_daily (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-03 00:00:00.000Z', '2017-01-01 00:00:00.000', -101, 'test.source', '{"foo":1060}'::jsonb);

INSERT INTO solaragg.agg_datum_hourly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 00:00:00.000Z', '2017-01-01 00:00:00.000', -101, 'test.source', '{"foo":1070}'::jsonb);
INSERT INTO solaragg.agg_datum_hourly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 01:00:00.000Z', '2017-01-01 00:00:00.000', -101, 'test.source', '{"foo":1080}'::jsonb);
INSERT INTO solaragg.agg_datum_hourly (ts_start, local_date, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 02:00:00.000Z', '2017-01-01 00:00:00.000', -101, 'test.source', '{"foo":1090}'::jsonb);

INSERT INTO solardatum.da_datum (ts, posted, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 03:01:00.000Z', CURRENT_TIMESTAMP, -101, 'test.source', '{"foo":1100}'::jsonb);
INSERT INTO solardatum.da_datum (ts, posted, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 03:02:00.000Z', CURRENT_TIMESTAMP, -101, 'test.source', '{"foo":1110}'::jsonb);
INSERT INTO solardatum.da_datum (ts, posted, node_id, source_id, jdata_a)
	VALUES ('2017-04-04 03:03:00.000Z', CURRENT_TIMESTAMP, -101, 'test.source', '{"foo":1120}'::jsonb);
