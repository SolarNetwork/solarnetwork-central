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
import static java.time.ZoneOffset.UTC;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.LOCATION_STREAM_METADATA_SORT_KEY_MAPPING;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.NODE_STREAM_METADATA_SORT_KEY_MAPPING;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.datumStreamSortMapping;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.orderBySorts;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.timeColumnName;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Location;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.datum.v2.dao.CombiningConfig;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.dao.jdbc.CommonSqlUtils;
import net.solarnetwork.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Select for {@link DatumEntity} instances via a {@link DatumCriteria} filter.
 *
 * <p>
 * When the criteria has a maximum result count and the rows of each stream are
 * ordered by time, the limit can be applied to each stream in a lateral join,
 * ordered by time alone, before the overall ordering and limit. TimescaleDB can
 * then stop reading each stream at the limit, however the statement is planned.
 * A plain join often does not: on a large database PostgreSQL was seen to read
 * every row of the stream in the date range, and all the stream metadata in
 * stream order, and sort them, both when it planned the statement with its
 * parameter values and when it used a generic plan. This per-stream limit is
 * used when it cannot read more rows than the plain join: when the criteria can
 * match only one stream, or when the results are sorted by time first (as a
 * plain join must then read every row of every stream in the date range before
 * sorting them). For criteria that can match many streams sorted by stream
 * first, a plain join can stop after the first stream, so the plain join is
 * kept.
 * </p>
 *
 * @author matt
 * @version 1.10
 * @since 3.8
 */
