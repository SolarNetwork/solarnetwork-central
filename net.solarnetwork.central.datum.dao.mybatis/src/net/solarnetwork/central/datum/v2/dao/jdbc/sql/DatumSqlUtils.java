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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import static java.lang.String.format;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.v2.dao.CombiningConfig;
import net.solarnetwork.central.datum.v2.dao.CombiningIdsConfig;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.LocationMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.NodeMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.ObjectMetadataCriteria;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.StreamCriteria;
import net.solarnetwork.central.datum.v2.dao.StreamMetadataCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.LocalDateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.domain.ByteOrdering;
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
	 * Regex for a line starting with a {@literal --} SQL style comment
	 * character.
	 */
	public static final Pattern SQL_COMMENT = Pattern.compile("^\\s*--");

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

	private static String[] DEFAULT_METADATA_SORT_KEYS = new String[] { "loc", "node", "source" };

	/**
	 * Test if a default stream metadata sort key is present in a list of sort
	 * descriptors.
	 * 
	 * <p>
	 * The default list of metadata sort keys used is:
	 * </p>
	 * 
	 * <ol>
	 * <li>loc</li>
	 * <li>node</li>
	 * <li>source</li>
	 * </ol>
	 * 
	 * @param sorts
	 *        the sort descriptors to search for metadata keys in
	 * @return {@literal true} if some sort descriptor in {@code sorts} is also
	 *         in the default list of metadata sort keys
	 */
	public static boolean hasMetadataSortKey(Iterable<SortDescriptor> sorts) {
		return hasMetadataSortKey(sorts, DEFAULT_METADATA_SORT_KEYS);
	}

	/**
	 * Test if a stream metadata sort key is present in a list of sort
	 * descriptors.
	 * 
	 * <p>
	 * This method can be useful for sorting datum stream result sets, to know
	 * if stream metadata must be available to sort by. For example a SQL query
	 * might need to include a {@literal JOIN} clause to a stream metadata table
	 * if a metadata sort key is requested.
	 * </p>
	 * 
	 * @param sorts
	 *        the sort descriptors to search for metadata keys in
	 * @param orderedMetaSortKeys
	 *        a sorted list of metadata keys that represent all possible
	 *        metadata sort keys
	 * @return {@literal true} if some sort descriptor in {@code sorts} is also
	 *         in {@code orderedMetaSortKeys}
	 */
	public static boolean hasMetadataSortKey(Iterable<SortDescriptor> sorts,
			String[] orderedMetaSortKeys) {
		if ( sorts == null ) {
			return false;
		}
		for ( SortDescriptor s : sorts ) {
			if ( Arrays.binarySearch(orderedMetaSortKeys, s.getSortKey()) >= 0 ) {
				return true;
			}
		}
		return false;
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
			buf.append(
					"\tAND s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(?))))\n");
			paramCount += 1;
		}
		if ( filter.getStreamIds() != null ) {
			buf.append("\tAND s.stream_id = ANY(?)\n");
			paramCount += 1;
		}
		if ( filter.getUserIds() != null ) {
			buf.append("\tAND un.user_id = ANY(?)\n");
			paramCount += 1;
		}
		if ( filter.getTokenIds() != null ) {
			buf.append("\tAND ut.auth_token = ANY(?)\n");
			buf.append("	AND (COALESCE(jsonb_array_length(ut.jpolicy->'sourceIds'), 0) < 1\n");
			buf.append(
					"		OR s.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(jsonb_array_elements_text(ut.jpolicy->'sourceIds'))))\n");
			buf.append("	)\n");
			buf.append("	AND (COALESCE(jsonb_array_length(ut.jpolicy->'nodeIds'), 0) < 1\n");
			buf.append(
					"		OR s.node_id = ANY(ARRAY(SELECT solarcommon.jsonb_array_to_bigint_array(ut.jpolicy->'nodeIds')))\n");
			buf.append("	)\n");
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
	public static int whereNodeMetadata(ObjectMetadataCriteria filter, StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getObjectIds() != null ) {
			buf.append("\tAND s.node_id = ANY(?)\n");
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
	public static int whereLocationMetadata(ObjectMetadataCriteria filter, StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getObjectIds() != null ) {
			buf.append("\tAND s.loc_id = ANY(?)\n");
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
			buf.append("\tAND s.loc_id = ANY(?)\n");
			paramCount += 1;
		} else if ( filter.getNodeId() != null ) {
			buf.append("\tAND s.node_id = ANY(?)\n");
			paramCount += 1;
		}
		paramCount += whereStreamMetadata(filter, buf);
		return paramCount;
	}

	/**
	 * A metadata select style, to optimize queries.
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
	public static int nodeMetadataFilterSql(ObjectMetadataCriteria filter, StringBuilder buf) {
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
	 * @see #prepareObjectMetadataFilter(NodeMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int nodeMetadataFilterSql(ObjectMetadataCriteria filter, MetadataSelectStyle style,
			StringBuilder buf) {
		return nodeMetadataFilterSql(filter, style, null, null, null, null,
				SQL_AT_STREAM_METADATA_TIME_ZONE, buf);
	}

	/**
	 * Generate SQL query to find node metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param style
	 *        the select style
	 * @param combiningConfig
	 *        an optional combining configuration to remap source/node IDs
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #whereNodeMetadata(NodeMetadataCriteria, StringBuilder)
	 * @see #prepareObjectMetadataFilter(NodeMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int nodeMetadataFilterSql(ObjectMetadataCriteria filter, MetadataSelectStyle style,
			CombiningConfig combiningConfig, StringBuilder buf) {
		return nodeMetadataFilterSql(filter, style, null, null, null, combiningConfig,
				SQL_AT_STREAM_METADATA_TIME_ZONE, buf);
	}

	/**
	 * Generate a SQL clause for metadata object ID selection.
	 * 
	 * @param combiningConfig
	 *        the optional combining configuration
	 * @param kind
	 *        the object kind
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static int metadataObjectSourceIdsSql(CombiningConfig combiningConfig, ObjectDatumKind kind,
			StringBuilder buf) {
		final String objName = (kind == ObjectDatumKind.Location ? "loc" : "node");
		int paramCount = 0;
		if ( combiningConfig == null || !combiningConfig.isWithObjectIds() ) {
			buf.append(", s.").append(objName).append("_id");
		} else {
			CombiningIdsConfig<Long> objIdConfig = combiningConfig
					.getIdsConfig(CombiningConfig.OBJECT_IDS_CONFIG);
			buf.append("\n, CASE\n");
			for ( Iterator<Long> iterator = objIdConfig.getIdSets().keySet().iterator(); iterator
					.hasNext(); ) {
				buf.append("	WHEN array_position(?, s.").append(objName)
						.append("_id) IS NOT NULL THEN ?\n");
				paramCount += 2;
				iterator.next();
			}
			buf.append("	ELSE s.").append(objName).append("_id\n");
			buf.append("	END AS ").append(objName).append("_id\n");
			buf.append(", COALESCE(array_position(?, s.").append(objName).append("_id), 0) AS obj_rank");
			paramCount += 1;
		}
		if ( combiningConfig == null || !combiningConfig.isWithSourceIds() ) {
			buf.append(", s.source_id");
		} else {
			CombiningIdsConfig<String> objIdConfig = combiningConfig
					.getIdsConfig(CombiningConfig.SOURCE_IDS_CONFIG);
			buf.append("\n, CASE\n");
			for ( Iterator<String> iterator = objIdConfig.getIdSets().keySet().iterator(); iterator
					.hasNext(); ) {
				buf.append("	WHEN array_position(?, s.source_id::TEXT) IS NOT NULL THEN ?\n");
				paramCount += 2;
				iterator.next();
			}
			buf.append("	ELSE s.source_id\n");
			buf.append("	END AS source_id\n");
			buf.append(", COALESCE(array_position(?, s.source_id::TEXT), 0) AS source_rank");
			paramCount += 1;
		}
		if ( combiningConfig != null ) {
			buf.append("\n, s.names_i\n");
			buf.append(", s.names_a");
		}
		return paramCount;
	}

	/**
	 * Generate SQL query to find node metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param style
	 *        the select style
	 * @param streamFilter
	 *        the filter whose date or local date range to use
	 * @param tableName
	 *        the datum table name to use
	 * @param aggregation
	 *        the aggregation level of the datum table name to determine the SQL
	 * @param combiningConfig
	 *        an optional combining configuration to remap source/node IDs
	 * @param zoneClause
	 *        the time zone clause to use for local date ranges (e.g.
	 *        {@link #SQL_AT_STREAM_METADATA_TIME_ZONE} column name
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #whereNodeMetadata(NodeMetadataCriteria, StringBuilder)
	 * @see #prepareObjectMetadataFilter(NodeMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int nodeMetadataFilterSql(ObjectMetadataCriteria filter, MetadataSelectStyle style,
			ObjectStreamCriteria streamFilter, String datumTableName, Aggregation aggregation,
			CombiningConfig combiningConfig, String zoneClause, StringBuilder buf) {
		buf.append("SELECT s.stream_id");
		int paramCount = metadataObjectSourceIdsSql(combiningConfig, ObjectDatumKind.Node, buf);
		if ( style == MetadataSelectStyle.Full ) {
			buf.append(", s.names_i, s.names_a, s.names_s, s.jdata, 'n'::CHARACTER AS kind");
		}
		if ( style != MetadataSelectStyle.Minimum ) {
			buf.append(", COALESCE(l.time_zone, 'UTC') AS time_zone");
		}
		buf.append("\nFROM solardatm.da_datm_meta s\n");
		if ( filter != null && (filter.getUserIds() != null || filter.getTokenIds() != null) ) {
			buf.append("INNER JOIN solaruser.user_node un ON un.node_id = s.node_id\n");
		}
		if ( filter != null && filter.getTokenIds() != null ) {
			buf.append("INNER JOIN solaruser.user_auth_token ut ON ut.user_id = un.user_id\n");
		}
		// for Minimum style we don't need to find the time zone so don't have to join to sn_loc table
		if ( style != MetadataSelectStyle.Minimum ) {
			buf.append("LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = s.node_id\n");
			buf.append("LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id\n");
		}
		if ( filter != null || streamFilter != null ) {
			StringBuilder where = new StringBuilder();
			if ( streamFilter != null ) {
				// NOTE join added directly to buf
				paramCount += joinStreamMetadataDateRangeSql(streamFilter, datumTableName, aggregation,
						zoneClause, buf);
			}
			if ( filter != null ) {
				paramCount += whereNodeMetadata(filter, where);
			}
			if ( where.length() > 0 ) {
				buf.append("WHERE");
				buf.append(where.substring(4));
			}
		}
		return paramCount;
	}

	/**
	 * Generate SQL {@code INNER JOIN} clause for stream metadata to a datum
	 * table on the most extreme datum available (earliest or latest).
	 * 
	 * @param tableName
	 *        the datum table name
	 * @param timeColumnName
	 *        the datum table time column name, e.g. {@literal ts} or
	 *        {@literal ts_start}
	 * @param latest
	 *        {@literal true} for the highest time value and a join table name
	 *        of {@literal late}, {@literal false} for the smallest time value
	 *        and a join table name of {@literal early}
	 * @param buf
	 *        the buffer to append the SQL to
	 */
	public static void joinStreamMetadataExtremeDatumSql(String tableName, String timeColumnName,
			boolean latest, StringBuilder buf) {
		buf.append("INNER JOIN LATERAL (\n");
		buf.append("		SELECT datum.*\n");
		buf.append("		FROM ").append(tableName).append(" datum\n");
		buf.append("		WHERE datum.stream_id = s.stream_id\n");
		buf.append("		ORDER BY datum.").append(timeColumnName);
		if ( latest ) {
			buf.append(" DESC");
		}
		buf.append("\n");
		buf.append("		LIMIT 1\n");
		buf.append("	) ").append(latest ? "late" : "early").append(" ON ")
				.append(latest ? "late" : "early").append(".stream_id = s.stream_id\n");
	}

	/**
	 * Generate a SQL {@literal INNER JOIN} clause to limit metadata to a date
	 * range.
	 * 
	 * @param filter
	 *        the filter whose date or local date range to use
	 * @param tableName
	 *        the datum table name to use
	 * @param aggregation
	 *        the aggregation level of the datum table name to determine the SQL
	 *        column name
	 * @param zoneClause
	 *        the time zone clause to use for local date ranges (e.g.
	 *        {@link #SQL_AT_STREAM_METADATA_TIME_ZONE}
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static int joinStreamMetadataDateRangeSql(ObjectStreamCriteria filter, String tableName,
			Aggregation aggregation, String zoneClause, StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int paramCount = filter.hasLocalDateRange()
				? DatumSqlUtils.whereLocalDateRange(filter, aggregation, zoneClause, where)
				: DatumSqlUtils.whereDateRange(filter, aggregation, where);
		if ( paramCount < 1 ) {
			return 0;
		}
		buf.append("INNER JOIN LATERAL (");
		buf.append("	SELECT stream_id");
		buf.append("	FROM ").append(tableName).append(" datum");
		buf.append("	WHERE datum.stream_id = s.stream_id");
		buf.append(where);
		buf.append("	LIMIT 1");
		buf.append(") d ON d.stream_id = s.stream_id\n");
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
	public static int locationMetadataFilterSql(ObjectMetadataCriteria filter, StringBuilder buf) {
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
	 * @see #prepareObjectMetadataFilter(LocationMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int locationMetadataFilterSql(ObjectMetadataCriteria filter, MetadataSelectStyle style,
			StringBuilder buf) {
		return locationMetadataFilterSql(filter, style, null, null, null, null,
				SQL_AT_STREAM_METADATA_TIME_ZONE, buf);
	}

	/**
	 * Generate SQL query to find location metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param style
	 *        the select style
	 * @param combiningConfig
	 *        an optional combining configuration to remap source/node IDs
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #whereLocationMetadata(LocationMetadataCriteria, StringBuilder)
	 * @see #prepareObjectMetadataFilter(LocationMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int locationMetadataFilterSql(ObjectMetadataCriteria filter, MetadataSelectStyle style,
			CombiningConfig combiningConfig, StringBuilder buf) {
		return locationMetadataFilterSql(filter, style, null, null, null, combiningConfig,
				SQL_AT_STREAM_METADATA_TIME_ZONE, buf);
	}

	/**
	 * Generate SQL query to find location metadata.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param style
	 *        the select style
	 * @param streamFilter
	 *        optional stream filter to use date ranges from
	 * @param streamFilter
	 *        the filter whose date or local date range to use
	 * @param tableName
	 *        the datum table name to use
	 * @param aggregation
	 *        the aggregation level of the datum table name to determine the SQL
	 * @param combiningConfig
	 *        an optional combining configuration to remap source/node IDs
	 * @param zoneClause
	 *        the time zone clause to use for local date ranges (e.g.
	 *        {@link #SQL_AT_STREAM_METADATA_TIME_ZONE} column name
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @see #whereLocationMetadata(LocationMetadataCriteria, StringBuilder)
	 * @see #prepareObjectMetadataFilter(LocationMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int locationMetadataFilterSql(ObjectMetadataCriteria filter, MetadataSelectStyle style,
			ObjectStreamCriteria streamFilter, String datumTableName, Aggregation aggregation,
			CombiningConfig combiningConfig, String zoneClause, StringBuilder buf) {
		buf.append("SELECT s.stream_id");
		int paramCount = metadataObjectSourceIdsSql(combiningConfig, ObjectDatumKind.Location, buf);
		if ( style == MetadataSelectStyle.Full ) {
			buf.append(", s.names_i, s.names_a, s.names_s, s.jdata, 'l'::CHARACTER AS kind");
		}
		if ( style != MetadataSelectStyle.Minimum ) {
			buf.append(", COALESCE(l.time_zone, 'UTC') AS time_zone");
		}
		buf.append("\nFROM solardatm.da_loc_datm_meta s\n");
		if ( filter != null && filter.getUserIds() != null ) {
			buf.append("INNER JOIN solaruser.user_node un ON un.node_id = s.node_id\n");
		}
		// for Minimum style we don't need to find the time zone so don't have to join to sn_loc table
		if ( style != MetadataSelectStyle.Minimum ) {
			buf.append("LEFT OUTER JOIN solarnet.sn_loc l ON l.id = s.loc_id\n");
		}

		if ( filter != null || streamFilter != null ) {
			StringBuilder where = new StringBuilder();
			if ( streamFilter != null ) {
				// NOTE join added directly to buf
				paramCount += joinStreamMetadataDateRangeSql(streamFilter, datumTableName, aggregation,
						zoneClause, buf);
			}
			if ( filter != null ) {
				paramCount += whereLocationMetadata(filter, where);
			}
			if ( where.length() > 0 ) {
				buf.append("WHERE");
				buf.append(where.substring(4));
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
			if ( filter.getTokenIds() != null ) {
				Array array = con.createArrayOf("text", filter.getTokenIds());
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
	 * @see #locationMetadataFilterSql(ObjectMetadataCriteria, StringBuilder)
	 * @see #prepareStreamMetadataFilter(StreamMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int prepareObjectMetadataFilter(ObjectMetadataCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null ) {
			if ( filter.getObjectIds() != null ) {
				Array array = con.createArrayOf("bigint", filter.getObjectIds());
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
	public static int prepareDatumMetadataFilter(ObjectStreamCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		return prepareDatumMetadataFilter(filter, null, con, stmt, parameterOffset);
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
	 * @param combiningConfig
	 *        an optional combining configuration to remap source/node IDs
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @see #whereDatumMetadata(DatumStreamCriteria, StringBuilder)
	 * @see #prepareStreamMetadataFilter(StreamMetadataCriteria, Connection,
	 *      PreparedStatement, int)
	 */
	public static int prepareDatumMetadataFilter(ObjectStreamCriteria filter,
			CombiningConfig combiningConfig, Connection con, PreparedStatement stmt, int parameterOffset)
			throws SQLException {
		if ( combiningConfig != null && combiningConfig.isWithObjectIds() ) {
			CombiningIdsConfig<Long> objIdConfig = combiningConfig
					.getIdsConfig(CombiningConfig.OBJECT_IDS_CONFIG);
			List<Long> allIds = new ArrayList<>();
			for ( Map.Entry<Long, Set<Long>> me : objIdConfig.getIdSets().entrySet() ) {
				allIds.addAll(me.getValue());
				Long[] ids = me.getValue().toArray(new Long[me.getValue().size()]);
				Array array = con.createArrayOf("bigint", ids);
				stmt.setArray(++parameterOffset, array);
				array.free();
				stmt.setObject(++parameterOffset, me.getKey());
			}
			Array array = con.createArrayOf("bigint", allIds.toArray(new Long[allIds.size()]));
			stmt.setArray(++parameterOffset, array);
			array.free();
		}
		if ( combiningConfig != null && combiningConfig.isWithSourceIds() ) {
			CombiningIdsConfig<String> sourceIdConfig = combiningConfig
					.getIdsConfig(CombiningConfig.SOURCE_IDS_CONFIG);
			List<String> allIds = new ArrayList<>();
			for ( Map.Entry<String, Set<String>> me : sourceIdConfig.getIdSets().entrySet() ) {
				allIds.addAll(me.getValue());
				String[] ids = me.getValue().toArray(new String[me.getValue().size()]);
				Array array = con.createArrayOf("text", ids);
				stmt.setArray(++parameterOffset, array);
				array.free();
				stmt.setString(++parameterOffset, me.getKey());
			}
			Array array = con.createArrayOf("text", allIds.toArray(new String[allIds.size()]));
			stmt.setArray(++parameterOffset, array);
			array.free();
		}
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
		return whereLocalDateRange(filter, aggregation, zoneClause, null, null, buf);
	}

	/**
	 * Generate SQL {@literal WHERE} criteria to find stream metadata.
	 * 
	 * <p>
	 * The generated SQL will include {@literal date_trunc()} calls on the start
	 * and/or end date parameters, according to the {@code startRoundingMode}
	 * and {@code endRoundingMode} parameters.
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
	 * @param startRoundingMode
	 *        the rounding mode to apply to the start date, or {@literal null}
	 *        for none
	 * @param endRoundingMode
	 *        the rounding mode to apply to the end date, or {@literal null}
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static int whereLocalDateRange(LocalDateRangeCriteria filter, Aggregation aggregation,
			String zoneClause, RoundingMode startRoundingMode, RoundingMode endRoundingMode,
			StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getLocalStartDate() != null ) {
			buf.append("\tAND datum.").append(timeColumnName(aggregation)).append(" >= ");
			dateRoundedParameter(aggregation, startRoundingMode, buf);
			if ( zoneClause != null ) {
				buf.append(" ").append(zoneClause);
			}
			buf.append("\n");
			paramCount += 1;
		}
		if ( filter.getLocalEndDate() != null ) {
			buf.append("\tAND datum.").append(timeColumnName(aggregation)).append(" < ");
			dateRoundedParameter(aggregation, endRoundingMode, buf);
			if ( zoneClause != null ) {
				buf.append(" ").append(zoneClause);
			}
			buf.append("\n");
			paramCount += 1;
		}
		return paramCount;
	}

	/**
	 * Generate a rounded date SQL parameter clause.
	 * 
	 * <p>
	 * The output is along the lines of {@literal date_trunc('day', ?)}. If no
	 * rounding mode is needed (because {@code roundingMode} is {@literal null}
	 * or {@literal UNNECESSARY}) then a simple {@literal ?} placeholder will be
	 * generated.
	 * </p>
	 * 
	 * @param aggregation
	 *        the aggregation mode
	 * @param roundingMode
	 *        the rounding mode
	 * @param buf
	 *        the buffer to append the generated SQL to
	 * @throws IllegalArgumentException
	 *         if {@code roundingMode} is not supported
	 */
	public static void dateRoundedParameter(Aggregation aggregation, RoundingMode roundingMode,
			StringBuilder buf) {
		if ( roundingMode == null || roundingMode == RoundingMode.UNNECESSARY || aggregation == null
				|| !(aggregation == Aggregation.Hour || aggregation == Aggregation.Day
						|| aggregation == Aggregation.Month || aggregation == Aggregation.Year) ) {
			buf.append("?");
			return;
		}

		buf.append("date_trunc('").append(sqlDateRoundingInterval(aggregation)).append("', ?");
		switch (roundingMode) {
			case DOWN:
			case FLOOR:
				// nothing more
				break;

			case UP:
			case CEILING:
				buf.append(" + ").append(sqlInterval(aggregation, 1));
				break;

			default:
				throw new IllegalArgumentException(format(
						"The rounding mode %s is not supported for date parameters.", roundingMode));
		}
		buf.append(')');
	}

	/**
	 * Generate a SQL {@literal INTERVAL} clause for an aggregation.
	 * 
	 * @param aggregation
	 *        the aggregation
	 * @param count
	 *        the interval count
	 * @return the SQL clause
	 * @throws IllegalArgumentException
	 *         if {@code aggregation} is {@literal null} or not supported
	 */
	public static String sqlInterval(Aggregation aggregation, int count) {
		if ( aggregation == null ) {
			throw new IllegalArgumentException("The aggregation argument must not be null.");
		}
		StringBuilder buf = new StringBuilder();
		buf.append("INTERVAL 'P");
		if ( aggregation == Aggregation.Hour ) {
			buf.append('T');
		}
		buf.append(count);
		switch (aggregation) {
			case Hour:
				buf.append('H');
				break;

			case Day:
				buf.append('D');
				break;

			case Month:
				buf.append('M');
				break;

			case Year:
				buf.append('Y');
				break;

			default:
				throw new IllegalArgumentException(format(
						"The aggregation %s cannot be translated into a SQL interval.", aggregation));
		}
		buf.append("'");
		return buf.toString();
	}

	/**
	 * Get a SQL interval type for an aggregation.
	 * 
	 * @param aggregation
	 *        the aggregation to get the type for
	 * @return the type
	 * @throws IllegalArgumentException
	 *         if {@code aggregation} is {@literal null} or not supported
	 */
	public static String sqlDateRoundingInterval(Aggregation aggregation) {
		if ( aggregation != null ) {
			switch (aggregation) {
				case Hour:
					return "hour";

				case Day:
					return "day";

				case Month:
					return "month";

				case Year:
					return "year";

				default:
					// fall through to exception
			}
		}
		throw new IllegalArgumentException(
				format("The aggregation %s cannot be converted to a SQL interval."));
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
