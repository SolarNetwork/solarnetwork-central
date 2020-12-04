/* ==================================================================
 * SelectDatum.java - 19/11/2020 8:23:34 pm
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
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils.timeColumnName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.CombiningConfig;
import net.solarnetwork.central.datum.v2.dao.CombiningIdsConfig;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Select for {@link DatumEntity} instances via a {@link DatumCriteria} filter.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private final DatumCriteria filter;
	private final Aggregation aggregation;
	private final CombiningConfig combine;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 */
	public SelectDatum(DatumCriteria filter) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument not be null.");
		}
		this.filter = filter;
		this.aggregation = (filter.getAggregation() != null ? filter.getAggregation()
				: Aggregation.None);
		if ( aggregation == Aggregation.Minute ) {
			throw new IllegalArgumentException(
					"The Minute aggregation is not supported; please query the data directly by omitting the aggregation criteria or using None.");
		}
		if ( filter.isMostRecent()
				&& !(aggregation == Aggregation.None || aggregation == Aggregation.Hour
						|| aggregation == Aggregation.Day || aggregation == Aggregation.Month) ) {
			throw new IllegalArgumentException(
					format("The mostRecent flag cannot be used with aggregation %s.", aggregation));
		}
		if ( isMinuteAggregation() && !filter.hasDateOrLocalDateRange() ) {
			throw new IllegalArgumentException(
					format("A date range must be specified for aggregation %s.", aggregation));
		}
		this.combine = CombiningConfig.configFromCriteria(filter);
	}

	private boolean isMinuteAggregation() {
		return (aggregation != Aggregation.None && aggregation.compareLevel(Aggregation.Hour) < 0);
	}

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
		if ( combine != null ) {
			for ( CombiningIdsConfig<?> conf : combine.getIdsConfigs() ) {
				buf.append(", ").append(conf.getName()).append("_mappings AS (\n");
				buf.append("	SELECT id, vid, ROW_NUMBER() OVER (ORDER BY 1) AS rank FROM (\n");
				buf.append("		SELECT unnest(ids) AS id, v.vid, s.n FROM (\n");
				buf.append("			SELECT ids, n\n");
				//                                                 conf.idSets 2d array
				buf.append("			FROM solarcommon.reduce_dim(?) WITH ORDINALITY AS m(ids,n)\n");
				buf.append("		) s\n");
				//                          conf.virtualIds 1d array
				buf.append("	JOIN unnest(?) WITH ORDINALITY AS v(vid,n) ON v.n = s.n\n");
				buf.append("	) os\n");
				buf.append(")\n");
			}
		}
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT ");
		sqlColumns(buf);
	}

	private void sqlColumns(StringBuilder buf) {
		buf.append("datum.stream_id,\n");
		if ( aggregation != Aggregation.None ) {
			buf.append("datum.ts_start AS ts,\n");
		} else {
			buf.append("datum.ts,\n");
			buf.append("datum.received,\n");
		}
		buf.append("datum.data_i,\n");
		buf.append("datum.data_a,\n");
		buf.append("datum.data_s,\n");
		buf.append("datum.data_t");
		if ( aggregation != Aggregation.None ) {
			buf.append(",\ndatum.stat_i,\n");
			if ( aggregation.compareLevel(Aggregation.Hour) < 0 ) {
				// reading data not available for minute aggregation
				buf.append("NULL::BIGINT[][] AS read_a\n");
			} else {
				buf.append("datum.read_a\n");
			}
		} else {
			buf.append("\n");
		}
	}

	protected String sqlTableName() {
		switch (aggregation) {
			case FiveMinute:
			case TenMinute:
			case FifteenMinute:
			case ThirtyMinute:
				return filter.hasLocalDateRange()
						? "solardatm.rollup_datm_for_time_span_slots(s.stream_id, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone, ?)"
						: "solardatm.rollup_datm_for_time_span_slots(s.stream_id, ?, ?, ?)";

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

	private void sqlFrom(StringBuilder buf) {
		buf.append("FROM s\n");
		if ( filter.isMostRecent() ) {
			buf.append("INNER JOIN LATERAL (\n");
			buf.append("		SELECT datum.*\n");
			buf.append("		FROM ").append(sqlTableName()).append(" datum\n");
			buf.append("		WHERE datum.stream_id = s.stream_id\n");
			buf.append("		ORDER BY datum.").append(timeColumnName(aggregation)).append(" DESC\n");
			buf.append("		LIMIT 1\n");
			buf.append("	) datum ON datum.stream_id = s.stream_id\n");
		} else {
			buf.append("INNER JOIN ").append(sqlTableName())
					.append(" datum ON datum.stream_id = s.stream_id\n");
		}
	}

	private void sqlWhere(StringBuilder buf) {
		if ( filter.isMostRecent() || isMinuteAggregation() ) {
			// date range not supported in MostRecent and not part of WHERE for *Minute aggregation
			return;
		}

		StringBuilder where = new StringBuilder();
		int idx = filter.hasLocalDateRange()
				? DatumSqlUtils.whereLocalDateRange(filter, aggregation,
						DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, where)
				: DatumSqlUtils.whereDateRange(filter, aggregation, where);
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
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
			order.append(", datum.stream_id, ts");
		}
		if ( order.length() > 0 ) {
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
		DatumSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		if ( filter.hasLocalDateRange() ) {
			p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, con, stmt, p);
		} else {
			p = DatumSqlUtils.prepareDateRangeFilter(filter, con, stmt, p);
		}
		if ( aggregation != Aggregation.None && aggregation.compareLevel(Aggregation.Hour) < 0 ) {
			// add "secs" parameter
			stmt.setObject(++p, aggregation.getLevel());
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
			if ( isMinuteAggregation() ) {
				// We use a specialized count query here because the actual query does a lot 
				// of computation to produce minute-level aggregation. The 
				// solardatm.count_datm_time_span_slots() function is designed to run faster.
				sqlCte(buf);
				buf.append("SELECT SUM(datum.dcount) AS dcount\n");
				buf.append("FROM s\n");
				buf.append("INNER JOIN solardatm.count_datm_time_span_slots(s.stream_id");
				if ( filter.hasLocalDateRange() ) {
					buf.append(", ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone");
				} else {
					buf.append(", ?, ?");
				}
				buf.append(", ?) datum(dcount) ON TRUE\n");
				return buf.toString();
			}

			// non-minute aggregation; use normal wrapped count query
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
