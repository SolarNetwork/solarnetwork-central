/* ==================================================================
 * DatumSqlUtils.java - 17/11/2020 12:19:21 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.LocationMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.NodeMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.StreamMetadataCriteria;
import net.solarnetwork.domain.SortDescriptor;

/**
 * SQL utilities for datum.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public final class DatumSqlUtils {

	private DatumSqlUtils() {
		// don't construct me
	}

	/**
	 * A standard mapping of sort keys to SQL column names suitable for ordering
	 * by stream metadata.
	 * 
	 * <p>
	 * This map contains the following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>loc -&gt; obj_id</li>
	 * <li>node -&gt; obj_id</li>
	 * <li>obj -&gt; obj_id</li>
	 * <li>source -&gt; source_id</li>
	 * <li>stream -&gt; stream_id</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> STREAM_METADATA_SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.put("loc", "obj_id");
		map.put("node", "obj_id");
		map.put("obj", "obj_id");
		map.put("source", "source_id");
		map.put("stream", "stream_id");
		STREAM_METADATA_SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	/**
	 * A standard mapping of sort keys to SQL column names suitable for ordering
	 * by datum stream metadata.
	 * 
	 * <p>
	 * This map contains the following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>node -&gt; node_id</li>
	 * <li>source -&gt; source_id</li>
	 * <li>stream -&gt; stream_id</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> NODE_STREAM_METADATA_SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.put("node", "node_id");
		map.put("source", "source_id");
		map.put("stream", "stream_id");
		NODE_STREAM_METADATA_SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	/**
	 * A standard mapping of sort keys to SQL column names suitable for ordering
	 * by datum stream columns.
	 * 
	 * <p>
	 * This map contains the entries from
	 * {@link #NODE_STREAM_METADATA_SORT_KEY_MAPPING} and following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>created -&gt; ts</li>
	 * <li>time -&gt; ts</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> NODE_STREAM_SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(5);
		map.putAll(NODE_STREAM_METADATA_SORT_KEY_MAPPING);
		map.put("created", "ts");
		map.put("time", "ts");
		NODE_STREAM_SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	/**
	 * A standard mapping of sort keys to SQL column names suitable for ordering
	 * by aggregate datum stream columns.
	 * 
	 * <p>
	 * This map contains the entries from
	 * {@link #NODE_STREAM_METADATA_SORT_KEY_MAPPING} and following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>created -&gt; ts_start</li>
	 * <li>time -&gt; ts_start</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> AGG_NODE_STREAM_SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.putAll(NODE_STREAM_METADATA_SORT_KEY_MAPPING);
		map.put("created", "ts_start");
		map.put("time", "ts_start");
		AGG_NODE_STREAM_SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	/**
	 * A standard mapping of sort keys to SQL column names suitable for ordering
	 * by location datum stream metadata.
	 * 
	 * <p>
	 * This map contains the following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>node -&gt; node_id</li>
	 * <li>source -&gt; source_id</li>
	 * <li>stream -&gt; stream_id</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> LOCATION_STREAM_METADATA_SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.put("loc", "loc_id");
		map.put("source", "source_id");
		map.put("stream", "stream_id");
		LOCATION_STREAM_METADATA_SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	/**
	 * A standard mapping of sort keys to SQL column names suitable for ordering
	 * by location datum stream columns.
	 * 
	 * <p>
	 * This map contains the entries from
	 * {@link #LOCATION_STREAM_METADATA_SORT_KEY_MAPPING} and following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>created -&gt; ts</li>
	 * <li>time -&gt; ts</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> LOCATION_STREAM_SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(5);
		map.putAll(LOCATION_STREAM_METADATA_SORT_KEY_MAPPING);
		map.put("created", "ts");
		map.put("time", "ts");
		LOCATION_STREAM_SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	/**
	 * A standard mapping of sort keys to SQL column names suitable for ordering
	 * by aggregate datum stream columns.
	 * 
	 * <p>
	 * This map contains the entries from
	 * {@link #LOCATION_STREAM_METADATA_SORT_KEY_MAPPING} and following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>created -&gt; ts_start</li>
	 * <li>time -&gt; ts_start</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> AGG_LOCATION_STREAM_SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.putAll(LOCATION_STREAM_METADATA_SORT_KEY_MAPPING);
		map.put("created", "ts_start");
		map.put("time", "ts_start");
		AGG_LOCATION_STREAM_SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	/**
	 * Generate SQL {@literal ORDER BY} criteria for a set of
	 * {@link SortDescriptor}.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal , key} for each key.
	 * The leading comma and space characters are <b>not</b> stripped, but the
	 * returned value indicates the number of characters to trim from the
	 * results if needed.
	 * </p>
	 * 
	 * @param sorts
	 *        the sorts
	 * @param sortKeyMapping
	 *        the mapping of sort keys to SQL sort names
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of leading "joining" characters added to {@code buf};
	 *         will either be {@literal 0} or {@literal 2}
	 */
	public static int orderBySorts(Iterable<SortDescriptor> sorts, Map<String, String> sortKeyMapping,
			StringBuilder buf) {
		if ( sorts == null || sortKeyMapping == null || sortKeyMapping.isEmpty() ) {
			return 0;
		}
		for ( SortDescriptor sort : sorts ) {
			String sqlName = sortKeyMapping.get(sort.getSortKey());
			if ( sqlName != null ) {
				buf.append(", ").append(sqlName);
				if ( sort.isDescending() ) {
					buf.append(" DESC");
				}
			}
		}
		return 2;
	}

	public static int whereStreamMetadata(StreamMetadataCriteria filter, StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getSourceIds() != null ) {
			buf.append("\tAND meta.source_id = ANY(?)\n");
			paramCount += 1;
		}
		if ( filter.getStreamIds() != null ) {
			buf.append("\tAND meta.stream_id = ANY(?)\n");
			paramCount += 1;
		}
		if ( filter.getUserIds() != null ) {
			buf.append("\tAND un.user_id = ANY(?)\n");
			paramCount += 1;
		}
		return paramCount;
	}

	/**
	 * Generate SQL {@literal WHERE} criteria to find node metadata.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal \tAND c = ?\n} for
	 * each clause. The leading tab and {@literal AND} and space characters are
	 * <b>not</b> stripped.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static int whereNodeMetadata(NodeMetadataCriteria filter, StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getNodeIds() != null ) {
			buf.append("\tAND meta.node_id = ANY(?)\n");
			paramCount += 1;
		}
		paramCount += whereStreamMetadata(filter, buf);
		return paramCount;
	}

	/**
	 * Generate SQL {@literal WHERE} criteria to find location metadata.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal \tAND c = ?\n} for
	 * each clause. The leading tab and {@literal AND} and space characters are
	 * <b>not</b> stripped.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static int whereLocationMetadata(LocationMetadataCriteria filter, StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getLocationIds() != null ) {
			buf.append("\tAND meta.loc_id = ANY(?)\n");
			paramCount += 1;
		}
		paramCount += whereStreamMetadata(filter, buf);
		return paramCount;
	}

	/**
	 * Generate SQL query to find node metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #whereNodeMetadata(NodeMetadataCriteria, StringBuilder)
	 * @see #nodeMetadataFilterPrepare(NodeMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int nodeMetadataFilterSql(NodeMetadataCriteria filter, StringBuilder buf) {
		buf.append("SELECT meta.stream_id, meta.node_id, meta.source_id, meta.names_i, meta.names_a\n");
		buf.append(
				"\t, meta.names_s, meta.jdata, 'n'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone\n");
		buf.append("FROM solardatm.da_datm_meta meta\n");
		if ( filter.getUserIds() != null ) {
			buf.append("INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id\n");
		}
		buf.append("LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id\n");
		buf.append("LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id\n");
		int paramCount = 0;
		if ( filter != null ) {
			StringBuilder where = new StringBuilder();
			paramCount += whereNodeMetadata(filter, where);
			if ( where.length() > 0 ) {
				buf.append("WHERE ");
				buf.append(where.substring(5));
			}
		}
		return paramCount;
	}

	/**
	 * Generate SQL query to find location metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #whereLocationMetadata(LocationMetadataCriteria, StringBuilder)
	 * @see #locationMetadataFilterPrepare(LocationMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int locationMetadataFilterSql(LocationMetadataCriteria filter, StringBuilder buf) {
		buf.append("SELECT meta.stream_id, meta.loc_id, meta.source_id, meta.names_i, meta.names_a\n");
		buf.append(
				"\t, meta.names_s, meta.jdata, 'l'::CHARACTER AS kind, COALESCE(l.time_zone, 'UTC') AS time_zone\n");
		buf.append("FROM solardatm.da_loc_datm_meta meta\n");
		if ( filter.getUserIds() != null ) {
			buf.append("INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id\n");
		}
		buf.append("LEFT OUTER JOIN solarnet.sn_loc l ON l.id = meta.loc_id\n");
		int paramCount = 0;
		if ( filter != null ) {
			StringBuilder where = new StringBuilder();
			paramCount += whereLocationMetadata(filter, where);
			if ( where.length() > 0 ) {
				buf.append("WHERE ");
				buf.append(where.substring(5));
			}
		}
		return paramCount;
	}

	/**
	 * Prepare a SQL query to find stream metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static int streamMetadataFilterPrepare(StreamMetadataCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null ) {
			if ( filter.getSourceIds() != null ) {
				Array array = con.createArrayOf("text", filter.getSourceIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			}
			if ( filter.getStreamIds() != null ) {
				Array array = con.createArrayOf("uuid", filter.getStreamIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			}
			if ( filter.getUserIds() != null ) {
				Array array = con.createArrayOf("bigint", filter.getUserIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			}
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL query to find node metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @see #nodeMetadataFilterSql(NodeMetadataCriteria, StringBuilder)
	 * @see #streamMetadataFilterPrepare(StreamMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int nodeMetadataFilterPrepare(NodeMetadataCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null ) {
			if ( filter.getNodeIds() != null ) {
				Array array = con.createArrayOf("bigint", filter.getNodeIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			}
			parameterOffset = streamMetadataFilterPrepare(filter, con, stmt, parameterOffset);
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL query to find location metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @see #locationMetadataFilterSql(NodeMetadataCriteria, StringBuilder)
	 * @see #streamMetadataFilterPrepare(StreamMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int locationMetadataFilterPrepare(LocationMetadataCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null ) {
			if ( filter.getLocationIds() != null ) {
				Array array = con.createArrayOf("bigint", filter.getLocationIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			}
			parameterOffset = streamMetadataFilterPrepare(filter, con, stmt, parameterOffset);
		}
		return parameterOffset;
	}

	/**
	 * Wrap a SQL query with a {@literal SELECT COUNT(*)} clause.
	 * 
	 * @param sql
	 *        the SQL query to wrap
	 * @return the wrapped query
	 */
	public static String wrappedCountQuery(String sql) {
		return "SELECT COUNT(*) FROM (" + sql + ") AS q";
	}

	/**
	 * Execute a query for a count result.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param creator
	 *        the statement creator; if implements
	 *        {@link CountPreparedStatementCreatorProvider} then
	 *        {@link CountPreparedStatementCreatorProvider#countPreparedStatementCreator()}
	 *        will be used
	 * @return the result, or {@literal null} if no result count is available
	 */
	public static Long executeCountQuery(JdbcOperations jdbcTemplate, PreparedStatementCreator creator) {
		return jdbcTemplate.query(creator, new ResultSetExtractor<Long>() {

			@Override
			public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
				return rs.next() ? rs.getLong(1) : null;
			}
		});
	}

}
