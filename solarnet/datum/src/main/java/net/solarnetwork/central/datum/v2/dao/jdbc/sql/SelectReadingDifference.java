/* ==================================================================
 * ReadingDatumCriteriaPreparedStatementSetter.java - 17/11/2020 11:47:59 am
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

import static java.time.Instant.now;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.datumStreamSortMapping;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.orderBySorts;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Location;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Period;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Generate dynamic SQL for a {@link DatumCriteria} difference query.
 *
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public final class SelectReadingDifference
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/**
	 * The default time tolerance used for the
	 * {@link DatumReadingType#NearestDifference} query.
	 */
	public static final Period DEFAULT_NEAREST_DIFFERENCE_TIME_TOLERANCE = Period.ofMonths(3);

	private final DatumCriteria filter;
	private final DatumReadingType readingType;
	private final boolean aliased;
	private final String metaStreamIdColumnName;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@code null}
	 */
	public SelectReadingDifference(DatumCriteria filter) {
		super();
		if ( filter == null || filter.getReadingType() == null ) {
			throw new IllegalArgumentException("The filter argument and reading type must not be null.");
		}
		this.filter = filter;
		this.readingType = filter.getReadingType();
		this.aliased = (filter.includeStreamAliases() && filter.getObjectKind() != Location);
		this.metaStreamIdColumnName = (aliased ? "s.orig_stream_id" : "s.stream_id");
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
		if ( aliased ) {
			buf.append(", datum AS (\n");
		}
		buf.append("SELECT (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).*\n");
		buf.append("\t, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id\n");
		buf.append("FROM s\n");
		buf.append("INNER JOIN solardatm.");
		buf.append(switch (readingType) {
			case Difference -> "find_datm_diff_rows";
			case NearestDifference -> "find_datm_diff_near_rows";
			case DifferenceWithin -> "find_datm_diff_within_rows";
			case CalculatedAtDifference -> "find_datm_diff_at_rows";
			default -> throw new UnsupportedOperationException(
					"Reading type " + filter.getReadingType() + " not supported.");
		});
		buf.append("(").append(metaStreamIdColumnName);
		if ( filter.hasLocalDateRange() ) {
			buf.append(", ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone");
		} else {
			buf.append(", ?, ?");
		}
		if ( filter.getReadingType() == DatumReadingType.NearestDifference
				|| filter.getReadingType() == DatumReadingType.CalculatedAtDifference ) {
			buf.append(", ?");
		}
		buf.append(") d ON TRUE\n");
		buf.append("GROUP BY ").append(metaStreamIdColumnName).append("\n");
		if ( aliased ) {
			buf.append(")\n");
			buf.append("SELECT s.stream_id\n");
			buf.append("""
						, datum.ts_start
						, datum.ts_end
						, datum.data_i
						, datum.data_a
						, datum.data_s
						, datum.data_t
						, datum.stat_i
						, datum.read_a
					FROM s
					INNER JOIN datum ON datum.stream_id = s.orig_stream_id
					""");
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		StringBuilder order = new StringBuilder();
		// pass Minute aggregate because time output column is ts_start, not ts
		int idx = orderBySorts(filter.getSorts(), datumStreamSortMapping(
				filter.getLocationId() != null ? Location : Node, Aggregation.Minute), order);
		if ( idx > 0 ) {
			buf.append("ORDER BY ").append(order.substring(idx));
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderBy(buf);
		return buf.toString();
	}

	private PreparedStatement createStatement(Connection con, String sql) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = DatumSqlUtils.prepareObjectMetadataFilter(filter, null, con, stmt, 0);
		if ( filter.hasLocalDateRange() ) {
			stmt.setObject(++p, filter.getLocalStartDate(), Types.TIMESTAMP);
			stmt.setObject(++p, filter.getLocalEndDate(), Types.TIMESTAMP);
		} else {
			stmt.setTimestamp(++p,
					Timestamp.from(filter.getStartDate() != null ? filter.getStartDate() : now()));
			stmt.setTimestamp(++p,
					Timestamp.from(filter.getEndDate() != null ? filter.getEndDate() : now()));
		}
		if ( readingType == DatumReadingType.NearestDifference
				|| readingType == DatumReadingType.CalculatedAtDifference ) {
			Period t = filter.getTimeTolerance();
			if ( t == null ) {
				t = DEFAULT_NEAREST_DIFFERENCE_TIME_TOLERANCE;
			}
			stmt.setObject(++p, t, Types.OTHER);
		}
		return stmt;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return createStatement(con, getSql());
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
			return createStatement(con, getSql());
		}

	}

}
