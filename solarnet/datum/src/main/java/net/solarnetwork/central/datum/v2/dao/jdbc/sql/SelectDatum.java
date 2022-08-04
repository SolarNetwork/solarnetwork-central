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
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.orderBySorts;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.timeColumnName;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.datum.v2.dao.CombiningConfig;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Select for {@link DatumEntity} instances via a {@link DatumCriteria} filter.
 * 
 * @author matt
 * @version 1.3
 * @since 3.8
 */
public class SelectDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/**
	 * The {@code fetchSize} property default value.
	 * 
	 * @since 1.2
	 */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final DatumCriteria filter;
	private final Aggregation aggregation;
	private final CombiningConfig combine;
	private final int fetchSize;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 */
	public SelectDatum(DatumCriteria filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param fetchSize
	 *        the row fetch size
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 * @since 1.2
	 */
	public SelectDatum(DatumCriteria filter, int fetchSize) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.aggregation = (filter.getAggregation() != null ? filter.getAggregation()
				: Aggregation.None);
		if ( aggregation == Aggregation.Minute ) {
			throw new IllegalArgumentException(
					"The Minute aggregation is not supported; please query the data directly by omitting the aggregation criteria or using None.");
		} else if ( aggregation == Aggregation.RunningTotal ) {
			throw new IllegalArgumentException(
					"The RunningTotal aggregation is not supported; use the SelectDatumRunningTotal class.");
		}
		if ( filter.isMostRecent()
				&& !(aggregation == Aggregation.None || aggregation == Aggregation.Hour
						|| aggregation == Aggregation.Day || aggregation == Aggregation.Month) ) {
			throw new IllegalArgumentException(
					format("The mostRecent flag cannot be used with aggregation %s.", aggregation));
		}
		if ( isDateOrLocalDateRangeRequired() && !filter.hasDateOrLocalDateRange() ) {
			throw new IllegalArgumentException(
					format("A date range must be specified for aggregation %s.", aggregation));
		}
		this.combine = CombiningConfig.configFromCriteria(filter);
		this.fetchSize = fetchSize;
	}

	private boolean isDateOrLocalDateRangeRequired() {
		return isMinuteAggregation();
	}

	private boolean isMinuteAggregation() {
		return (aggregation != Aggregation.None && aggregation.compareLevel(Aggregation.Hour) < 0);
	}

	private boolean isMetadataTimeZoneRequired() {
		return (filter.hasLocalDate() || isDefaultLocalDateRange()
				|| filter.getAggregation() == Aggregation.Week);
	}

	private void sqlCte(StringBuilder buf, boolean ordered) {
		buf.append("WITH ").append(combine != null ? "rs" : "s").append(" AS (\n");
		if ( filter.getObjectKind() == ObjectDatumKind.Location ) {
			DatumSqlUtils.locationMetadataFilterSql(filter,
					isMetadataTimeZoneRequired() ? DatumSqlUtils.MetadataSelectStyle.WithZone
							: DatumSqlUtils.MetadataSelectStyle.Minimum,
					combine, buf);
		} else {
			DatumSqlUtils.nodeMetadataFilterSql(filter,
					isMetadataTimeZoneRequired() ? DatumSqlUtils.MetadataSelectStyle.WithZone
							: DatumSqlUtils.MetadataSelectStyle.Minimum,
					combine, buf);
		}
		buf.append(")\n");
		if ( combine != null ) {
			buf.append(", s AS (\n");
			buf.append("	SELECT solardatm.virutal_stream_id(")
					.append(filter.getObjectKind() == ObjectDatumKind.Location ? "loc_id" : "node_id")
					.append(", source_id) AS vstream_id\n");
			buf.append("	, *\n");
			buf.append("	FROM rs\n");
			buf.append(")\n");
			buf.append(", vs AS (\n");
			buf.append("	SELECT DISTINCT ON (vstream_id) vstream_id, ")
					.append(filter.getObjectKind() == ObjectDatumKind.Location ? "loc_id" : "node_id")
					.append(", source_id\n");
			buf.append("	FROM s\n");
			buf.append(")\n");
		}
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT ");
		if ( combine != null ) {
			buf.append("s.vstream_id AS stream_id,\n");
			buf.append("	s.obj_rank,\n");
			buf.append("	s.source_rank,\n");
			buf.append("	s.names_i,\n");
			buf.append("	s.names_a,\n");
		} else {
			buf.append("	datum.stream_id,\n");
		}
		if ( combine != null && isMinuteAggregation() ) {
			buf.append("	ds.ts,\n");
			buf.append("	ds.data_i,\n");
			buf.append("	ds.data_a,\n");
			buf.append("	ds.data_s,\n");
			buf.append("	ds.data_t,\n");
			buf.append("	ds.stat_i,\n");
			buf.append("	ds.read_a\n");
			buf.append("FROM s\n");
			buf.append("INNER JOIN (\n");
			buf.append("	SELECT datum.stream_id,\n");
			buf.append("	datum.ts_start AS ts,\n");
			DatumSqlUtils.rollupAggDataSql(buf);
		} else {
			if ( aggregation == Aggregation.Week ) {
				buf.append(
						"	date_trunc('week', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone AS ts,\n");
			} else if ( aggregation != Aggregation.None ) {
				buf.append("	datum.ts_start AS ts,\n");
			} else {
				buf.append("	datum.ts,\n");
				buf.append("	datum.received,\n");
			}
			if ( aggregation == Aggregation.Week ) {
				DatumSqlUtils.rollupAggDataSql(buf);
			} else {
				buf.append("	datum.data_i,\n");
				buf.append("	datum.data_a,\n");
				buf.append("	datum.data_s,\n");
				buf.append("	datum.data_t");
				if ( aggregation != Aggregation.None ) {
					buf.append(",\n	datum.stat_i,\n");
					if ( isMinuteAggregation() ) {
						// reading data not available for minute aggregation
						buf.append("	NULL::BIGINT[][] AS read_a\n");
					} else {
						buf.append("	datum.read_a");
					}
				}
				buf.append("\n");
			}
		}
	}

	private boolean isDateRangeInJoin() {
		switch (aggregation) {
			case FiveMinute:
			case TenMinute:
			case FifteenMinute:
			case ThirtyMinute:
			case DayOfWeek:
			case SeasonalDayOfWeek:
			case HourOfDay:
			case SeasonalHourOfDay:
			case WeekOfYear:
				return true;

			default:
				return filter.isMostRecent();
		}
	}

	private boolean isDefaultLocalDateRange() {
		return !filter.hasDateOrLocalDateRange() && (filter.getAggregation() == Aggregation.DayOfWeek
				|| filter.getAggregation() == Aggregation.SeasonalDayOfWeek
				|| filter.getAggregation() == Aggregation.HourOfDay
				|| filter.getAggregation() == Aggregation.SeasonalHourOfDay
				|| filter.getAggregation() == Aggregation.WeekOfYear);
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
			case Week:
				return "solardatm.agg_datm_daily";

			case Month:
				return "solardatm.agg_datm_monthly";

			case DayOfWeek:
				return filter.hasLocalDateRange() || isDefaultLocalDateRange()
						? "solardatm.find_agg_datm_dow(s.stream_id, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
						: "solardatm.find_agg_datm_dow(s.stream_id, ?, ?)";

			case SeasonalDayOfWeek:
				return filter.hasLocalDateRange() || isDefaultLocalDateRange()
						? "solardatm.find_agg_datm_dow_seasonal(s.stream_id, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
						: "solardatm.find_agg_datm_dow_seasonal(s.stream_id, ?, ?)";

			case HourOfDay:
				return filter.hasLocalDateRange() || isDefaultLocalDateRange()
						? "solardatm.find_agg_datm_hod(s.stream_id, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
						: "solardatm.find_agg_datm_hod(s.stream_id, ?, ?)";

			case SeasonalHourOfDay:
				return filter.hasLocalDateRange() || isDefaultLocalDateRange()
						? "solardatm.find_agg_datm_hod_seasonal(s.stream_id, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
						: "solardatm.find_agg_datm_hod_seasonal(s.stream_id, ?, ?)";

			case WeekOfYear:
				return filter.hasLocalDateRange() || isDefaultLocalDateRange()
						? "solardatm.find_agg_datm_woy(s.stream_id, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
						: "solardatm.find_agg_datm_woy(s.stream_id, ?, ?)";

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
			if ( filter.hasDateOrLocalDate() ) {
				addSqlWhere(buf, true);
			}
			buf.append("		ORDER BY datum.").append(timeColumnName(aggregation)).append(" DESC\n");
			buf.append("		LIMIT 1\n");
			buf.append("	) datum ON datum.stream_id = s.stream_id\n");
		} else {
			buf.append("INNER JOIN ").append(sqlTableName())
					.append(" datum ON datum.stream_id = s.stream_id\n");
		}
	}

	private void sqlWhere(StringBuilder buf) {
		if ( isDateRangeInJoin() ) {
			return;
		}
		addSqlWhere(buf, false);
	}

	private void addSqlWhere(StringBuilder buf, boolean and) {
		StringBuilder where = new StringBuilder();
		int idx = filter.hasLocalDate()
				? DatumSqlUtils.whereLocalDateRange(filter, aggregation,
						DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, where)
				: DatumSqlUtils.whereDateRange(filter, aggregation, where);
		if ( idx > 0 ) {
			if ( and ) {
				buf.append(where);
			} else {
				buf.append("WHERE").append(where.substring(4));
			}
		}
	}

	private void sqlOrderByJoins(StringBuilder buf) {
		if ( combine == null ) {
			return;
		}
		buf.append("INNER JOIN vs ON vs.vstream_id = datum.stream_id\n");
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

	private void sqlCore(StringBuilder buf, boolean ordered) {
		sqlCte(buf, ordered);
		if ( combine != null ) {
			buf.append(", d AS (\n");
		}
		sqlSelect(buf);
		sqlFrom(buf);
		sqlWhere(buf);
		if ( aggregation == Aggregation.Week ) {
			buf.append(
					"GROUP BY datum.stream_id, date_trunc('week', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone\n");
		}
		if ( combine != null ) {
			if ( isMinuteAggregation() ) {
				buf.append("	GROUP BY datum.stream_id, ts\n");
				if ( combine != null ) {
					buf.append(") AS ds ON ds.stream_id = s.stream_id\n");
				}
			}
			buf.append(")\n");
			buf.append(VirtualDatumSqlUtils.combineCteSql(combine.getType())).append("\n");
			buf.append("SELECT datum.*, vs.")
					.append(filter.getObjectKind() == ObjectDatumKind.Location ? "loc_id" : "node_id")
					.append(", vs.source_id\n");
			buf.append("FROM datum\n");
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf, true);
		sqlOrderByJoins(buf);
		sqlOrderBy(buf);
		CommonSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, combine, con, stmt, p);
		if ( isDefaultLocalDateRange() ) {
			// currently this implies a DOW/HOD style query; default date range will be the past 2 years
			stmt.setObject(++p, LocalDate.now().minusYears(2).atStartOfDay(), Types.TIMESTAMP);
			stmt.setObject(++p, LocalDate.now().plusDays(1).atStartOfDay(), Types.TIMESTAMP);
		} else if ( filter.hasLocalDate() ) {
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
		if ( fetchSize > 0 ) {
			stmt.setFetchSize(fetchSize);
		}
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
				sqlCte(buf, false);
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
			sqlCore(buf, false);
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
