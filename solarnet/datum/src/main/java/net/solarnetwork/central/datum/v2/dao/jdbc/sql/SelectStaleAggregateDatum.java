/* ==================================================================
 * SelectStaleAggregateDatum.java - 23/11/2020 8:51:47 am
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
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Select for {@link StaleAuditDatum} instances via a
 * {@link DatumStreamCriteria} filter.
 *
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public final class SelectStaleAggregateDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private final DatumStreamCriteria filter;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectStaleAggregateDatum(DatumStreamCriteria filter) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT datum.stream_id,\n");
		buf.append("	datum.ts_start AS ts,\n");
		buf.append("	datum.agg_kind,\n");
		buf.append("	datum.created\n");
	}

	private void sqlCte(StringBuilder buf) {
		if ( !filter.hasDatumMetadataCriteria() ) {
			return;
		}
		buf.append("WITH s AS (\n");
		if ( filter.getLocationId() != null ) {
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
	}

	private void sqlFrom(StringBuilder buf) {
		if ( filter.hasDatumMetadataCriteria() ) {
			buf.append("FROM s\n");
			buf.append("INNER JOIN solardatm.agg_stale_datm datum ON datum.stream_id = s.stream_id\n");
		} else {
			buf.append("FROM solardatm.agg_stale_datm datum\n");
		}
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.getAggregation() != null ) {
			where.append("\tAND datum.agg_kind = ?\n");
			idx++;
		}
		if ( filter.hasLocalDateRange() ) {
			idx += DatumSqlUtils.whereLocalDateRange(filter, Aggregation.Hour,
					DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, where);
		} else {
			idx += DatumSqlUtils.whereDateRange(filter, Aggregation.Hour, where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		if ( filter == null ) {
			return;
		}

		StringBuilder order = new StringBuilder();
		int idx = 2;
		if ( filter.hasSorts() ) {
			idx = orderBySorts(filter.getSorts(), DatumSqlUtils.STALE_AGGREGATE_SORT_KEY_MAPPING, order);
		} else {
			order.append(", datum.agg_kind, ts, datum.stream_id");
		}
		if ( !order.isEmpty() ) {
			buf.append("ORDER BY ").append(order.substring(idx));
		}
	}

	private void sqlCore(StringBuilder buf) {
		sqlCte(buf);
		sqlSelect(buf);
		sqlFrom(buf);
		sqlWhere(buf);
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderBy(buf);
		CommonSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, con, stmt, p);
		if ( filter.getAggregation() != null ) {
			stmt.setString(++p, filter.getAggregation().getKey());
		}
		if ( filter.hasLocalDateRange() ) {
			p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, con, stmt, p);
		} else {
			p = DatumSqlUtils.prepareDateRangeFilter(filter, stmt, p);
		}
		return p;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = prepareCore(con, stmt, 0);
		DatumSqlUtils.preparePaginationFilter(filter, con, stmt, p);
		return stmt;
	}

	@Override
	public PreparedStatementCreator countPreparedStatementCreator() {
		return new CountPreparedStatementCreator();
	}

	private final class CountPreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		@Override
		public String getSql() {
			StringBuilder buf = new StringBuilder();
			sqlCore(buf);
			return DatumSqlUtils.wrappedCountQuery(buf.toString());
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			PreparedStatement stmt = con.prepareStatement(getSql());
			prepareCore(con, stmt, 0);
			return stmt;
		}

	}

}
