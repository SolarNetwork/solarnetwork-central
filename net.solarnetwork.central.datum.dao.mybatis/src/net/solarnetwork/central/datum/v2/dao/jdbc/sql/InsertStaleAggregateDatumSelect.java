/* ==================================================================
 * InsertStaleAggregateDatumSelect.java - 25/11/2020 2:53:43 pm
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.AggregationCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Insert records into the {@literal solardatm.agg_stale_datm} table that match
 * the result of a query for existing data rows in the
 * {@literal solardatm.da_datm} table.
 * 
 * <p>
 * This pattern supports the "manually mark data as stale" for aggregate
 * processing.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class InsertStaleAggregateDatumSelect implements PreparedStatementCreator, SqlProvider {

	private final DatumStreamCriteria filter;
	private final Aggregation aggregation;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search filter
	 */
	public InsertStaleAggregateDatumSelect(DatumStreamCriteria filter) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument not be null.");
		}
		if ( !filter.hasLocalDateRange()
				&& (filter.getStartDate() == null || filter.getEndDate() == null) ) {
			throw new IllegalArgumentException("A date range must be specified.");
		}
		this.filter = filter;
		this.aggregation = aggregation(filter);
	}

	private static Aggregation aggregation(AggregationCriteria filter) {
		Aggregation agg = Aggregation.Hour;
		if ( filter.getAggregation() != null ) {
			switch (filter.getAggregation()) {
				case Day:
				case Month:
					agg = filter.getAggregation();
					break;

				default:
					// ignore
			}
		}
		return agg;
	}

	private void sqlInsert(StringBuilder buf) {
		buf.append("INSERT INTO solardatm.agg_stale_datm (stream_id, ts_start, agg_kind)\n");
	}

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT ");
		sqlColumns(buf);
	}

	private void sqlColumns(StringBuilder buf) {
		buf.append("datum.stream_id,\n");
		buf.append("datum.ts_start,\n");
		buf.append("'").append(aggregation.getKey() + "' AS agg_kind\n");
	}

	private void sqlFrom(StringBuilder buf) {
		buf.append("FROM s\n");
		buf.append("INNER JOIN solardatm.find_datm_");
		switch (aggregation) {
			case Day:
				buf.append("days");
				break;

			case Month:
				buf.append("months");
				break;

			default:
				buf.append("hours");
		}
		buf.append("(s.stream_id, ");
		if ( filter.hasLocalDateRange() ) {
			buf.append("? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone");
		} else {
			buf.append("?, ?");
		}
		buf.append(") datum ON TRUE\n");
	}

	private void sqlCore(StringBuilder buf) {
		sqlCte(buf);
		sqlSelect(buf);
		sqlFrom(buf);
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlInsert(buf);
		sqlCore(buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		if ( filter.hasLocalDateRange() ) {
			p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, con, stmt, p);
		} else {
			p = DatumSqlUtils.prepareDateRangeFilter(filter, con, stmt, p);
		}
		return p;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		prepareCore(con, stmt, 0);
		return stmt;
	}

}
