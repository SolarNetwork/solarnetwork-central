/* ==================================================================
 * GetDatumAuxiliary.java - 28/11/2020 9:28:25 am
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
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;

/**
 * Get for {@link DatumAuxiliaryEntity} instances via a {@link DatumAuxiliaryPK}
 * ID.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class GetDatumAuxiliary implements PreparedStatementCreator, SqlProvider {

	private final DatumAuxiliaryPK id;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public GetDatumAuxiliary(DatumAuxiliaryPK id) {
		super();
		if ( id == null ) {
			throw new IllegalArgumentException("The id argument not be null.");
		}
		this.id = id;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("SELECT stream_id, ts, atype, updated, notes, jdata_af, jdata_as, jmeta\n");
		buf.append("FROM solardatm.da_datm_aux\n");
		buf.append("WHERE stream_id = ? AND ts = ? AND atype = ?::solardatm.da_datm_aux_type");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setObject(1, id.getStreamId(), Types.OTHER);
		stmt.setTimestamp(2, Timestamp.from(id.getTimestamp()));
		stmt.setString(3, id.getKind().name());
		return stmt;
	}

}
