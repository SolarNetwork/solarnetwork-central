/* ==================================================================
 * SelectNodeStreamMetadata.java - 19/11/2020 3:21:24 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils.orderBySorts;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils.MetadataSelectStyle;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Generate dynamic SQL for a "find node metadata" query.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectObjectStreamMetadata implements PreparedStatementCreator, SqlProvider {

	private final ObjectStreamCriteria filter;
	private final ObjectDatumKind kind;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code filter.getObjectKind()} value will be used if available,
	 * otherwise the {@code Node} kind will be used.
	 * </p>
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectObjectStreamMetadata(ObjectStreamCriteria filter) {
		this(filter, filter.getObjectKind() != null ? filter.getObjectKind() : ObjectDatumKind.Node);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectObjectStreamMetadata(ObjectStreamCriteria filter, ObjectDatumKind kind) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument must not be null.");
		}
		if ( kind == null ) {
			throw new IllegalArgumentException("The kind argument must not be null.");
		}
		this.filter = filter;
		this.kind = kind;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		if ( kind == ObjectDatumKind.Location ) {
			DatumSqlUtils.locationMetadataFilterSql(filter, MetadataSelectStyle.Full, filter,
					"solardatm.da_datm", Aggregation.None, buf);
		} else {
			DatumSqlUtils.nodeMetadataFilterSql(filter, MetadataSelectStyle.Full, filter,
					"solardatm.da_datm", Aggregation.None, buf);
		}
		StringBuilder order = new StringBuilder();
		int idx = orderBySorts(filter.getSorts(),
				kind == ObjectDatumKind.Location
						? DatumSqlUtils.LOCATION_STREAM_METADATA_SORT_KEY_MAPPING
						: DatumSqlUtils.NODE_STREAM_METADATA_SORT_KEY_MAPPING,
				order);
		if ( idx > 0 ) {
			buf.append("\nORDER BY ");
			buf.append(order.substring(idx));
		}
		return buf.toString();
	}

	private PreparedStatement createStatement(Connection con, String sql) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = 0;
		if ( filter.hasLocalDateRange() ) {
			p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, con, stmt, p);
		} else {
			p = DatumSqlUtils.prepareDateRangeFilter(filter, con, stmt, p);
		}
		DatumSqlUtils.prepareObjectMetadataFilter(filter, con, stmt, p);
		return stmt;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return createStatement(con, getSql());
	}

}
