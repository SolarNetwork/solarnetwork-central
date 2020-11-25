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

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.LocationMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.NodeMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.StreamCriteria;
import net.solarnetwork.central.datum.v2.dao.StreamMetadataCriteria;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.LocalDateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.domain.ByteOrdering;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.util.ByteUtils;

/**
 * SQL utilities for datum.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public final class DatumSqlUtils {

	private static final Logger log = LoggerFactory.getLogger(DatumSqlUtils.class);

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
	 * <li>kind -&gt; agg_kind</li>
	 * <li>stream -&gt; stream_id</li>
	 * <li>time -&gt; ts_start</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> STALE_AGGREGATE_SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.put("kind", "agg_kind");
		map.put("stream", "stream_id");
		map.put("time", "ts_start");
		STALE_AGGREGATE_SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
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
	 * by datum stream metadata.
	 * 
	 * <p>
	 * This map contains the following entries:
	 * </p>
	 * 
	 * <ol>
	 * <li>created -&gt; aud_ts</li>
	 * <li>node -&gt; aud_node_id</li>
	 * <li>source -&gt; aud_source_id</li>
	 * <li>time -&gt; aud_ts</li>
	 * </ol>
	 * 
	 * @see #orderBySorts(Iterable, Map, StringBuilder)
	 */
	public static final Map<String, String> AUDIT_DATUM_SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.put("created", "aud_ts");
		map.put("node", "aud_node_id");
		map.put("source", "aud_source_id");
		map.put("time", "aud_ts");
		AUDIT_DATUM_SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
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
		boolean appended = false;
		for ( SortDescriptor sort : sorts ) {
			String sqlName = sortKeyMapping.get(sort.getSortKey());
			if ( sqlName != null ) {
				appended = true;
				buf.append(", ").append(sqlName);
				if ( sort.isDescending() ) {
					buf.append(" DESC");
				}
			}
		}
		return (appended ? 2 : 0);
	}

	/**
	 * Generate SQL {@literal WHERE} criteria to find stream metadata.
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
	 * Generate SQL {@literal WHERE} criteria to find location metadata.
	 * 
	 * <p>
	 * The filter is assumed to be for location metadata if a location ID is
	 * available; otherwise node metadata is assumed.
	 * </p>
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
	public static int whereDatumMetadata(DatumStreamCriteria filter, StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getLocationId() != null ) {
			buf.append("\tAND meta.loc_id = ANY(?)\n");
			paramCount += 1;
		} else if ( filter.getNodeId() != null ) {
			buf.append("\tAND meta.node_id = ANY(?)\n");
			paramCount += 1;
		}
		paramCount += whereStreamMetadata(filter, buf);
		return paramCount;
	}

	/**
	 * A metadata select style, to optimize queries.
	 * 
	 * <p>
	 * TODO
	 * </p>
	 * 
	 * @author matt
	 * @version 1.0
	 */
	public enum MetadataSelectStyle {
		/**
		 * Query for the minimum metadata.
		 * 
		 * <p>
		 * This will output the following columns:
		 * </p>
		 * 
		 * <ol>
		 * <li>stream_id</li>
		 * <li>obj_id</li>
		 * <li>source_id</li>
		 * </ol>
		 */
		Minimum,

		/**
		 * Query for the minimum metadata.
		 * 
		 * <p>
		 * This will output the following columns:
		 * </p>
		 * 
		 * <ol>
		 * <li>stream_id</li>
		 * <li>obj_id</li>
		 * <li>source_id</li>
		 * <li>time_zone</li>
		 * </ol>
		 */
		WithZone,

		/**
		 * Query for the full metadata including time zone.
		 * 
		 * <p>
		 * This will output the following columns:
		 * </p>
		 * 
		 * <ol>
		 * <li>stream_id</li>
		 * <li>obj_id</li>
		 * <li>source_id</li>
		 * <li>names_i</li>
		 * <li>names_a</li>
		 * <li>names_s</li>
		 * <li>jdata</li>
		 * <li>kind</li>
		 * <li>time_zone</li>
		 * </ol>
		 */
		Full;
	}

	/**
	 * Generate SQL query to find full node metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #nodeMetadataFilterSql(NodeMetadataCriteria, MetadataSelectStyle,
	 *      StringBuilder)
	 */
	public static int nodeMetadataFilterSql(NodeMetadataCriteria filter, StringBuilder buf) {
		return nodeMetadataFilterSql(filter, MetadataSelectStyle.Full, buf);
	}

	/**
	 * Generate SQL query to find node metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param style
	 *        the select style
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #whereNodeMetadata(NodeMetadataCriteria, StringBuilder)
	 * @see #prepareNodeMetadataFilter(NodeMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int nodeMetadataFilterSql(NodeMetadataCriteria filter, MetadataSelectStyle style,
			StringBuilder buf) {
		buf.append("SELECT meta.stream_id, meta.node_id, meta.source_id");
		if ( style == MetadataSelectStyle.Full ) {
			buf.append(", meta.names_i, meta.names_a, meta.names_s, meta.jdata, 'n'::CHARACTER AS kind");
		}
		if ( style != MetadataSelectStyle.Minimum ) {
			buf.append(", COALESCE(l.time_zone, 'UTC') AS time_zone");
		}
		buf.append("\nFROM solardatm.da_datm_meta meta\n");
		if ( filter.getUserIds() != null ) {
			buf.append("INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id\n");
		}
		// for Minimum style we don't need to find the time zone so don't have to join to sn_loc table
		if ( style != MetadataSelectStyle.Minimum ) {
			buf.append("LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = meta.node_id\n");
			buf.append("LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id\n");
		}
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
	 * Generate SQL query to find full location metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #locationMetadataFilterSql(LocationMetadataCriteria,
	 *      MetadataSelectStyle, StringBuilder)
	 */
	public static int locationMetadataFilterSql(LocationMetadataCriteria filter, StringBuilder buf) {
		return locationMetadataFilterSql(filter, MetadataSelectStyle.Full, buf);
	}

	/**
	 * Generate SQL query to find location metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param style
	 *        the select style
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #whereLocationMetadata(LocationMetadataCriteria, StringBuilder)
	 * @see #prepareLocationMetadataFilter(LocationMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int locationMetadataFilterSql(LocationMetadataCriteria filter,
			MetadataSelectStyle style, StringBuilder buf) {
		buf.append("SELECT meta.stream_id, meta.loc_id, meta.source_id");
		if ( style == MetadataSelectStyle.Full ) {
			buf.append(", meta.names_i, meta.names_a, meta.names_s, meta.jdata, 'l'::CHARACTER AS kind");
		}
		if ( style != MetadataSelectStyle.Minimum ) {
			buf.append(", COALESCE(l.time_zone, 'UTC') AS time_zone");
		}
		buf.append("\nFROM solardatm.da_loc_datm_meta meta\n");
		if ( filter.getUserIds() != null ) {
			buf.append("INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id\n");
		}
		// for Minimum style we don't need to find the time zone so don't have to join to sn_loc table
		if ( style != MetadataSelectStyle.Minimum ) {
			buf.append("LEFT OUTER JOIN solarnet.sn_loc l ON l.id = meta.loc_id\n");
		}
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
	 * Prepare a SQL query to find streams.
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
	public static int prepareStreamFilter(StreamCriteria filter, Connection con, PreparedStatement stmt,
			int parameterOffset) throws SQLException {
		if ( filter != null ) {
			if ( filter.getStreamIds() != null ) {
				Array array = con.createArrayOf("uuid", filter.getStreamIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			}
		}
		return parameterOffset;
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
	public static int prepareStreamMetadataFilter(StreamMetadataCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null ) {
			parameterOffset = prepareStreamFilter(filter, con, stmt, parameterOffset);
			if ( filter.getSourceIds() != null ) {
				Array array = con.createArrayOf("text", filter.getSourceIds());
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
	 * @see #prepareStreamMetadataFilter(StreamMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int prepareNodeMetadataFilter(NodeMetadataCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null ) {
			if ( filter.getNodeIds() != null ) {
				Array array = con.createArrayOf("bigint", filter.getNodeIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			}
			parameterOffset = prepareStreamMetadataFilter(filter, con, stmt, parameterOffset);
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
	 * @see #prepareStreamMetadataFilter(StreamMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int prepareLocationMetadataFilter(LocationMetadataCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null ) {
			if ( filter.getLocationIds() != null ) {
				Array array = con.createArrayOf("bigint", filter.getLocationIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			}
			parameterOffset = prepareStreamMetadataFilter(filter, con, stmt, parameterOffset);
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL query to find datum metadata.
	 * 
	 * <p>
	 * The first parameter set If a location ID is provided on the filter, then
	 * the filter is assumed to be for location metadata; otherwise node
	 * metadata is assumed.
	 * </p>
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
	 * @see #whereDatumMetadata(DatumCriteria, StringBuilder)
	 * @see #prepareStreamMetadataFilter(StreamMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int prepareDatumMetadataFilter(DatumStreamCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null ) {
			if ( filter.getLocationId() != null ) {
				Array array = con.createArrayOf("bigint", filter.getLocationIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			} else if ( filter.getNodeId() != null ) {
				Array array = con.createArrayOf("bigint", filter.getNodeIds());
				stmt.setArray(++parameterOffset, array);
				array.free();
			}
			parameterOffset = prepareStreamMetadataFilter(filter, con, stmt, parameterOffset);
		}
		return parameterOffset;
	}

	/**
	 * Get the SQL column name representing the time component of a query.
	 * 
	 * @param agg
	 *        the aggregate level being queried
	 * @return if {@code aggregation} is provided and not {@literal None} then
	 *         {@code ts_start}; otherwise {@code ts}
	 */
	public static String timeColumnName(Aggregation aggregation) {
		return (aggregation == null || aggregation == Aggregation.None ? "ts" : "ts_start");
	}

	/**
	 * Generate SQL {@literal WHERE} criteria to find stream metadata.
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
	 * @see #whereDateRange(DateRangeCriteria, Aggregation, StringBuilder)
	 */
	public static int whereDateRange(DateRangeCriteria filter, StringBuilder buf) {
		return whereDateRange(filter, null, buf);
	}

	/**
	 * Generate SQL {@literal WHERE} criteria to find stream metadata.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal \tAND c = ?\n} for
	 * each clause. The leading tab and {@literal AND} and space characters are
	 * <b>not</b> stripped.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @param aggregation
	 *        if provided and not {@code None} then treat the time criteria
	 *        column name as {@code ts_start}; otherwise use {@code ts}
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static int whereDateRange(DateRangeCriteria filter, Aggregation aggregation,
			StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getStartDate() != null ) {
			buf.append("\tAND datum.").append(timeColumnName(aggregation)).append(" >= ?\n");
			paramCount += 1;
		}
		if ( filter.getEndDate() != null ) {
			buf.append("\tAND datum.").append(timeColumnName(aggregation)).append(" < ?\n");
			paramCount += 1;
		}
		return paramCount;
	}

	/**
	 * Generate SQL {@literal LIMIT x OFFSET y} criteria to support pagination.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal \nLIMIT ? OFFSET ?}.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static int limitOffset(PaginationCriteria filter, StringBuilder buf) {
		if ( filter != null && filter.getMax() != null ) {
			int max = filter.getMax();
			if ( max > 0 ) {
				buf.append("\nLIMIT ? OFFSET ?");
				return 2;
			}
		}
		return 0;
	}

	/**
	 * Generate SQL {@literal LIMIT x OFFSET y} criteria to support pagination
	 * where the limit and offset are generated as literal values.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal \nLIMIT x OFFSET y}.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static void limitOffsetLiteral(PaginationCriteria filter, StringBuilder buf) {
		if ( filter != null && filter.getMax() != null ) {
			int max = filter.getMax();
			if ( max > 0 ) {
				buf.append("\nLIMIT ").append(max);
			}
			Integer offset = filter.getOffset();
			if ( offset != null ) {
				buf.append(" OFFSET ").append(offset);
			}
		}
	}

	/**
	 * Generate SQL {@literal FOR UPDATE SKIP LOCKED} criteria to support
	 * locking.
	 * 
	 * @param skipLocked
	 *        {@literal true} to include the {@literal SKIP LOCKED} clause
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static void forUpdate(boolean skipLocked, StringBuilder buf) {
		buf.append("\nFOR UPDATE");
		if ( skipLocked ) {
			buf.append(" SKIP LOCKED");
		}
	}

	/**
	 * A standardized SQL clause for casting a local date to a stream's time
	 * zone.
	 * 
	 * <p>
	 * Can be passed as the {@code zoneClause} to
	 * {@link #whereLocalDateRange(LocalDateRangeCriteria, Aggregation, String, StringBuilder)}.
	 * </p>
	 */
	public static final String SQL_AT_STREAM_METADATA_TIME_ZONE = "AT TIME ZONE s.time_zone";

	/**
	 * Generate SQL {@literal WHERE} criteria to find stream metadata.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal \tAND c = ?\n} for
	 * each clause. The leading tab and {@literal AND} and space characters are
	 * <b>not</b> stripped.
	 * </p>
	 * 
	 * <p>
	 * The {@code zoneClause} argument can be used to cast the local date
	 * parameters into absolute dates. For example, assuming a stream metadata
	 * table is available under the alias {@literal s}, a {@code zoneClause} of
	 * {@literal AT TIME ZONE s.time_zone} would generate SQl like:
	 * </p>
	 * 
	 * <pre>
	 * 	AND datum.ts >= ? AT TIME ZONE s.time_zone
	 * 	AND datum.ts < ? AT TIME ZONE s.time_zone
	 * </pre>
	 * 
	 * @param filter
	 *        the search criteria
	 * @param aggregation
	 *        if provided and not {@code None} then treat the time criteria
	 *        column name as {@code ts_start}; otherwise use {@code ts}
	 * @param zoneClause
	 *        if provided, then an extra clause to add after each local date
	 *        parameter placeholder
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static int whereLocalDateRange(LocalDateRangeCriteria filter, Aggregation aggregation,
			String zoneClause, StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getLocalStartDate() != null ) {
			buf.append("\tAND datum.").append(timeColumnName(aggregation)).append(" >= ?");
			if ( zoneClause != null ) {
				buf.append(" ").append(zoneClause);
			}
			buf.append("\n");
			paramCount += 1;
		}
		if ( filter.getLocalEndDate() != null ) {
			buf.append("\tAND datum.").append(timeColumnName(aggregation)).append(" < ?");
			if ( zoneClause != null ) {
				buf.append(" ").append(zoneClause);
			}
			buf.append("\n");
			paramCount += 1;
		}
		return paramCount;
	}

	/**
	 * Prepare a SQL query date range filter.
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
	public static int prepareDateRangeFilter(DateRangeCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter.getStartDate() != null ) {
			stmt.setTimestamp(++parameterOffset, Timestamp.from(filter.getStartDate()));
		}
		if ( filter.getEndDate() != null ) {
			stmt.setTimestamp(++parameterOffset, Timestamp.from(filter.getEndDate()));
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL query local date range filter.
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
	public static int prepareLocalDateRangeFilter(LocalDateRangeCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter.getLocalStartDate() != null ) {
			stmt.setObject(++parameterOffset, filter.getLocalStartDate(), Types.TIMESTAMP);
		}
		if ( filter.getLocalEndDate() != null ) {
			stmt.setObject(++parameterOffset, filter.getLocalEndDate(), Types.TIMESTAMP);
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL query limit/offset.
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
	 * @see #limitOffset(PaginationCriteria, StringBuilder)
	 */
	public static int preparePaginationFilter(PaginationCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null && filter.getMax() != null ) {
			int max = filter.getMax();
			if ( max > 0 ) {
				int offset = 0;
				if ( filter.getOffset() != null ) {
					offset = filter.getOffset();
				}
				stmt.setInt(++parameterOffset, max);
				stmt.setInt(++parameterOffset, offset);
			}
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

	/**
	 * Standardized utility to execute a filter based query.
	 * 
	 * @param <M>
	 *        the filter result type
	 * @param <K>
	 *        the filter result key type
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param filter
	 *        the pagination criteria
	 * @param sql
	 *        the SQL to execute
	 * @param mapper
	 *        the row mapper to use
	 * @return the results, never {@literal null}
	 */
	public static <M extends Identity<K>, K> FilterResults<M, K> executeFilterQuery(
			JdbcOperations jdbcTemplate, PaginationCriteria filter, PreparedStatementCreator sql,
			RowMapper<M> mapper) {
		Long totalCount = null;
		if ( filter.getMax() != null && sql instanceof CountPreparedStatementCreatorProvider ) {
			totalCount = DatumSqlUtils.executeCountQuery(jdbcTemplate,
					((CountPreparedStatementCreatorProvider) sql).countPreparedStatementCreator());
		}

		List<M> results = jdbcTemplate.query(sql, mapper);

		if ( filter.getMax() == null ) {
			totalCount = (long) results.size();
		}

		int offset = (filter.getOffset() != null ? filter.getOffset() : 0);
		return new BasicFilterResults<>(results, totalCount, offset, results.size());
	}

	/**
	 * Get a UUID column value.
	 * 
	 * <p>
	 * This method can be more efficient than calling
	 * {@link ResultSet#getString(int)} if the JDBC driver returns a UUID
	 * instance natively. Otherwise this method will call {@code toString()} on
	 * the column value and parse that as a UUID.
	 * </p>
	 * 
	 * @param rs
	 *        the result set to read from
	 * @param column
	 *        the column number to get as a UUID
	 * @return the UUID, or {@literal null} if the column value is null
	 * @throws SQLException
	 *         if an error occurs
	 * @throws IllegalArgumentException
	 *         if the column value is non-null but does not conform to the
	 *         string representation as described in {@link UUID#toString()}
	 */
	public static UUID getUuid(ResultSet rs, int column) throws SQLException {
		Object sid = rs.getObject(column);
		return (sid instanceof UUID ? (UUID) sid : sid != null ? UUID.fromString(sid.toString()) : null);
	}

	/**
	 * Generate a cache key out of a stream filter.
	 * 
	 * <p>
	 * If the filter defines a single stream ID, then the
	 * {@link UUID#toString()} version of that is returned directly. Otherwise,
	 * the stream IDs are hashed into a SHA-1 digest value, and the hex-encoded
	 * result of that is returned.
	 * </p>
	 * 
	 * @param filter
	 *        the filter
	 * @param sortKeyMapping
	 *        the sort mapping to use if sorts are included in the filter
	 * @return the cache key, as a string, or {@literal null} if one cannot be
	 *         generated
	 */
	public static String streamMetadataCacheKey(StreamMetadataCriteria filter,
			Map<String, String> sortKeyMapping) {
		UUID[] streamIds = filter.getStreamIds();
		if ( streamIds == null || streamIds.length < 1 ) {
			return null;
		}

		// specialized case for single stream ID: use ID directly as cache key
		if ( streamIds.length == 1 ) {
			return streamIds[0].toString();
		}

		// otherwise, compute SHA1 digest
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] buf = new byte[8];
			if ( streamIds.length > 1 ) {
				// for >1 stream ID, sort them for a stable cache key
				streamIds = new UUID[filter.getStreamIds().length];
				System.arraycopy(filter.getStreamIds(), 0, streamIds, 0, streamIds.length);
				Arrays.sort(streamIds);
			}
			for ( UUID uuid : streamIds ) {
				ByteUtils.encodeUnsignedInt64(uuid.getMostSignificantBits(), buf, 0,
						ByteOrdering.BigEndian);
				digest.update(buf);
				ByteUtils.encodeUnsignedInt64(uuid.getLeastSignificantBits(), buf, 0,
						ByteOrdering.BigEndian);
				digest.update(buf);
			}
			if ( filter.getSorts() != null && !filter.getSorts().isEmpty() ) {
				StringBuilder order = new StringBuilder();
				orderBySorts(filter.getSorts(), sortKeyMapping, order);
				digest.update(order.toString().getBytes(Charset.forName("UTF-8")));
			}
			return ByteUtils.encodeHexString(digest.digest(), 0, digest.getDigestLength(), false, true);
		} catch ( NoSuchAlgorithmException e ) {
			log.warn("SHA-1 digest not available; cache key cannot be generated.");
			return null;
		}
	}

}
