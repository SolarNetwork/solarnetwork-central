/* ==================================================================
 * InsertCapacityGroupMeasurementDate.java - 1/09/2022 10:48:29 am
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Insert a capacity group measurement date.
 *
 * @author matt
 * @version 1.0
 */
public final class InsertCapacityGroupMeasurementDate implements PreparedStatementCreator, SqlProvider {

	private final OscpRole type;
	private final UserLongCompositePK id;
	private final Instant ts;

	/**
	 * Constructor.
	 *
	 * @param type
	 *        the type of token to create
	 * @param id
	 *        the ID of the group
	 * @param ts
	 *        the value to set
	 * @throws IllegalArgumentException
	 *         if any argument except {@code ts} is {@literal null} or the
	 *         {@code id} is not assigned
	 */
	public InsertCapacityGroupMeasurementDate(OscpRole type, UserLongCompositePK id, Instant ts) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.id = requireNonNullArgument(id, "id");
		if ( !id.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("The entity ID must be assigned.");
		}
		this.ts = ts;
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO solaroscp.oscp_cg_%s_meas (user_id, cg_id, meas_at)
				VALUES (?, ?, ?)
				ON CONFLICT (user_id, cg_id) DO NOTHING
				""".formatted(type.getAlias());
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setObject(1, id.getUserId(), Types.BIGINT);
		stmt.setObject(2, id.getEntityId(), Types.BIGINT);
		if ( ts == null ) {
			stmt.setNull(3, Types.TIMESTAMP);
		} else {
			stmt.setTimestamp(3, Timestamp.from(ts));
		}
		return stmt;
	}

}
