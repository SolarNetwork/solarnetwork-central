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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.DatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.DatumPK;

/**
 * Select for {@link DatumEntity} instances.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectDatum implements PreparedStatementCreator, SqlProvider {

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

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("SELECT stream_id, ts, received, data_i, data_a, data_s, data_t\n");
		buf.append("FROM solardatm.da_datm\n");
		if ( id != null ) {
			buf.append("WHERE stream_id = ? AND ts = ?");
		} else {
			// TODO
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		if ( id != null ) {
			stmt.setObject(1, id.getStreamId(), Types.OTHER);
			stmt.setTimestamp(2, Timestamp.from(id.getTimestamp()));
		} else {
			// TODO
		}
		return stmt;
	}

}
