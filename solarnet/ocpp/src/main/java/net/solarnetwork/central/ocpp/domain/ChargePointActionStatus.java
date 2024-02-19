/* ==================================================================
 * ChargePointActionStatus.java - 16/11/2022 5:08:46 pm
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

package net.solarnetwork.central.ocpp.domain;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.util.ObjectUtils;

/**
 * OCPP "last seen" timestamp for each action of a charger.
 * 
 * @author matt
 * @version 1.1
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "created", "userId", "chargePointId", "evseId", "connectorId", "action",
		"messageId", "ts" })
public class ChargePointActionStatus extends BasicEntity<ChargePointActionStatusKey>
		implements UserRelatedEntity<ChargePointActionStatusKey> {

	private static final long serialVersionUID = -2984317823572410962L;

	/** The message ID. */
	private String messageId;

	/** The last action timestamp. */
	@JsonProperty("ts")
	private Instant timestamp;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param connectorId
	 *        the connector ID, or {@literal 0} for the charger itself
	 * @param action
	 *        the action name
	 * @param created
	 *        the creation date
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code created} is {@literal null}
	 */
	public ChargePointActionStatus(long userId, long chargePointId, int connectorId, String action,
			Instant created) {
		this(new ChargePointActionStatusKey(userId, chargePointId, connectorId, action), created);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param evseId
	 *        the EVSE ID
	 * @param connectorId
	 *        the connector ID, or {@literal 0} for the charger itself
	 * @param action
	 *        the action name
	 * @param created
	 *        the creation date
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code created} is {@literal null}
	 * @since 1.1
	 */
	public ChargePointActionStatus(long userId, long chargePointId, int evseId, int connectorId,
			String action, Instant created) {
		this(new ChargePointActionStatusKey(userId, chargePointId, evseId, connectorId, action),
				created);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if any argument other than {@code created} is {@literal null}
	 */
	public ChargePointActionStatus(ChargePointActionStatusKey id, Instant created) {
		super(ObjectUtils.requireNonNullArgument(id, "id"), created);
	}

	@Override
	public Long getUserId() {
		return getId().getUserId();
	}

	/**
	 * Get the Charge Point ID.
	 * 
	 * @return the Charge Point ID
	 */
	public long getChargePointId() {
		return getId().getChargePointId();
	}

	/**
	 * Get the EVSE ID.
	 * 
	 * @return the EVSE ID, or {@literal 0} for the charger itself
	 * @since 1.1
	 */
	public int getEvseId() {
		return getId().getEvseId();
	}

	/**
	 * Get the connector ID.
	 * 
	 * @return the connector ID, or {@literal 0} for the EVSE itself
	 */
	public int getConnectorId() {
		return getId().getConnectorId();
	}

	/**
	 * Get the action.
	 * 
	 * @return the action, never {@literal null}
	 */
	public String getAction() {
		return getId().getAction();
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
	 * Set the message ID.
	 * 
	 * @param messageId
	 *        the message ID to set
	 */
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	/**
	 * Get the timestamp.
	 * 
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Set the timestamp.
	 * 
	 * @param timestamp
	 *        the timestamp to set
	 */
	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

}
