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
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;

/**
 * Update a {@link ChargePointActionStatus} entity timestamp, creating it if not
 * already present.
 * 
 * @author matt
 * @version 1.0
 */
public class UpsertChargePointIdentifierActionTimestamp
		implements PreparedStatementCreator, SqlProvider {

	private final Long userId;
	private final String chargePointIdentifier;
	private final Integer connectorId;
	private final String action;
	private final Instant date;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointIdentifier
	 *        the charge point identifier
	 * @param connectorId
	 *        the connector ID, or {@literal null} for a charger-wide action
	 * @param action
	 *        the action name
	 * @param date
	 *        the timestamp
	 * @throws IllegalArgumentException
	 *         if any argument except {@code connectorId} is {@literal null}
	 */
	public UpsertChargePointIdentifierActionTimestamp(Long userId, String chargePointIdentifier,
			Integer connectorId, String action, Instant date) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.chargePointIdentifier = requireNonNullArgument(chargePointIdentifier,
				"chargePointIdentifier");
		this.connectorId = connectorId;
		this.action = requireNonNullArgument(action, "action");
		this.date = requireNonNullArgument(date, "date");
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO solarev.ocpp_charge_point_action_status (user_id, cp_id, conn_id, action, ts)
				SELECT cp.user_id, cp.id, ? AS conn_id, ? AS action, ? as ts
				FROM solarev.ocpp_charge_point cp
				WHERE cp.user_id = ?
					AND cp.ident = ?
				ON CONFLICT (user_id, cp_id, conn_id, action) DO UPDATE
				SET ts = EXCLUDED.ts
				""";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement(getSql());
		ps.setInt(1, connectorId != null ? connectorId.intValue() : 0);
		ps.setString(2, action);
		ps.setTimestamp(3, Timestamp.from(date));
		ps.setLong(4, userId);
		ps.setString(5, chargePointIdentifier);
		return ps;
	}

}
