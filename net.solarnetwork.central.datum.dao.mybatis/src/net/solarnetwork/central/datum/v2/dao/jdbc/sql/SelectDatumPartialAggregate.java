/* ==================================================================
 * SelectDatumPartialAggregate.java - 3/12/2020 4:23:00 pm
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils.orderBySorts;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;
import net.solarnetwork.central.datum.v2.domain.PartialAggregationInterval;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Select for {@link DatumEntity} instances via a {@link DatumCriteria} filter
 * using partial aggregation ranges.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectDatumPartialAggregate
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private final DatumCriteria filter;
	private final Aggregation aggregation;
	private final PartialAggregationInterval partialInterval;
	private final List<DatumCriteria> intervalFilters;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 */
	public SelectDatumPartialAggregate(DatumCriteria filter) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument not be null.");
		}
		this.filter = filter;
		this.aggregation = (filter.getAggregation() != null ? filter.getAggregation()
				: Aggregation.None);
		if ( !(aggregation == Aggregation.Hour || aggregation == Aggregation.Day
				|| aggregation == Aggregation.Month || aggregation == Aggregation.Year) ) {
			throw new IllegalArgumentException(
					format("Partial aggregation cannot be used with aggregation %s.", aggregation));
		}
		LocalDateTime start = filter.getLocalStartDate();
		if ( start == null && filter.getStartDate() != null ) {
			start = filter.getStartDate().atOffset(ZoneOffset.UTC).toLocalDateTime();
		}
		LocalDateTime end = filter.getLocalEndDate();
		if ( end == null && filter.getEndDate() != null ) {
			end = filter.getEndDate().atOffset(ZoneOffset.UTC).toLocalDateTime();
		}
		if ( start == null || end == null ) {
			throw new IllegalArgumentException(
					"A date range must be specified for partial aggregation.");
		}
		this.partialInterval = new PartialAggregationInterval(aggregation,
				filter.getPartialAggregation(), start, end);
		if ( partialInterval.getIntervals().isEmpty() ) {
			throw new IllegalArgumentException("Invalid date range for partial aggregation.");
		}
		this.intervalFilters = partialInterval.getIntervals().stream().map(e -> {
			BasicDatumCriteria f = BasicDatumCriteria.copy(filter);
			f.setAggregation(e.getAggregation());
			f.setLocalStartDate(e.getStart());
			f.setLocalEndDate(e.getEnd());
			return f;
		}).collect(Collectors.toList());
	}

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
	}

	private void sqlSelect(DatumCriteria filter, StringBuilder buf) {
		buf.append("SELECT ");
		if ( filter.getAggregation() == aggregation ) {
			// main agg: direct results
			buf.append("datum.stream_id,\n");
			buf.append("datum.ts_start,\n");
			buf.append("datum.data_i,\n");
			buf.append("datum.data_a,\n");
			buf.append("datum.data_s,\n");
			buf.append("datum.data_t,\n");
			buf.append("datum.stat_i,\n");
			buf.append("datum.read_a\n");
		} else {
			// partial agg: dynamic rollup to main agg
			buf.append("(solardatm.rollup_agg_datm(\n");
			buf.append("\t\t(datum.stream_id, datum.ts_start, datum.data_i, datum.data_a, datum.data_s");
			buf.append(", datum.data_t, datum.stat_i, datum.read_a)::solardatm.agg_datm\n");
			buf.append("\t\t, ? AT TIME ZONE s.time_zone  ORDER BY datum.ts_start)).*\n");
		}
	}

	private void sqlFrom(DatumCriteria filter, StringBuilder buf) {
		buf.append("FROM s\n");
		buf.append("INNER JOIN ").append(sqlTableName(filter))
				.append(" datum ON datum.stream_id = s.stream_id\n");
	}

	protected String sqlTableName(DatumCriteria filter) {
		switch (filter.getAggregation()) {
			case Hour:
				return "solardatm.agg_datm_hourly";

			case Day:
				return "solardatm.agg_datm_daily";

			case Month:
				return "solardatm.agg_datm_monthly";

			default:
				return "solardatm.da_datm";
		}
	}

	private void sqlWhere(DatumCriteria filter, StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		DatumSqlUtils.whereLocalDateRange(filter, filter.getAggregation(),
				DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, where);
		buf.append("WHERE").append(where.substring(4));
		if ( filter.getAggregation() != aggregation ) {
			// partial aggregation can produce NULL output; omit those
			buf.append("HAVING COUNT(*) > 0\n");
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
			order.append(", datum.stream_id, ts_start");
		}
		if ( order.length() > 0 ) {
			buf.append("ORDER BY ").append(order.substring(idx));
		}
	}

	private void sqlCore(StringBuilder buf) {
		sqlCte(buf);

		// write main queries in CTE
		buf.append(", datum AS (\n");

		boolean multi = false;
		for ( DatumCriteria intervalFilter : intervalFilters ) {
			if ( multi ) {
				buf.append("UNION ALL\n");
			}
			sqlSelect(intervalFilter, buf);
			sqlFrom(intervalFilter, buf);
			sqlWhere(intervalFilter, buf);
			multi = true;
		}

		buf.append(")\n");
		buf.append("SELECT * FROM datum\n");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderBy(buf);
		DatumSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		for ( DatumCriteria intervalFilter : intervalFilters ) {
			if ( intervalFilter.getAggregation() != aggregation ) {
				// set partial aggregation effective date, which is start of main aggregation period
				LocalDateTime aggDate = DatumUtils.truncateDate(intervalFilter.getLocalStartDate(),
						aggregation);
				stmt.setObject(++p, aggDate, Types.TIMESTAMP);
			}
			p = DatumSqlUtils.prepareLocalDateRangeFilter(intervalFilter, con, stmt, p);
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
