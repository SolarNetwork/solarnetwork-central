/**
 * Find source IDs matching a datum metadata search filter.
 *
 * Search filters are specified using LDAP filter syntax, e.g. <code>(/m/foo=bar)</code>.
 *
 * @param nodes				array of node IDs
 * @param criteria			the search filter
 *
 * @returns All matching source IDs.
 */
CREATE OR REPLACE FUNCTION solardatum.sources_for_meta(
    IN nodes bigint[],
    IN criteria text
  )
  RETURNS TABLE(node_id solarcommon.node_id, source_id solarcommon.source_id)
  LANGUAGE plv8 ROWS 100 STABLE AS
$BODY$
'use strict';

var objectPathMatcher = require('util/objectPathMatcher').default,
	searchFilter = require('util/searchFilter').default;

var filter = searchFilter(criteria),
	stmt,
	curs,
	rec,
	meta,
	matcher,
	resultRec = {};

if ( !filter.rootNode ) {
	plv8.elog(NOTICE, 'Malformed search filter:', criteria);
	return;
}

stmt = plv8.prepare('SELECT node_id, source_id, jdata FROM solardatum.da_meta WHERE node_id = ANY($1)', ['bigint[]']);
curs = stmt.cursor([nodes]);

while ( rec = curs.fetch() ) {
	meta = rec.jdata;
	matcher = objectPathMatcher(meta);
	if ( matcher.matchesFilter(filter) ) {
		resultRec.node_id = rec.node_id;
		resultRec.source_id = rec.source_id;
		plv8.return_next(resultRec);
	}
}

curs.close();
stmt.free();

$BODY$;

/**
 * Find source IDs matching a location metadata search filter.
 *
 * Search filters are specified using LDAP filter syntax, e.g. <code>(/m/foo=bar)</code>.
 *
 * @param locs				array of location IDs
 * @param criteria			the search filter
 *
 * @returns All matching source IDs.
 */
CREATE OR REPLACE FUNCTION solardatum.sources_for_loc_meta(
    IN locs bigint[],
    IN criteria text
  )
  RETURNS TABLE(loc_id solarcommon.loc_id ,source_id solarcommon.source_id)
  LANGUAGE plv8 ROWS 100 STABLE AS
$BODY$
'use strict';

var objectPathMatcher = require('util/objectPathMatcher').default,
	searchFilter = require('util/searchFilter').default;

var filter = searchFilter(criteria),
	stmt,
	curs,
	rec,
	meta,
	matcher,
	resultRec = {};

if ( !filter.rootNode ) {
	plv8.elog(NOTICE, 'Malformed search filter:', criteria);
	return;
}

stmt = plv8.prepare('SELECT loc_id, source_id, jdata FROM solardatum.da_loc_meta WHERE loc_id = ANY($1)', ['bigint[]']);
curs = stmt.cursor([locs]);

while ( rec = curs.fetch() ) {
	meta = rec.jdata;
	matcher = objectPathMatcher(meta);
	if ( matcher.matchesFilter(filter) ) {
		resultRec.loc_id = rec.loc_id;
		resultRec.source_id = rec.source_id;
		plv8.return_next(resultRec);
	}
}

curs.close();
stmt.free();

$BODY$;
