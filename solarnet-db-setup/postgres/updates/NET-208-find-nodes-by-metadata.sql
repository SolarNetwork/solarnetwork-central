/**
 * Find node IDs matching a metadata search filter.
 *
 * Search filters are specified using LDAP filter syntax, e.g. <code>(/m/foo=bar)</code>.
 *
 * @param nodes				array of node IDs
 * @param criteria			the search filter
 *
 * @returns All matching node IDs.
 */
CREATE OR REPLACE FUNCTION solarnet.find_nodes_for_meta(nodes bigint[], criteria text)
  RETURNS TABLE(node_id bigint)
  LANGUAGE plv8 ROWS 100 STABLE AS
$$
	'use strict';

	var objectPathMatcher = require('util/objectPathMatcher').default,
		searchFilter = require('util/searchFilter').default;

	var filter = searchFilter(criteria),
		stmt,
		curs,
		rec,
		meta,
		matcher;

	if ( !filter.rootNode ) {
		plv8.elog(NOTICE, 'Malformed search filter:', criteria);
		return;
	}

	stmt = plv8.prepare('SELECT node_id, jdata FROM solarnet.sn_node_meta WHERE node_id = ANY($1)', ['bigint[]']);
	curs = stmt.cursor([nodes]);

	while ( rec = curs.fetch() ) {
		meta = rec.jdata;
		matcher = objectPathMatcher(meta);
		if ( matcher.matchesFilter(filter) ) {
			plv8.return_next(rec.node_id);
		}
	}

	curs.close();
	stmt.free();
$$;
