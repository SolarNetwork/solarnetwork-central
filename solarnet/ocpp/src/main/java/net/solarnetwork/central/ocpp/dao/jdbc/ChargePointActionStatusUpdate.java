/* ==================================================================
 * ChargePointActionStatusUpdate.java - 15/05/2024 8:14:01 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import net.solarnetwork.central.domain.BasePK;

/**
 * Entity for charge point action status update process.
 * 
 * <p>
 * The {@code messageId} and {@code date} properties are not considered when
 * comparing for equality.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ChargePointActionStatusUpdate extends BasePK implements Serializable, Cloneable {

	private static final long serialVersionUID = -8964521616468244046L;

	/** The user ID. */
	private final long userId;

	/** The charge point ID. */
	private final String chargePointIdentifier;

	/** The connector ID. */
	private final int evseId;

	/** The connector ID. */
	private final int connectorId;

	/** The action name. */
	private final String action;

	private final String messageId;

	private final Instant date;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param evseId
	 *        the EVSE ID (if {@literal null} then {@literal 0} will be used)
	 * @param connectorId
	 *        the connector ID, or {@literal 0} for the charger itself
	 * @param action
	 *        the action name
	 * @param messageId
	 *        the message ID
	 * @param date
	 *        the timestamp
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code evseId} or {@code connectorId}
	 *         are {@literal null}
	 */
	public ChargePointActionStatusUpdate(Long userId, String chargePointIdentifier, Integer evseId,
			Integer connectorId, String action, String messageId, Instant date) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.chargePointIdentifier = requireNonNullArgument(chargePointIdentifier,
				"chargePointIdentifier");
		this.evseId = evseId != null ? evseId : 0;
		this.connectorId = connectorId != null ? connectorId : 0;
		this.action = requireNonNullArgument(action, "action");
		this.messageId = requireNonNullArgument(messageId, "messageId");
		this.date = requireNonNullArgument(date, "date");
	}

	@Override
	protected void populateIdValue(StringBuilder buf) {
		buf.append("u=").append(userId);
		buf.append(";cp=").append(chargePointIdentifier);
		buf.append(";e=").append(evseId);
		buf.append(";c=").append(connectorId);
		buf.append(";a=").append(action);
	}

	@Override
	protected void populateStringValue(StringBuilder buf) {
		buf.append("userId=").append(userId);
		buf.append(", chargePointIdentifier=").append(chargePointIdentifier);
		buf.append("; evseId=").append(evseId);
		buf.append("; connectorId=").append(connectorId);
		buf.append("; action=").append(action);
	}

	@Override
	public ChargePointActionStatusUpdate clone() {
		return (ChargePointActionStatusUpdate) super.clone();
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, chargePointIdentifier, evseId, connectorId, action);
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof ChargePointActionStatusUpdate) ) {
			return false;
		}
		ChargePointActionStatusUpdate other = (ChargePointActionStatusUpdate) obj;
		return userId == other.userId && chargePointIdentifier.equals(other.chargePointIdentifier)
				&& evseId == other.evseId && connectorId == other.connectorId
				&& action.equals(other.action);
	}

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID
	 */
	public long getUserId() {
		return userId;
	}

	/**
	 * Get the Charge Point identifier.
	 * 
	 * @return the Charge Point identifier
	 */
	public String getChargePointIdentifier() {
		return chargePointIdentifier;
	}

	/**
	 * Get the EVSE ID.
	 * 
	 * @return the EVSE ID, or {@literal 0} for the charger itself
	 */
	public int getEvseId() {
		return evseId;
	}

	/**
	 * Get the connector ID.
	 * 
	 * @return the connector ID, or {@literal 0} for the EVSE itself
	 */
	public int getConnectorId() {
		return connectorId;
	}

	/**
	 * Get the action.
	 * 
	 * @return the action, never {@literal null}
	 */
	public String getAction() {
		return action;
	}

	/**
	 * Get the message ID.
	 * 
	 * @return the message ID
	 */
	public String getMessageId() {
		return messageId;
	}

	/**
	 * Get the date.
	 * 
	 * @return the date
	 */
	public Instant getDate() {
		return date;
	}

}
