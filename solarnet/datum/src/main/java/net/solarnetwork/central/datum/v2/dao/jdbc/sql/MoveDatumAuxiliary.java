/* ==================================================================
 * MoveDatumAuxiliary.java - 28/11/2020 5:50:00 pm
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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.codec.JsonUtils;

/**
 * Move and update a {@code da_datm_aux} record (change primary key and update
 * values) by calling the {@code solardatm.move_datum_aux} database procedure.
 * 
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public class MoveDatumAuxiliary implements CallableStatementCreator, SqlProvider {

	private final DatumAuxiliaryPK from;
	private final DatumAuxiliary to;

	/**
	 * Constructor.
	 * 
	 * @param from
	 *        the primary key of the existing datum to move
	 * @param to
	 *        the destintation data to update
	 */
	public MoveDatumAuxiliary(DatumAuxiliaryPK from, DatumAuxiliary to) {
		super();
		this.from = requireNonNullArgument(from, "from");
		this.to = requireNonNullArgument(to, "to");
	}

	@Override
	public String getSql() {
		return "{? = call solardatm.move_datum_aux(?,?,?::solardatm.da_datm_aux_type,?,?,?::solardatm.da_datm_aux_type,?,?::jsonb,?::jsonb,?::jsonb)}";
	}

	@Override
	public CallableStatement createCallableStatement(Connection con) throws SQLException {
		CallableStatement stmt = con.prepareCall(getSql());
		stmt.registerOutParameter(1, Types.BOOLEAN);
		stmt.setObject(2, from.getStreamId(), Types.OTHER);
		stmt.setTimestamp(3, Timestamp.from(from.getTimestamp()));
		stmt.setString(4, from.getKind().name());
		stmt.setObject(5, to.getStreamId(), Types.OTHER);
		stmt.setTimestamp(6, Timestamp.from(to.getTimestamp()));
		stmt.setString(7, to.getType().name());
		stmt.setString(8, to.getNotes());
		stmt.setString(9, JsonUtils.getJSONString(to.getSamplesFinal(), null));
		stmt.setString(10, JsonUtils.getJSONString(to.getSamplesStart(), null));
		stmt.setString(11, JsonUtils.getJSONString(to.getMetadata(), null));
		return stmt;
	}

}
