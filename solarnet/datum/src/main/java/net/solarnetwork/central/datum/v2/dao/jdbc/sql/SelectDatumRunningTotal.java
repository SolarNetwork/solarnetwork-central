/* ==================================================================
 * SelectDatumRunningTotal.java - 10/12/2020 6:33:07 am
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.orderBySorts;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Select for {@link AggregateDatum} instances for
 * {@link Aggregation#RunningTotal} aggregation via a {@link DatumCriteria}
 * filter.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectDatumRunningTotal implements PreparedStatementCreator, SqlProvider {

	private final DatumCriteria filter;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 */
	public SelectDatumRunningTotal(DatumCriteria filter) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
	}

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		if ( filter.getObjectKind() == ObjectDatumKind.Location ) {
			DatumSqlUtils.locationMetadataFilterSql(filter,
					filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
							: DatumSqlUtils.MetadataSelectStyle.Minimum,
					buf);
		} else {
			DatumSqlUtils.nodeMetadataFilterSql(filter,
					filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
							: DatumSqlUtils.MetadataSelectStyle.Minimum,
					buf);
		}
		buf.append(")\n");
		if ( !filter.hasDateOrLocalDateRange() ) {
			// query based on epoch - latest available
			buf.append(", r AS (\n");
			buf.append("	SELECT s.stream_id, MAX(latest.ts_start) AS ts_max\n");
			buf.append("	FROM s\n");
			buf.append("		, unnest(ARRAY['h','d','M']) AS agg\n");
			buf.append("		, solardatm.find_agg_time_greatest(s.stream_id, agg.agg) latest\n");
			buf.append("	GROUP BY s.stream_id\n");
			buf.append(")\n");
		}
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT datum.stream_id,\n");
		buf.append("	CURRENT_TIMESTAMP AS ts,\n");
		DatumSqlUtils.rollupAggDataSql(buf);
	}

	private void sqlFrom(StringBuilder buf) {
		buf.append("FROM ");
		if ( filter.hasDateOrLocalDateRange() ) {
			buf.append("s");
		} else {
			buf.append("r");
		}
		buf.append(", solardatm.find_agg_datm_running_total(");
		if ( filter.hasDateOrLocalDateRange() ) {
			buf.append("s");
		} else {
			buf.append("r");
		}
		buf.append(".stream_id, ");
		if ( filter.hasLocalDateRange() ) {
			buf.append("? ").append(SQL_AT_STREAM_METADATA_TIME_ZONE);
			buf.append(", ? ").append(SQL_AT_STREAM_METADATA_TIME_ZONE);
		} else if ( filter.hasDateRange() ) {
			buf.append("?, ?");
		} else {
			// start date will be epoch
			buf.append("?, r.ts_max");
		}
		buf.append(") datum\n");
		buf.append("GROUP BY datum.stream_id\n");
	}

	private void sqlCore(StringBuilder buf, boolean ordered) {
		final boolean metaSort = DatumSqlUtils.hasMetadataSortKey(filter.getSorts());
		sqlCte(buf);
		if ( ordered && metaSort ) {
			buf.append(", datum AS (\n");
		}
		sqlSelect(buf);
		sqlFrom(buf);
		if ( ordered && metaSort ) {
			buf.append(")\n");
			buf.append("SELECT datum.*\n");
			buf.append("FROM datum\n");
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		StringBuilder order = new StringBuilder();
		int idx = 2;
		if ( filter.hasSorts() ) {
			idx = orderBySorts(filter.getSorts(),
					filter.getLocationId() != null ? DatumSqlUtils.LOCATION_STREAM_SORT_KEY_MAPPING
							: DatumSqlUtils.NODE_STREAM_SORT_KEY_MAPPING,
					order);
		} else {
			order.append(", datum.stream_id");
		}
		if ( order.length() > 0 ) {
			buf.append("ORDER BY ").append(order.substring(idx));
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf, true);
		if ( DatumSqlUtils.hasMetadataSortKey(filter.getSorts()) ) {
			buf.append("INNER JOIN s ON s.stream_id = datum.stream_id\n");
		}
		sqlOrderBy(buf);
		DatumSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = prepareCore(con, stmt, 0);
		DatumSqlUtils.preparePaginationFilter(filter, con, stmt, p);
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		if ( filter.hasLocalDateRange() ) {
			p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, con, stmt, p);
		} else if ( filter.hasDateRange() ) {
			p = DatumSqlUtils.prepareDateRangeFilter(filter, con, stmt, p);
		} else {
			// set start date to epoch, end date will be stream max date
			stmt.setTimestamp(++p, Timestamp.from(Instant.EPOCH));
		}
		return p;
	}

}
