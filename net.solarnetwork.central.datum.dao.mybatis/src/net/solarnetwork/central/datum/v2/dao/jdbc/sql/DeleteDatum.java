/* ==================================================================
 * DeleteDatum.java - 6/12/2020 8:14:49 am
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;

/**
 * Delete datum stream data matching an {@link ObjectStreamCriteria} filter.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class DeleteDatum implements PreparedStatementCreator, SqlProvider {

	private final ObjectStreamCriteria filter;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 */
	public DeleteDatum(ObjectStreamCriteria filter) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument not be null.");
		}
		if ( !filter.hasLocalDateRange() ) {
			throw new IllegalArgumentException("The filter must provide a local date range.");
		}
		this.filter = filter;
	}

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT SUM(d.count)::BIGINT AS count\n");
	}

	private void sqlFrom(StringBuilder buf) {
		buf.append("FROM s, solardatm.delete_datm(s.stream_id, ?, ?, s.time_zone) d(count)");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCte(buf);
		sqlSelect(buf);
		sqlFrom(buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		DatumSqlUtils.prepareLocalDateRangeFilter(filter, con, stmt, p);
		return stmt;
	}

}