public final class SelectDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/**
	 * The {@code fetchSize} property default value.
	 *
	 * @since 1.2
	 */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final DatumCriteria filter;
	private final Aggregation aggregation;
	private final boolean aliased;
	private final String metaStreamIdColumnName;
	private final @Nullable CombiningConfig combine;
	private final @Nullable DatumRollupType rollup;
	private final int fetchSize;
	private final boolean contributors;
	private final StreamLimit streamLimit;

	/** The time order of a limit applied to each stream. */
	private enum StreamLimit {

		/** No per-stream limit. */
		None,

		/** Limit each stream, ordered by time ascending. */
		Ascending,

		/** Limit each stream, ordered by time descending. */
		Descending,

	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@code null} or invalid
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
	 *         if {@code filter} is {@code null} or invalid
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

		this.fetchSize = fetchSize;
		this.aliased = (filter.includeStreamAliases() && filter.getObjectKind() != Location);
		this.metaStreamIdColumnName = (aliased ? "s.orig_stream_id" : "s.stream_id");
		this.contributors = (combine != null && filter.getObjectKind() != Location);
		this.streamLimit = streamLimit(filter, aggregation, combine, rollup);
	}

	/**
	 * Decide if, and in what time order, to apply the limit to each stream.
	 *
	 * <p>
	 * Each stream then provides at most offset + max rows, so the overall
	 * ordering, offset and limit return the same rows as a plain join only if
	 * the requested ordering, within one stream, is by time in one direction.
	 * Sort keys that map to stream metadata (stream, node, location, source)
	 * are constant within a stream, so they may come before the time key.
	 * </p>
	 *
	 * @param filter
	 *        the criteria
	 * @param aggregation
	 *        the aggregation
	 * @param combine
	 *        the virtual stream combining configuration
	 * @param rollup
	 *        the rollup type
	 * @return the per-stream limit style, never {@code null}
	 */
	private static StreamLimit streamLimit(DatumCriteria filter, Aggregation aggregation,
			@Nullable CombiningConfig combine, @Nullable DatumRollupType rollup) {
		// the most recent query already limits each stream to one row; virtual streams are combined
		// from all the rows of their real streams, and a rollup reads every row
		if ( filter.isMostRecent() || combine != null || rollup != null ) {
			return StreamLimit.None;
		}
		// only the tables joined directly to the stream metadata; the other aggregations query
		// functions given the date range, or GROUP BY the Week
		if ( !(aggregation == Aggregation.None || aggregation == Aggregation.Hour
				|| aggregation == Aggregation.Day || aggregation == Aggregation.Month) ) {
			return StreamLimit.None;
		}
		final Integer max = filter.getMax();
		if ( max == null || max < 1 ) {
			return StreamLimit.None;
		}
		// find the time direction exactly as sqlOrderBy() renders the sorts
		final List<SortDescriptor> filterSorts = filter.getSorts();
		final List<SortDescriptor> sorts = (filterSorts != null ? filterSorts
				: DatumSqlUtils.SORTS_BY_STREAM_TIME);
		final boolean location = (filter.getLocationId() != null);
		final Map<String, String> sortMapping = datumStreamSortMapping(location ? Location : Node,
				aggregation);
		final Map<String, String> metaSortMapping = (location ? LOCATION_STREAM_METADATA_SORT_KEY_MAPPING
				: NODE_STREAM_METADATA_SORT_KEY_MAPPING);
		final String timeColumnName = timeColumnName(aggregation);
		boolean timeFirst = true;
		for ( SortDescriptor sort : sorts ) {
			final String sortKey = nonEmptyString(sort.getSortKey());
			if ( sortKey == null ) {
				continue;
			}
			final String sqlName = nonEmptyString(sortMapping.get(sortKey));
			if ( sqlName == null ) {
				// not included in the ORDER BY
				continue;
			}
			if ( timeColumnName.equals(sqlName) ) {
				// when sorted by time first, a plain join must read every row of every stream in
				// the date range before sorting, so the per-stream limit cannot read more; otherwise
				// a plain join can stop after the first stream, so limit each stream only if just
				// one can match
				if ( timeFirst || isSingleStream(filter) ) {
					return (sort.isDescending() ? StreamLimit.Descending : StreamLimit.Ascending);
				}
				return StreamLimit.None;
			}
			if ( !metaSortMapping.containsKey(sortKey) ) {
				// not known to be constant within a stream
				return StreamLimit.None;
			}
			timeFirst = false;
		}
		// no time ordering within a stream
		return StreamLimit.None;
	}

	/**
	 * Test if a criteria can match at most one stream.
	 *
	 * <p>
	 * This is true for one stream ID, or for one node or location ID with one
	 * source ID that is not a pattern, as the stream metadata (including the
	 * aliased views) has one stream per object and source ID.
	 * </p>
	 *
	 * @param filter
	 *        the criteria
	 * @return {@code true} if the metadata filter can match at most one stream
	 */
	private static boolean isSingleStream(DatumCriteria filter) {
		final UUID[] streamIds = filter.getStreamIds();
		if ( streamIds != null && streamIds.length == 1 ) {
			return true;
		}
		final Long[] objectIds = filter.getObjectIds();
		final String[] sourceIds = filter.getSourceIds();
		return (objectIds != null && objectIds.length == 1 && sourceIds != null && sourceIds.length == 1
				&& sourceIds[0] != null && !DatumUtils.WILDCARD_PATTERN_MATCHER.isPattern(sourceIds[0]));
	}

	/**
	 * Get the per-stream limit, which is the offset plus maximum result count.
	 *
	 * @return the limit
	 */
	private long streamLimitMax() {
		final Integer max = filter.getMax();
		final Long offset = filter.getOffset();
		long result = (max != null ? max : 0L);
		if ( offset != null && offset > 0 ) {
			result = (offset > Long.MAX_VALUE - result ? Long.MAX_VALUE : result + offset);
		}
		return result;
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

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH ").append(combine != null ? "rs" : "s").append(" AS (\n");
		if ( filter.getObjectKind() == Location ) {
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
					.append(filter.getObjectKind() == Location ? "loc_id" : "node_id")
					.append(", source_id) AS vstream_id\n");
			buf.append("	, *\n");
			buf.append("	FROM rs\n");
			buf.append(")\n");
			buf.append(", vs AS (\n");
			buf.append("	SELECT DISTINCT ON (vstream_id) vstream_id, ")
					.append(filter.getObjectKind() == Location ? "loc_id" : "node_id")
					.append(", source_id\n");
			buf.append("	FROM s\n");
			buf.append(")\n");
		}
	}

	private void sqlSelect(StringBuilder buf) {
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
		buf.append("SELECT ");
		if ( combine != null ) {
			buf.append("s.vstream_id AS stream_id,\n");
			buf.append("	s.obj_rank,\n");
			buf.append("	s.source_rank,\n");
			buf.append("	s.names_i,\n");
			buf.append("	s.names_a,\n");
			if ( contributors ) {
				buf.append("	s.rnode_id,\n");
				buf.append("	s.rsource_id,\n");
			}
		} else if ( aliased ) {
			buf.append("s.stream_id,\n");
		} else {
			buf.append("datum.stream_id,\n");
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
						"	date_trunc('week', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone AS ts");
				if ( combine == null && rollup == null ) {
					buf.append("_start");
				}
				buf.append(",\n");
			} else if ( aggregation != Aggregation.None ) {
				buf.append("	datum.ts_start");
				if ( combine != null || rollup != null ) {
					buf.append(" AS ts");
				}
				buf.append(",\n");
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
		return isDateRangeFunction() || filter.isMostRecent();
	}

	/**
	 * Test if the aggregation queries a function given the date range as
	 * arguments, which uses the local dates only when both are given.
	 */
	private boolean isDateRangeFunction() {
		return switch (aggregation) {
			case FiveMinute, TenMinute, FifteenMinute, ThirtyMinute, DayOfWeek, SeasonalDayOfWeek, DayOfYear, HourOfDay, SeasonalHourOfDay, HourOfYear, WeekOfYear -> true;

			default -> false;
		};
	}

	private boolean isDefaultLocalDateRange() {
		return !filter.hasDateOrLocalDateRange() && (filter.getAggregation() == Aggregation.DayOfWeek
				|| filter.getAggregation() == Aggregation.SeasonalDayOfWeek
				|| filter.getAggregation() == Aggregation.DayOfYear
				|| filter.getAggregation() == Aggregation.HourOfDay
				|| filter.getAggregation() == Aggregation.SeasonalHourOfDay
				|| filter.getAggregation() == Aggregation.HourOfYear
				|| filter.getAggregation() == Aggregation.WeekOfYear);
	}

	private String sqlTableName() {
		return switch (aggregation) {
			case FiveMinute, TenMinute, FifteenMinute, ThirtyMinute -> String.format(
					filter.hasLocalDateRange()
							? "solardatm.rollup_datm_for_time_span_slots(%s, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone, ?)"
							: "solardatm.rollup_datm_for_time_span_slots(%s, ?, ?, ?)",
					metaStreamIdColumnName);

			case Hour -> "solardatm.agg_datm_hourly";

			case Day, Week -> "solardatm.agg_datm_daily";

			case Month -> "solardatm.agg_datm_monthly";

			case DayOfWeek -> String.format(filter.hasLocalDateRange() || isDefaultLocalDateRange()
					? "solardatm.find_agg_datm_dow(%s, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
					: "solardatm.find_agg_datm_dow(%s, ?, ?)", metaStreamIdColumnName);

			case SeasonalDayOfWeek -> String
					.format(filter.hasLocalDateRange() || isDefaultLocalDateRange()
							? "solardatm.find_agg_datm_dow_seasonal(%s, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
							: "solardatm.find_agg_datm_dow_seasonal(%s, ?, ?)", metaStreamIdColumnName);

			case DayOfYear -> String.format(filter.hasLocalDateRange() || isDefaultLocalDateRange()
					? "solardatm.find_agg_datm_doy(%s, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
					: "solardatm.find_agg_datm_doy(%s, ?, ?)", metaStreamIdColumnName);

			case HourOfDay -> String.format(filter.hasLocalDateRange() || isDefaultLocalDateRange()
					? "solardatm.find_agg_datm_hod(%s, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
					: "solardatm.find_agg_datm_hod(%s, ?, ?)", metaStreamIdColumnName);

			case SeasonalHourOfDay -> String
					.format(filter.hasLocalDateRange() || isDefaultLocalDateRange()
							? "solardatm.find_agg_datm_hod_seasonal(%s, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
							: "solardatm.find_agg_datm_hod_seasonal(%s, ?, ?)", metaStreamIdColumnName);

			case HourOfYear -> String.format(filter.hasLocalDateRange() || isDefaultLocalDateRange()
					? "solardatm.find_agg_datm_hoy(%s, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
					: "solardatm.find_agg_datm_hoy(%s, ?, ?)", metaStreamIdColumnName);

			case WeekOfYear -> String.format(filter.hasLocalDateRange() || isDefaultLocalDateRange()
					? "solardatm.find_agg_datm_woy(%s, ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone)"
					: "solardatm.find_agg_datm_woy(%s, ?, ?)", metaStreamIdColumnName);

			default -> "solardatm.da_datm";
		};
	}

	private void sqlFrom(StringBuilder buf, boolean withStreamLimit) {
		buf.append("FROM s\n");
		if ( filter.isMostRecent() ) {
			sqlFromLateral(buf, true, "1");
		} else if ( withStreamLimit && streamLimit != StreamLimit.None ) {
			sqlFromLateral(buf, streamLimit == StreamLimit.Descending, "?");
		} else {
			buf.append("INNER JOIN ").append(sqlTableName()).append(" datum ON datum.stream_id = ")
					.append(metaStreamIdColumnName).append("\n");
		}
	}

	private void sqlFromLateral(StringBuilder buf, boolean descending, String limit) {
		buf.append("INNER JOIN LATERAL (\n");
		buf.append("		SELECT datum.*\n");
		buf.append("		FROM ").append(sqlTableName()).append(" datum\n");
		buf.append("		WHERE datum.stream_id = ").append(metaStreamIdColumnName).append("\n");
		if ( filter.hasDateOrLocalDate() ) {
			addSqlWhere(buf, true);
		}
		// order by time alone: TimescaleDB uses its ordered ChunkAppend only then
		buf.append("		ORDER BY datum.").append(timeColumnName(aggregation));
		if ( descending ) {
			buf.append(" DESC");
		}
		buf.append("\n");
		buf.append("		LIMIT ").append(limit).append("\n");
		buf.append("	) datum ON datum.stream_id = ").append(metaStreamIdColumnName).append("\n");
	}

	private void sqlWhere(StringBuilder buf, boolean withStreamLimit) {
		if ( isDateRangeInJoin() || (withStreamLimit && streamLimit != StreamLimit.None) ) {
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
		int idx = orderBySorts(
				filter.getSorts() != null ? filter.getSorts() : DatumSqlUtils.SORTS_BY_STREAM_TIME,
				datumStreamSortMapping(filter.getLocationId() != null ? Location : Node, aggregation),
				order);
		if ( idx > 0 ) {
			buf.append("ORDER BY ").append(order.substring(idx));
		}
	}

	private void sqlCore(StringBuilder buf, boolean withContributors, boolean withStreamLimit) {
		sqlCte(buf);
		if ( combine != null ) {
			buf.append(", d AS (\n");
		}
		sqlSelect(buf);
		sqlFrom(buf, withStreamLimit);
		sqlWhere(buf, withStreamLimit);
		if ( aggregation == Aggregation.Week ) {
			buf.append("GROUP BY ");
			if ( combine != null ) {
				// each real stream must produce its own rows to be combined
				buf.append(
						"s.vstream_id, datum.stream_id, s.obj_rank, s.source_rank, s.names_i, s.names_a");
				if ( contributors ) {
					buf.append(", s.rnode_id, s.rsource_id");
				}
			} else if ( aliased ) {
				buf.append("s.stream_id");
			} else {
				buf.append("datum.stream_id");
			}
			buf.append(
					", date_trunc('week', datum.ts_start AT TIME ZONE s.time_zone) AT TIME ZONE s.time_zone\n");
		}
		if ( combine != null ) {
			if ( isMinuteAggregation() ) {
				buf.append("	GROUP BY datum.stream_id, ts\n");
				buf.append(") AS ds ON ds.stream_id = ").append(metaStreamIdColumnName).append("\n");
			}
			buf.append(")\n");
			buf.append(VirtualDatumSqlUtils.combineCteSql(combine.getType())).append("\n");
			if ( withContributors ) {
				buf.append(VirtualDatumSqlUtils.combineContributorsCteSql()).append("\n");
			}
			buf.append("SELECT datum.*, vs.")
					.append(filter.getObjectKind() == Location ? "loc_id" : "node_id")
					.append(", vs.source_id");
			if ( withContributors ) {
				buf.append(VirtualDatumSqlUtils.COMBINE_CONTRIBUTORS_SELECT_SQL);
			}
			buf.append("\nFROM datum\n");
			if ( withContributors ) {
				buf.append(VirtualDatumSqlUtils.COMBINE_CONTRIBUTORS_JOIN_SQL);
			}
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf, contributors, true);
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
		if ( isDefaultLocalDateRange() ) {
			// currently this implies a DOW/HOD style query; default date range will be the past 2 years
			stmt.setObject(++p, LocalDate.now(UTC).minusYears(2).atStartOfDay(), Types.TIMESTAMP);
			stmt.setObject(++p, LocalDate.now(UTC).plusDays(1).atStartOfDay(), Types.TIMESTAMP);
		} else if ( isDateRangeFunction() ? filter.hasLocalDateRange() : filter.hasLocalDate() ) {
			// the date range function SQL uses the local dates only when both are given
			p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, stmt, p);
		} else {
			p = DatumSqlUtils.prepareDateRangeFilter(filter, stmt, p);
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
		if ( streamLimit != StreamLimit.None ) {
			// the per-stream limit follows the date range, before the overall limit and offset
			stmt.setLong(++p, streamLimitMax());
		}
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
			if ( isMinuteAggregation() && combine == null ) {
				// We use a specialized count query here because the actual query does a lot
				// of computation to produce minute-level aggregation. The
				// solardatm.count_datm_time_span_slots() function is designed to run faster.
				// It counts the slots of each real stream, so cannot be used when combining.
				sqlCte(buf);
				buf.append("SELECT SUM(datum.dcount) AS dcount\n");
				buf.append("FROM s\n");
				buf.append("INNER JOIN solardatm.count_datm_time_span_slots(%s"
						.formatted(metaStreamIdColumnName));
				if ( filter.hasLocalDateRange() ) {
					buf.append(", ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone");
				} else {
					buf.append(", ?, ?");
				}
				buf.append(", ?) datum(dcount) ON TRUE\n");
				return buf.toString();
			}

			// use normal wrapped count query, without any per-stream limit so every row is counted
			sqlCore(buf, false, false);
			sqlOrderByJoins(buf);
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
