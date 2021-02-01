/* ==================================================================
 * StoreDatumAuxiliary.java - 29/11/2020 7:46:10 am
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.util.JsonUtils;

/**
 * Insert or update a {@code da_datm_aux} record by calling the
 * {@code solardatm.store_datum_aux} database procedure.
 * 
 * @author matt
 * @version 1.0 since 3.8
 */
public class StoreDatumAuxiliary implements CallableStatementCreator, SqlProvider {

	private final DatumAuxiliary datum;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum to store
	 */
	public StoreDatumAuxiliary(DatumAuxiliary datum) {
		super();
		if ( datum == null ) {
			throw new IllegalArgumentException("The datum argument not be null.");
		}
		this.datum = datum;
	}

	@Override
	public String getSql() {
		return "{call solardatm.store_datum_aux(?,?,?::solardatm.da_datm_aux_type,?,?::jsonb,?::jsonb,?::jsonb)}";
	}

	@Override
	public CallableStatement createCallableStatement(Connection con) throws SQLException {
		CallableStatement stmt = con.prepareCall(getSql());
		stmt.setObject(1, datum.getStreamId(), Types.OTHER);
		stmt.setTimestamp(2, Timestamp.from(datum.getTimestamp()));
		stmt.setString(3, datum.getType().name());
		stmt.setString(4, datum.getNotes());
		stmt.setString(5, JsonUtils.getJSONString(datum.getSamplesFinal(), null));
		stmt.setString(6, JsonUtils.getJSONString(datum.getSamplesStart(), null));
		stmt.setString(7, JsonUtils.getJSONString(datum.getMetadata(), null));
		return stmt;
	}

}
