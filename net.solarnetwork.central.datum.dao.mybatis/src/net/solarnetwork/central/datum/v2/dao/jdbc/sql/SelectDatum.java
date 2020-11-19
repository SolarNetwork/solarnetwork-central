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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;
import net.solarnetwork.central.datum.v2.domain.DatumPK;

/**
 * Select for {@link DatumEntity} instances via a {@link DatumPK} ID or
 * {@link DatumCriteria} filter.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private final DatumCriteria filter;
	private final DatumPK id;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectDatum(DatumCriteria filter) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument not be null.");
		}
		this.filter = filter;
		this.id = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public SelectDatum(DatumPK id) {
		super();
		if ( id == null ) {
			throw new IllegalArgumentException("The id argument not be null.");
		}
		this.filter = null;
		this.id = id;
	}

	private void sqlFrom(StringBuilder buf) {
		buf.append("FROM solardatm.da_datm datum\n");
		if ( filter == null ) {
			return;
		}
		if ( filter.getStreamId() == null ) {
			if ( filter.getLocationId() != null ) {
				buf.append(
						"INNER JOIN solardatm.da_loc_datm_meta meta ON meta.stream_id = datum.stream_id\n");
			} else if ( filter.getNodeId() != null ) {
				buf.append(
						"INNER JOIN solardatm.da_datm_meta meta ON meta.stream_id = datum.stream_id\n");
			}
		}
	}

	private void sqlWhere(StringBuilder buf) {
		if ( id != null ) {
			buf.append("WHERE datum.stream_id = ? AND datum.ts = ?");
			return;
		}
		StringBuilder where = new StringBuilder();
		if ( filter.getStreamId() != null ) {
			where.append("\tAND datum.stream_id = ANY(?)\n");
		} else {
			DatumSqlUtils.whereDatumMetadata(filter, where);
		}
		DatumSqlUtils.whereDateRange(filter, where);
		if ( where.length() > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		if ( filter == null ) {
			return;
		}
		buf.append("ORDER BY ");

		StringBuilder order = new StringBuilder();
		int idx = 0;
		if ( filter.getSorts() != null && !filter.getSorts().isEmpty() ) {
			idx = DatumSqlUtils.orderBySorts(filter.getSorts(),
					filter.getLocationId() != null ? DatumSqlUtils.LOCATION_STREAM_SORT_KEY_MAPPING
							: DatumSqlUtils.NODE_STREAM_SORT_KEY_MAPPING,
					buf);
		}
		if ( idx < 1 ) {
			order.append("datum.stream_id, datum.ts");
		}
		buf.append(order.substring(idx));
	}

	private void sqlLimit(StringBuilder buf) {
		if ( filter != null && filter.getMax() != null ) {
			int max = filter.getMax();
			if ( max > 0 ) {
				buf.append("\nLIMIT ? OFFSET ?");
			}
		}
	}

	private void sqlCore(StringBuilder buf) {
		buf.append(
				"SELECT datum.stream_id, datum.ts, datum.received, datum.data_i, datum.data_a, datum.data_s, datum.data_t\n");
		sqlFrom(buf);
		sqlWhere(buf);
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderBy(buf);
		sqlLimit(buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( id != null ) {
			stmt.setObject(++p, id.getStreamId(), Types.OTHER);
			stmt.setTimestamp(++p, Timestamp.from(id.getTimestamp()));
			return p;
		}
		if ( filter.getStreamId() != null ) {
			Array a = con.createArrayOf("uuid", filter.getStreamIds());
			stmt.setArray(++p, a);
			a.free();
		} else {
			p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		}

		p = DatumSqlUtils.prepareDateRangeFilter(filter, con, stmt, p);

		return p;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
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
