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
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;

/**
 * Update a {@link ChargePointStatus} entity connection details, creating it if
 * not already present.
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
		buf.append(
				"INSERT INTO solarev.ocpp_charge_point_status (user_id, cp_id, connected_to, session_id");
		if ( updateDate ) {
			buf.append("""
					, connected_date)
					SELECT cp.user_id, cp.id, ? AS connected_to, ? AS session_id, ? AS connected_date
					FROM solarev.ocpp_charge_point cp
					WHERE cp.user_id = ?
						AND cp.ident = ?
					ON CONFLICT (user_id, cp_id) DO UPDATE
					SET connected_to = EXCLUDED.connected_to
						, session_id = EXCLUDED.session_id
						, connected_date = EXCLUDED.connected_date
					""");
		} else {
			buf.append("""
					)
					SELECT cp.user_id, cp.id, NULL::TEXT AS connected_to, NULL::TEXT AS session_id
					FROM solarev.ocpp_charge_point cp
					INNER JOIN solarev.ocpp_charge_point_status cps
						ON cps.user_id = cp.user_id AND cps.cp_id = cp.id
					WHERE cp.user_id = ?
						AND cp.ident = ?
						AND cps.connected_to = ?
						AND cps.session_id = ?
					ON CONFLICT (user_id, cp_id) DO UPDATE
					SET connected_to = EXCLUDED.connected_to
						, session_id = EXCLUDED.session_id
					""");
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement(getSql());
		int p = 0;
		if ( updateDate ) {
			ps.setString(++p, status.getConnectedTo());
			ps.setString(++p, status.getSessionId());
			ps.setTimestamp(++p, Timestamp.from(status.getConnectedDate()));
		}
		ps.setObject(++p, userId);
		ps.setString(++p, chargePointIdentifier);
		if ( !updateDate ) {
			ps.setString(++p, status.getConnectedTo());
			ps.setString(++p, status.getSessionId());
		}
		return ps;
	}

}
