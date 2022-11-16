/* ==================================================================
 * UpsertChargePointActionStatus.java - 16/11/2022 5:44:19 pm
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

package net.solarnetwork.central.ocpp.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;

/**
 * Update a {@link ChargePointActionStatus} entity connection details, creating
 * it if not already present.
 * 
 * @author matt
 * @version 1.0
 */
public class UpsertChargePointConnectionStatus
		implements PreparedStatementCreator, SqlProvider, PreparedStatementSetter {

	private final ChargePointStatus status;
	private final boolean updateDate;

	/**
	 * Constructor.
	 * 
	 * @param status
	 *        the status
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertChargePointConnectionStatus(ChargePointStatus status) {
		super();
		this.status = requireNonNullArgument(status, "status");
		this.updateDate = (status.getConnectedTo() != null && status.getConnectedDate() != null);
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("INSERT INTO solarev.ocpp_charge_point_status (user_id, cp_id, connected_to");
		if ( updateDate ) {
			buf.append("""
					, connected_date)
					VALUES (?, ?, ?, ?)
					ON CONFLICT (user_id, cp_id) DO UPDATE
					SET connected_to = EXCLUDED.connected_to
						, connected_date = EXCLUDED.connected_date
					""");
		} else {
			buf.append("""
					)
					VALUES (?, ?, ?)
					ON CONFLICT (user_id, cp_id) DO UPDATE
					SET connected_to = EXCLUDED.connected_to
					""");
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		return con.prepareStatement(getSql());
	}

	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		ps.setLong(1, status.getUserId());
		ps.setLong(2, status.getChargePointId());
		ps.setString(3, status.getConnectedTo());
		if ( updateDate ) {
			ps.setTimestamp(4, Timestamp.from(status.getConnectedDate()));
		}
	}

}
