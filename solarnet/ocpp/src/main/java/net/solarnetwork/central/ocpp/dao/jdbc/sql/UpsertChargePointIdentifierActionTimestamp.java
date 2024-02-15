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
 * @version 1.1
 */
public class UpsertChargePointIdentifierActionTimestamp
		implements PreparedStatementCreator, SqlProvider {

	private final Long userId;
	private final String chargePointIdentifier;
	private final Integer evseId;
	private final Integer connectorId;
	private final String action;
	private final String messageId;
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
	 * @param messageId
	 *        the message ID
	 * @param date
	 *        the timestamp
	 * @throws IllegalArgumentException
	 *         if any argument except {@code connectorId} is {@literal null}
	 */
	public UpsertChargePointIdentifierActionTimestamp(Long userId, String chargePointIdentifier,
			Integer connectorId, String action, String messageId, Instant date) {
		this(userId, chargePointIdentifier, 0, connectorId, action, messageId, date);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointIdentifier
	 *        the charge point identifier
	 * @param evseId
	 *        the EVSE ID
	 * @param connectorId
	 *        the connector ID, or {@literal null} for a charger-wide action
	 * @param action
	 *        the action name
	 * @param messageId
	 *        the message ID
	 * @param date
	 *        the timestamp
	 * @throws IllegalArgumentException
	 *         if any argument except {@code evseId} or {@code connectorId} is
	 *         {@literal null}
	 * @since 1.1
	 */
	public UpsertChargePointIdentifierActionTimestamp(Long userId, String chargePointIdentifier,
			Integer evseId, Integer connectorId, String action, String messageId, Instant date) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.chargePointIdentifier = requireNonNullArgument(chargePointIdentifier,
				"chargePointIdentifier");
		this.evseId = evseId;
		this.connectorId = connectorId;
		this.action = requireNonNullArgument(action, "action");
		this.messageId = requireNonNullArgument(messageId, "messageId");
		this.date = requireNonNullArgument(date, "date");
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO solarev.ocpp_charge_point_action_status (user_id, cp_id, evse_id, conn_id, action, msg_id, ts)
				SELECT cp.user_id, cp.id, ? AS evse_id, ? AS conn_id, ? AS action, ? as msg_id, ? as ts
				FROM solarev.ocpp_charge_point cp
				WHERE cp.user_id = ?
					AND cp.ident = ?
				ON CONFLICT (user_id, cp_id, evse_id, conn_id, action) DO UPDATE
				SET msg_id = EXCLUDED.msg_id
					, ts = EXCLUDED.ts
				""";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement(getSql());
		ps.setInt(1, evseId != null ? evseId.intValue() : 0);
		ps.setInt(2, connectorId != null ? connectorId.intValue() : 0);
		ps.setString(3, action);
		ps.setString(4, messageId);
		ps.setTimestamp(5, Timestamp.from(date));
		ps.setLong(6, userId);
		ps.setString(7, chargePointIdentifier);
		return ps;
	}

}
