/* ==================================================================
 * SelectDatumCalculatedAt.java - 19/11/2020 2:16:06 pm
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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Period;
import java.util.List;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Generate dynamic SQL for a {@link DatumCriteria} "calculate datum at a point
 * in time" query.
 *
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public final class SelectDatumCalculatedAt implements PreparedStatementCreator, SqlProvider {

	/**
	 * The default time tolerance used for the
	 * {@link net.solarnetwork.central.datum.domain.DatumReadingType#CalculatedAt}
	 * query.
	 */
	public static final Period DEFAULT_CALCULATED_AT_TIME_TOLERANCE = Period.ofMonths(1);

	private final DatumCriteria filter;
	private final boolean aliased;
	private final boolean sortByNode;
	private final boolean sortBySource;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@code null}
	 */
	public SelectDatumCalculatedAt(DatumCriteria filter) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.aliased = (filter.includeStreamAliases() && filter.getObjectKind() != Location);

		List<SortDescriptor> sorts = filter.getSorts();
		if ( sorts == null ) {
			sortByNode = false;
			sortBySource = false;
		} else {
			boolean byNode = false;
			boolean bySource = false;
			for ( SortDescriptor sort : sorts ) {
				if ( DatumSqlUtils.SORT_BY_NODE.equalsIgnoreCase(sort.getSortKey()) ) {
					byNode = true;
				} else if ( DatumSqlUtils.SORT_BY_SOURCE.equalsIgnoreCase(sort.getSortKey()) ) {
					bySource = true;
				}
			}
			sortByNode = byNode;
			sortBySource = bySource;
		}
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalStartDate() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
		if ( aliased ) {
			buf.append(", datum AS (\n");
		}
		buf.append("SELECT (solardatm.calc_datm_at(d, ?");
		if ( filter.hasLocalStartDate() ) {
			buf.append(" AT TIME ZONE s.time_zone");
		}
		buf.append(")).*\n");
		if ( !aliased && (sortByNode || sortBySource) ) {
			buf.append("\t");
			if ( sortByNode ) {
				buf.append(", min(s.node_id) AS node_id");
			}
			if ( sortBySource ) {
				buf.append(", min(s.source_id) AS source_id");
			}
			buf.append("\n");
		}
		buf.append("FROM s, solardatm.find_datm_around(s.");
		if ( aliased ) {
			buf.append("orig_");
		}
		buf.append("stream_id");
		if ( filter.hasLocalStartDate() ) {
			buf.append(", ? AT TIME ZONE s.time_zone");
		} else {
			buf.append(", ?");
		}
		buf.append(", ?) d\n");
		buf.append("GROUP BY d.stream_id\n");
		if ( aliased ) {
			buf.append("""
					)
					SELECT s.stream_id
						, datum.ts
						, datum.received
						, datum.data_i
						, datum.data_a
						, datum.data_s
						, datum.data_t
					FROM s
					INNER JOIN datum ON datum.stream_id = s.orig_stream_id
					""");
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		StringBuilder order = new StringBuilder();
		int idx = orderBySorts(filter.getSorts(),
				datumStreamSortMapping(filter.getLocationId() != null ? Location : Node, null), order);
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
		if ( filter.hasLocalStartDate() ) {
			stmt.setObject(++p, filter.getLocalStartDate(), Types.TIMESTAMP);
			stmt.setObject(++p, filter.getLocalStartDate(), Types.TIMESTAMP);
		} else {
			Timestamp t = Timestamp.from(filter.getStartDate() != null ? filter.getStartDate() : now());
			stmt.setTimestamp(++p, t);
			stmt.setTimestamp(++p, t);
		}
		Period t = filter.getTimeTolerance();
		if ( t == null ) {
			t = DEFAULT_CALCULATED_AT_TIME_TOLERANCE;
		}
		stmt.setObject(++p, t, Types.OTHER);
		return stmt;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return createStatement(con, getSql());
	}

}
