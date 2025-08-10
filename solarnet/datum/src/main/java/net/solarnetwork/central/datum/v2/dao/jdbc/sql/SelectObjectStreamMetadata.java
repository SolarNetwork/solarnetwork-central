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

import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.orderBySorts;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.MetadataSelectStyle;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.SearchFilter;

/**
 * Generate dynamic SQL for a "find stream metadata" query.
 *
 * @author matt
 * @version 2.2
 * @since 3.8
 */
public final class SelectObjectStreamMetadata implements PreparedStatementCreator, SqlProvider {

	private static final String SQL_AT_LOCATION_TIME_ZONE = "AT TIME ZONE COALESCE(l.time_zone, 'UTC')";

	private final ObjectStreamCriteria filter;
	private final ObjectDatumKind kind;
	private final SearchFilter searchFilter;
	private final MetadataSelectStyle style;

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
	 * @param kind
	 *        the datum kind
	 * @throws IllegalArgumentException
	 *         if {@code filter} or {@code kind} are {@literal null}
	 */
	public SelectObjectStreamMetadata(ObjectStreamCriteria filter, ObjectDatumKind kind) {
		this(filter, kind, null);
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @param kind
	 *        the datum kind
	 * @param style
	 *        the select style
	 * @throws IllegalArgumentException
	 *         if {@code filter} or {@code kind} are {@literal null}
	 */
	public SelectObjectStreamMetadata(ObjectStreamCriteria filter, ObjectDatumKind kind,
			MetadataSelectStyle style) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.kind = requireNonNullArgument(kind, "kind");
		this.style = (style != null ? style
				: filter.hasLocationCriteria() ? MetadataSelectStyle.WithGeography
						: MetadataSelectStyle.Full);
		this.searchFilter = filter.searchFilter();
	}

	@Override
	public String getSql() {
		final boolean withNameFts = (kind == ObjectDatumKind.Location
				&& style == MetadataSelectStyle.WithGeography && filter.hasLocationCriteria()
				&& filter.getLocation().getName() != null);

		StringBuilder buf = new StringBuilder();
		int idx;
		if ( withNameFts ) {
			buf.append("SELECT * FROM (\n");
		}
		if ( kind == ObjectDatumKind.Location ) {
			idx = DatumSqlUtils.locationMetadataFilterSql(filter, style, filter, "solardatm.da_datm",
					Aggregation.None, null, SQL_AT_LOCATION_TIME_ZONE, buf);
		} else {
			idx = DatumSqlUtils.nodeMetadataFilterSql(filter, style, filter, "solardatm.da_datm",
					Aggregation.None, null, SQL_AT_LOCATION_TIME_ZONE, buf);
		}
		// only allowing searchFilter if some other criteria specified as well
		if ( idx > 0 && searchFilter != null ) {
			DatumSqlUtils.metadataSearchFilterSql(searchFilter, buf);
		}

		if ( withNameFts ) {
			// add metadata rows that FTS match on stream metadata (in addition to location metadata above)
			buf.append("\nUNION\n\n");
			var tmp = new StringBuilder();
			DatumSqlUtils.locationMetadataFilterSql(filter, style, filter, "solardatm.da_datm",
					Aggregation.None, null, SQL_AT_LOCATION_TIME_ZONE, tmp);
			buf.append(tmp.toString().replace("l.fts_default", "s.fts_default"));
			if ( searchFilter != null ) {
				DatumSqlUtils.metadataSearchFilterSql(searchFilter, buf);
			}
			buf.append(") s\n");
		}

		StringBuilder order = new StringBuilder();
		idx = orderBySorts(filter.getSorts(),
				kind == ObjectDatumKind.Location
						? DatumSqlUtils.LOCATION_STREAM_METADATA_SORT_KEY_MAPPING
						: DatumSqlUtils.NODE_STREAM_METADATA_SORT_KEY_MAPPING,
				order);
		if ( idx > 0 ) {
			buf.append("ORDER BY ");
			buf.append(order.substring(idx));
		}
		return buf.toString();
	}

	private PreparedStatement createStatement(Connection con, String sql) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		final boolean withNameFts = (kind == ObjectDatumKind.Location
				&& style == MetadataSelectStyle.WithGeography && filter.hasLocationCriteria()
				&& filter.getLocation().getName() != null);

		int p = 0;
		if ( filter.hasLocalDateRange() ) {
			p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, stmt, p);
		} else {
			p = DatumSqlUtils.prepareDateRangeFilter(filter, stmt, p);
		}
		p = DatumSqlUtils.prepareObjectMetadataFilter(filter, kind, con, stmt, p);
		if ( p > 0 && searchFilter != null ) {
			p = DatumSqlUtils.prepareMetadataSearchFilter(searchFilter, con, stmt, p);
		}

		if ( withNameFts ) {
			stmt.setString(++p, filter.getLocation().getName());
			if ( searchFilter != null ) {
				DatumSqlUtils.prepareMetadataSearchFilter(searchFilter, con, stmt, p);
			}
		}

		return stmt;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return createStatement(con, getSql());
	}

}
