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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import static java.time.Instant.now;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumCriteria;

/**
 * Generate dynamic SQL for a {@link ReadingDatumCriteria}.
 * 
 * @author matt
 * @version 1.0
 */
public class ReadingDatumCriteriaPreparedStatementCreator
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private static final Map<String, String> SORT_KEY_MAPPING;
	static {
		Map<String, String> map = new LinkedHashMap<>(4);
		map.put("timestamp", "ts_start");
		map.put("stream", "stream_id");
		SORT_KEY_MAPPING = Collections.unmodifiableMap(map);
	}

	private final ReadingDatumCriteria filter;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public ReadingDatumCriteriaPreparedStatementCreator(ReadingDatumCriteria filter) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument must not be null.");
		}
		this.filter = filter;
	}

	private boolean useLocalDates() {
		return (filter != null && filter.getLocalStartDate() != null
				&& filter.getLocalStartDate() != null);
	}

	private void appendCoreSql(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter, buf);
		buf.append(")\n");
		buf.append("SELECT (solardatm.diff_datm(d ORDER BY d.ts, d.rtype)).*\n");
		buf.append("FROM s\n");
		buf.append("INNER JOIN solardatm.find_datm_diff_rows(s.stream_id");
		if ( useLocalDates() ) {
			buf.append(", ? AT TIME ZONE s.time_zone, ? AT TIME ZONE s.time_zone");
		} else {
			buf.append(", ?, ?");
		}
		buf.append(") d ON TRUE\n");
		buf.append("GROUP BY s.stream_id");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		appendCoreSql(buf);
		DatumSqlUtils.orderBySorts(filter.getSorts(), SORT_KEY_MAPPING, buf);
		return buf.toString();
	}

	private PreparedStatement createStatement(Connection con, String sql) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = DatumSqlUtils.nodeMetadataFilterPrepare(filter, con, stmt, 0);
		if ( useLocalDates() ) {
			stmt.setTimestamp(++p, Timestamp.valueOf(filter.getLocalStartDate()));
			stmt.setTimestamp(++p, Timestamp.valueOf(filter.getLocalEndDate()));
		} else {
			stmt.setTimestamp(++p,
					Timestamp.from(filter.getStartDate() != null ? filter.getStartDate() : now()));
			stmt.setTimestamp(++p,
					Timestamp.from(filter.getEndDate() != null ? filter.getEndDate() : now()));
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
			appendCoreSql(buf);
			return buf.toString();
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			return createStatement(con, getSql());
		}

	}

}
