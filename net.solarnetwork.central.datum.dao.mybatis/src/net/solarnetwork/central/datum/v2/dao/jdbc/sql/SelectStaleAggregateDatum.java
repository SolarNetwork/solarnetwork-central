/* ==================================================================
 * SelectStaleAggregateDatum.java - 23/11/2020 8:51:47 am
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils.orderBySorts;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Select for {@link StaleAuditDatum} instances via a
 * {@link DatumStreamCriteria} filter.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectStaleAggregateDatum implements PreparedStatementCreator, SqlProvider {

	private final DatumStreamCriteria filter;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectStaleAggregateDatum(DatumStreamCriteria filter) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument not be null.");
		}
		this.filter = filter;
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT datum.stream_id,\n");
		buf.append("	datum.ts_start AS ts,\n");
		buf.append("	datum.agg_kind,\n");
		buf.append("	datum.created\n");
	}

	private void sqlFrom(StringBuilder buf) {
		buf.append("FROM solardatm.agg_stale_datm datum\n");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.getAggregation() != null ) {
			where.append("\tAND datum.agg_kind = ?\n");
			idx++;
		}
		idx |= DatumSqlUtils.whereStreamMetadata(filter, buf);
		idx |= DatumSqlUtils.whereDateRange(filter, Aggregation.Hour, where);
		if ( idx > 0 ) {
			buf.append("WHERE ").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		if ( filter == null ) {
			return;
		}

		StringBuilder order = new StringBuilder();
		int idx = 2;
		if ( filter.hasSorts() ) {
			idx = orderBySorts(filter.getSorts(), DatumSqlUtils.STREAM_METADATA_SORT_KEY_MAPPING, order);
		} else {
			order.append(", datum.agg_kind, ts, datum.stream_id");
		}
		if ( order.length() > 0 ) {
			buf.append("ORDER BY ").append(order.substring(idx));
		}
	}

	private void sqlCore(StringBuilder buf) {
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
		if ( filter.getAggregation() != null ) {
			stmt.setString(++p, filter.getAggregation().getKey());
		}
		p = DatumSqlUtils.prepareStreamFilter(filter, con, stmt, p);
		p = DatumSqlUtils.prepareDateRangeFilter(filter, con, stmt, p);
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

}
