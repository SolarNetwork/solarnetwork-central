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
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.orderBySorts;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.CombiningConfig;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.LocalDateInterval;
import net.solarnetwork.central.datum.v2.domain.PartialAggregationInterval;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Select for {@link DatumEntity} instances via a {@link DatumCriteria} filter
 * using partial aggregation ranges.
 *
 * <p>
 * Special handling is provided when the main aggregation is {@code Year}. Since
 * there is no yearly aggregation table to query from, the monthly aggregation
 * table will be queried and aggregated to year values.
 * </p>
 *
 * @author matt
 * @version 1.2
 * @since 3.8
 */
public final class SelectDatumPartialAggregate
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private final DatumCriteria filter;
	private final Aggregation aggregation;
	private final CombiningConfig combine;
	private final DatumRollupType rollup;
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
		this(filter, filter.getPartialAggregation());
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the search criteria
	 * @param partial
	 *        the partial aggregation to use
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 */
	public SelectDatumPartialAggregate(DatumCriteria filter, Aggregation partial) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument must not be null.");
		}
		if ( partial == null ) {
			throw new IllegalArgumentException("The partial aggregation argument must not be null.");
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
		if ( aggregation == Aggregation.Year && partial.compareLevel(Aggregation.Day) < 0 ) {
			throw new IllegalArgumentException(format(
					"%s partial aggregation is too small to use with Year aggregation.", partial));
		}
		this.combine = CombiningConfig.configFromCriteria(filter);

		// support the All rollup
		if ( filter.hasDatumRollupCriteria() ) {
			if ( this.combine != null ) {
				throw new IllegalArgumentException("Virtual combinations are not suported with rollup.");
			}
			if ( filter.getDatumRollupType() == DatumRollupType.All ) {
				this.rollup = filter.getDatumRollupType();
			} else {
				throw new IllegalArgumentException("Only the `All` DatumRollupType is supported.");
			}
		} else {
			this.rollup = null;
		}

		List<LocalDateInterval> intervals;
		if ( this.rollup != null && this.aggregation == Aggregation.Year ) {
			// this works out to a simple query across months
			intervals = List.of(new LocalDateInterval(start, end, Aggregation.Month));
		} else {
			PartialAggregationInterval partialInterval = new PartialAggregationInterval(aggregation,
					partial, start, end);
			if ( partialInterval.getIntervals().isEmpty() ) {
				throw new IllegalArgumentException("Invalid date range for partial aggregation.");
			}
			intervals = partialInterval.getIntervals();
		}
		this.intervalFilters = intervals.stream().map(e -> {
			BasicDatumCriteria f = BasicDatumCriteria.copy(filter);
			f.setAggregation(e.getAggregation());
			f.setLocalStartDate(e.getStart());
			f.setLocalEndDate(e.getEnd());
			return f;
		}).collect(Collectors.toList());
	}

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH ").append(combine != null ? "rs" : "s").append(" AS (\n");
		if ( filter.getObjectKind() == ObjectDatumKind.Location ) {
			DatumSqlUtils.locationMetadataFilterSql(filter, DatumSqlUtils.MetadataSelectStyle.WithZone,
					combine, buf);
		} else {
			DatumSqlUtils.nodeMetadataFilterSql(filter, DatumSqlUtils.MetadataSelectStyle.WithZone,
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

	private static String sqlAgg(Aggregation agg) {
		return switch (agg) {
			case Year -> "year";
			case Month -> "month";
			case Day -> "day";
			default -> "hour";
		};
	}

	private void sqlSelect(DatumCriteria filter, StringBuilder buf) {
		buf.append("SELECT ");
		if ( combine != null ) {
			buf.append("s.vstream_id AS stream_id,\n");
			buf.append("	s.obj_rank,\n");
			buf.append("	s.source_rank,\n");
			buf.append("	s.names_i,\n");
			buf.append("	s.names_a,\n");
		} else {
			buf.append("datum.stream_id,\n");
		}
		if ( rollup != null
				|| (filter.getAggregation() == aggregation && aggregation != Aggregation.Year) ) {
			// main agg: direct results
			buf.append("	datum.ts_start AS ts,\n");
			buf.append("	datum.data_i,\n");
			buf.append("	datum.data_a,\n");
			buf.append("	datum.data_s,\n");
			buf.append("	datum.data_t,\n");
			buf.append("	datum.stat_i,\n");
			buf.append("	datum.read_a\n");
		} else {
			// partial agg: dynamic rollup to main agg
			if ( combine != null ) {
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
			}
			if ( rollup == null ) {
				buf.append("	date_trunc('").append(sqlAgg(aggregation)).append(
						"', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone AS ts,\n");
				DatumSqlUtils.rollupAggDataSql(buf);
			}
		}
	}

	private void sqlFrom(DatumCriteria filter, StringBuilder buf) {
		buf.append("FROM s\n");
		buf.append("INNER JOIN ").append(sqlTableName(filter))
				.append(" datum ON datum.stream_id = s.stream_id\n");
	}

	private String sqlTableName(DatumCriteria filter) {
		return switch (filter.getAggregation()) {
			case Hour -> "solardatm.agg_datm_hourly";
			case Day -> "solardatm.agg_datm_daily";
			case Month, Year -> "solardatm.agg_datm_monthly";
			default -> "solardatm.da_datm";
		};
	}

	private void sqlWhere(DatumCriteria filter, StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		DatumSqlUtils.whereLocalDateRange(filter, filter.getAggregation(),
				DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, where);
		buf.append("WHERE").append(where.substring(4));
		if ( filter.getAggregation() != aggregation || aggregation == Aggregation.Year ) {
			if ( rollup == null ) {
				buf.append("GROUP BY datum.stream_id, date_trunc('").append(sqlAgg(aggregation)).append(
						"', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone\n");
				// partial aggregation can produce NULL output; omit those
				buf.append("HAVING COUNT(*) > 0\n");
			}
			if ( combine != null ) {
				buf.append(") AS ds ON ds.stream_id = s.stream_id\n");
			}
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
		if ( !order.isEmpty() ) {
			buf.append("ORDER BY ").append(order.substring(idx));
		}
	}

	private void sqlCore(StringBuilder buf) {
		sqlCte(buf);

		// overall rollup
		if ( rollup != null ) {
			buf.append("""
					SELECT rlp.stream_id
						, MIN(rlp.ts) AS ts_start
						, MAX(rlp.ts) AS ts_end
						, (solardatm.rollup_agg_data(
								(rlp.data_i
								, rlp.data_a
								, rlp.data_s
								, rlp.data_t
								, rlp.stat_i
								, rlp.read_a)::solardatm.agg_data
							ORDER BY rlp.ts)).*
					FROM (
					""");
		}

		// write main queries in CTE
		if ( rollup == null ) {
			buf.append(", ").append(combine != null ? "d" : "datum").append(" AS (\n");
		}

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
		if ( rollup == null ) {
			buf.append(")\n");
		}
		if ( combine != null ) {
			buf.append(VirtualDatumSqlUtils.combineCteSql(combine.getType())).append("\n");
		}
		if ( rollup == null ) {
			buf.append("SELECT datum.*");
		}
		if ( combine != null ) {
			buf.append(", vs.")
					.append(filter.getObjectKind() == ObjectDatumKind.Location ? "loc_id" : "node_id")
					.append(", vs.source_id");

		}
		if ( rollup == null ) {
			buf.append("\nFROM datum\n");
		}
	}

	private void sqlOrderByJoins(StringBuilder buf) {
		if ( combine == null && !DatumSqlUtils.hasMetadataSortKey(filter.getSorts()) ) {
			return;
		}
		buf.append("INNER JOIN ");
		if ( combine != null ) {
			buf.append("vs ON vs.v");
		} else {
			buf.append("s ON s.");
		}
		buf.append("stream_id = datum.stream_id\n");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderByJoins(buf);
		if ( rollup != null ) {
			buf.append("""
					) rlp
					GROUP BY rlp.stream_id
					ORDER BY rlp.stream_id
					""");
		} else {
			sqlOrderBy(buf);
		}
		CommonSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, combine, con, stmt, p);
		for ( DatumCriteria intervalFilter : intervalFilters ) {
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
