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
public class UpsertChargePointIdentifierConnectionStatus
		implements PreparedStatementCreator, SqlProvider {

	private final Long userId;
	private final String chargePointIdentifier;
	private final ChargePointStatus status;
	private final boolean updateDate;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointIdentifier
	 *        the charge point identifier
	 * @param status
	 *        the status data
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertChargePointIdentifierConnectionStatus(Long userId, String chargePointIdentifier,
			ChargePointStatus status) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.chargePointIdentifier = requireNonNullArgument(chargePointIdentifier,
				"chargePointIdentifier");
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
					SELECT user_id, id, ? AS connected_to , ? AS connected_date
					FROM solarev.ocpp_charge_point
					WHERE user_id = ?
					AND ident = ?
					ON CONFLICT (user_id, cp_id) DO UPDATE
					SET connected_to = EXCLUDED.connected_to
						, connected_date = EXCLUDED.connected_date
					""");
		} else {
			buf.append("""
					)
					SELECT user_id, id, ? AS connected_to
					FROM solarev.ocpp_charge_point
					WHERE user_id = ?
					AND ident = ?
					ON CONFLICT (user_id, cp_id) DO UPDATE
					SET connected_to = EXCLUDED.connected_to
					""");
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement(getSql());
		int p = 0;
		ps.setString(++p, status.getConnectedTo());
		if ( updateDate ) {
			ps.setTimestamp(++p, Timestamp.from(status.getConnectedDate()));
		}
		ps.setObject(++p, userId);
		ps.setString(++p, chargePointIdentifier);
		return ps;
	}

}
