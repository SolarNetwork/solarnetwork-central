/* ==================================================================
 * DatumJdbcTestUtils.java - 28/03/2026 3:37:28 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.stream.Collectors.joining;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Datum JDBC utilities for tests.
 *
 * @author matt
 * @version 1.0
 */
public final class DatumJdbcTestUtils {

	private static final Logger log = LoggerFactory.getLogger(DatumJdbcTestUtils.class);

	private DatumJdbcTestUtils() {
		// not available
	}

	/**
	 * List object datum stream alias rows.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 * @return the rows
	 */
	public static List<Map<String, Object>> allObjectDatumStreamAliasData(JdbcOperations jdbcOps) {
		List<Map<String, Object>> data = jdbcOps.queryForList("""
				SELECT stream_id::text, created, modified
					, node_id, source_id, alias_node_id, alias_source_id
				FROM solardatm.da_datm_alias
				ORDER BY node_id, source_id, alias_node_id, alias_source_id
				""");
		log.debug("solardatm.da_datm_alias table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(joining("\n\t", "\n\t", "\n")));
		return data;
	}

}
