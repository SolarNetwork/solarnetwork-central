CREATE TABLE solardata.node_datum (
	node_id text,
	dtype int,
	source_id text,
	year int,
	ts timestamp,
	data_num map<text, decimal>,
	PRIMARY KEY ((node_id, dtype, source_id, year), ts)
);
CREATE TABLE solardata.loc_datum (
	loc_id text,
	ltype int,
	year int,
	ts timestamp,
	data_num map<text, decimal>,
	PRIMARY KEY ((loc_id, ltype, year), ts)
);
