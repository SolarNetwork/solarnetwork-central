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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Get for {@link DatumEntity} instances via a {@link DatumPK} ID.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class GetDatum implements PreparedStatementCreator, SqlProvider {

	private final DatumPK id;
	private final Aggregation aggregation;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public GetDatum(DatumPK id) {
		this(id, Aggregation.None);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param aggregation
	 *        the aggregation
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public GetDatum(DatumPK id, Aggregation aggregation) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.aggregation = aggregation(aggregation);
	}

	private static Aggregation aggregation(Aggregation aggregation) {
		// limit aggregation to specific supported ones
		Aggregation result = Aggregation.None;
		if ( aggregation != null ) {
			switch (aggregation) {
				case Hour:
				case Day:
				case Month:
					result = aggregation;
					break;

				default:
					// ignore all others
			}
		}
		return result;
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT ");
		sqlColumnsPk(buf);
	}

	private void sqlColumnsPk(StringBuilder buf) {
		buf.append("datum.stream_id,\n");
		if ( aggregation == Aggregation.None ) {
			buf.append("datum.ts,\n");
			buf.append("datum.received,\n");
		} else {
			buf.append("datum.ts_start AS ts,\n");
		}
		buf.append("datum.data_i,\n");
		buf.append("datum.data_a,\n");
		buf.append("datum.data_s,\n");
		buf.append("datum.data_t\n");

	}

	private void sqlFrom(StringBuilder buf) {
		buf.append("FROM ");
		switch (aggregation) {
			case Hour:
				buf.append("solardatm.da_datm_hourly");

			case Day:
				buf.append("solardatm.da_datm_daily");

			case Month:
				buf.append("solardatm.da_datm_monthly");

			default:
				buf.append("solardatm.da_datm");
		}
		buf.append(" datum\n");
	}

	private void sqlWhere(StringBuilder buf) {
		buf.append("WHERE datum.stream_id = ? AND datum.ts = ?");
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
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		stmt.setObject(++p, id.getStreamId(), Types.OTHER);
		stmt.setTimestamp(++p, Timestamp.from(id.getTimestamp()));
		return p;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		prepareCore(con, stmt, 0);
		return stmt;
	}

}
