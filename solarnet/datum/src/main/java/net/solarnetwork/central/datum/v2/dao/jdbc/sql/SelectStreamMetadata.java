/* ==================================================================
 * SelectStreamMetadata.java - 19/11/2020 3:21:24 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.STREAM_METADATA_SORT_KEY_MAPPING;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.orderBySorts;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.StreamMetadataCriteria;
import net.solarnetwork.util.Cachable;

/**
 * Generate dynamic SQL for a "find metadata for streams" query.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectStreamMetadata implements PreparedStatementCreator, SqlProvider, Cachable {

	private final StreamMetadataCriteria filter;
	private final Long cacheTti;
	private final Long cacheTtl;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectStreamMetadata(StreamMetadataCriteria filter) {
		this(filter, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @param cacheTtl
	 *        a cache time-to-live value, or {@literal null} for none
	 * @param cacheTti
	 *        a cache time-to-idle value, or {@literal null} for none
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectStreamMetadata(StreamMetadataCriteria filter, Long cacheTtl, Long cacheTti) {
		super();
		if ( filter == null || filter.getStreamIds() == null || filter.getStreamIds().length < 1 ) {
			throw new IllegalArgumentException("The filter argument and stream IDs must not be null.");
		}
		this.filter = filter;
		this.cacheTtl = cacheTtl;
		this.cacheTti = cacheTti;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append(
				"SELECT m.stream_id, obj_id, source_id, names_i, names_a, names_s, jdata, kind, time_zone\n");
		buf.append("FROM unnest(?) s(stream_id)\n");
		buf.append("INNER JOIN solardatm.find_metadata_for_stream(s.stream_id) m ON TRUE");
		StringBuilder order = new StringBuilder();
		int idx = orderBySorts(filter.getSorts(), STREAM_METADATA_SORT_KEY_MAPPING, order);
		if ( idx > 0 ) {
			buf.append("\nORDER BY ");
			buf.append(order.substring(idx));
		}
		return buf.toString();
	}

	private PreparedStatement createStatement(Connection con, String sql) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		Array array = con.createArrayOf("uuid", filter.getStreamIds());
		stmt.setArray(1, array);
		array.free();
		return stmt;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return createStatement(con, getSql());
	}

	@Override
	public String getCacheKey() {
		if ( cacheTtl == null && cacheTti == null ) {
			// caching not supported
			return null;
		}
		return DatumSqlUtils.streamMetadataCacheKey(filter, STREAM_METADATA_SORT_KEY_MAPPING);
	}

	@Override
	public Long getTtl() {
		return cacheTtl;
	}

	@Override
	public Long getTti() {
		return cacheTti;
	}

}
