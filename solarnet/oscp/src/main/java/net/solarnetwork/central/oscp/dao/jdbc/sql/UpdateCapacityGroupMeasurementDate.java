/* ==================================================================
 * UpdateCapacityGroupMeasurementDate.java - 1/09/2022 11:21:30 am
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
 * Update the measurement date of a capacity group configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class UpdateCapacityGroupMeasurementDate implements PreparedStatementCreator, SqlProvider {

	private final OscpRole type;
	private final UserLongCompositePK id;
	private final Instant expected;
	private final Instant ts;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the type of measurement
	 * @param id
	 *        the ID of the capacity group
	 * @param expected
	 *        the expected current value of the measurement
	 * @param ts
	 *        the value to set of the expectation matches
	 * @throws IllegalArgumentException
	 *         if any argument except {@code expected} is {@literal null} or the
	 *         {@code id} is not assigned
	 */
	public UpdateCapacityGroupMeasurementDate(OscpRole type, UserLongCompositePK id, Instant expected,
			Instant ts) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.id = requireNonNullArgument(id, "id");
		if ( !id.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("The entity ID must be assigned.");
		}
		this.expected = expected;
		this.ts = requireNonNullArgument(ts, "ts");
	}

	@Override
	public String getSql() {
		return """
				UPDATE solaroscp.oscp_cg_%s_meas
				SET meas_at = ?
				WHERE user_id = ? AND cg_id = ? AND meas_at %s
				""".formatted(type.getAlias(), expected != null ? "= ?" : "IS NULL");
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setTimestamp(1, Timestamp.from(ts));
		stmt.setObject(2, id.getUserId(), Types.BIGINT);
		stmt.setObject(3, id.getEntityId(), Types.BIGINT);
		if ( expected != null ) {
			stmt.setTimestamp(4, Timestamp.from(expected));
		}
		return stmt;
	}

}
