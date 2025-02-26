/* ==================================================================
 * DeleteDatumAuxiliary.java - 28/11/2020 9:40:21 am
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
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;

/**
 * Delete {@link DatumAuxiliaryEntity} instances by {@link DatumAuxiliaryPK} ID.
 *
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public final class DeleteDatumAuxiliary implements CallableStatementCreator, SqlProvider {

	private final DatumAuxiliaryPK id;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public DeleteDatumAuxiliary(DatumAuxiliaryPK id) {
		super();
		this.id = requireNonNullArgument(id, "id");
	}

	@Override
	public String getSql() {
		return "{call solardatm.delete_datum_aux(?,?,?::solardatm.da_datm_aux_type)}";
	}

	@Override
	public CallableStatement createCallableStatement(Connection con) throws SQLException {
		CallableStatement stmt = con.prepareCall(getSql());
		stmt.setObject(1, id.getStreamId(), Types.OTHER);
		stmt.setTimestamp(2, Timestamp.from(id.getTimestamp()));
		stmt.setString(3, id.getKind().name());
		return stmt;
	}

}
