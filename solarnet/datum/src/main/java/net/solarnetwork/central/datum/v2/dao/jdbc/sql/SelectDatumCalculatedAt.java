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
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.NODE_STREAM_SORT_KEY_MAPPING;
import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.orderBySorts;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Period;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.domain.datum.ObjectDatumKind;

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
	public static Period DEFAULT_CALCULATED_AT_TIME_TOLERANCE = Period.ofMonths(1);

	private final DatumCriteria filter;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectDatumCalculatedAt(DatumCriteria filter) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
	}

	private void appendCoreSql(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalStartDate() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
		buf.append("SELECT (solardatm.calc_datm_at(d, ?");
		if ( filter.hasLocalStartDate() ) {
			buf.append(" AT TIME ZONE s.time_zone");
		}
		buf.append(")).*\n");
		buf.append("\t, min(d.ts) AS ts, min(s.node_id) AS node_id, min(s.source_id) AS source_id\n");
		buf.append("FROM s\n");
		buf.append("INNER JOIN solardatm.find_datm_around(s.stream_id");
		if ( filter.hasLocalStartDate() ) {
			buf.append(", ? AT TIME ZONE s.time_zone");
		} else {
			buf.append(", ?");
		}
		buf.append(", ?) d ON TRUE\n");
		buf.append("GROUP BY s.stream_id");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		appendCoreSql(buf);
		StringBuilder order = new StringBuilder();
		int idx = orderBySorts(filter.getSorts(), NODE_STREAM_SORT_KEY_MAPPING, order);
		if ( idx > 0 ) {
			buf.append("\nORDER BY ");
			buf.append(order.substring(idx));
		}
		return buf.toString();
	}

	private PreparedStatement createStatement(Connection con, String sql) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = DatumSqlUtils.prepareObjectMetadataFilter(filter, ObjectDatumKind.Node, con, stmt, 0);
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
