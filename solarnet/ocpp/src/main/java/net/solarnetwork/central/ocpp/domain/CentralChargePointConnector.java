/* ==================================================================
 * CentralChargePointConnector.java - 26/02/2020 8:52:41 am
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

package net.solarnetwork.central.ocpp.domain;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.ocpp.domain.ChargePointConnector;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;

/**
 * A Charge Point connector entity.
 * 
 * <p>
 * A connector ID of {@literal 0} represents the Charge Point as a whole.
 * </p>
 * 
 * @author matt
 * @version 1.2
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "chargePointId", "evseId", "connectorId", "userId", "created", "info" })
public class CentralChargePointConnector extends ChargePointConnector
		implements UserRelatedEntity<ChargePointConnectorKey> {

	private static final long serialVersionUID = 8009006297002891630L;

	private final Long userId;

	/**
	 * Constructor.
	 */
	public CentralChargePointConnector() {
		super();
		this.userId = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the owner user ID
	 */
	public CentralChargePointConnector(Long userId) {
		super();
		this.userId = userId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param userId
	 *        the owner user ID
	 */
	public CentralChargePointConnector(ChargePointConnectorKey id, Long userId) {
		super(id);
		this.userId = userId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 */
	public CentralChargePointConnector(ChargePointConnectorKey id) {
		super(id);
		this.userId = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the created date
	 */
	public CentralChargePointConnector(ChargePointConnectorKey id, Instant created) {
		super(id, created);
		this.userId = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param chargePointId
	 *        the charge point ID
	 * @param connectorId
	 *        the connector ID
	 * @param created
	 *        the created date
	 */
	public CentralChargePointConnector(long chargePointId, int connectorId, Instant created) {
		this(chargePointId, 0, connectorId, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param chargePointId
	 *        the charge point ID
	 * @param evseId
	 *        the EVSE ID
	 * @param connectorId
	 *        the connector ID
	 * @param created
	 *        the created date
	 * @since 1.2
	 */
	public CentralChargePointConnector(long chargePointId, int evseId, int connectorId,
			Instant created) {
		super(new ChargePointConnectorKey(chargePointId, evseId, connectorId), created);
		this.userId = null;
	}

	/**
	 * Constructor.
	 * 
	 * @param chargePointId
	 *        the charge point ID
	 * @param connectorId
	 *        the connector ID
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the created date
	 */
	public CentralChargePointConnector(
			@JsonProperty(value = "chargePointId", required = true) long chargePointId,
			@JsonProperty(value = "connectorId", required = true) int connectorId,
			@JsonProperty(value = "userId", required = true) Long userId,
			@JsonProperty("created") Instant created) {
		this(chargePointId, 0, connectorId, userId, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param chargePointId
	 *        the charge point ID
	 * @param evseId
	 *        the EVSE ID
	 * @param connectorId
	 *        the connector ID
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the created date
	 * @since 1.2
	 */
	@JsonCreator
	public CentralChargePointConnector(
			@JsonProperty(value = "chargePointId", required = true) long chargePointId,
			@JsonProperty(value = "evseId", required = false, defaultValue = "0") int evseId,
			@JsonProperty(value = "connectorId", required = true) int connectorId,
			@JsonProperty(value = "userId", required = true) Long userId,
			@JsonProperty("created") Instant created) {
		super(new ChargePointConnectorKey(chargePointId, evseId, connectorId), created);
		this.userId = userId;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the other charge point to copy
	 */
	public CentralChargePointConnector(ChargePointConnector other) {
		super(other);
		this.userId = (other instanceof CentralChargePointConnector
				? ((CentralChargePointConnector) other).userId
				: null);
	}

	@Override
	public boolean isSameAs(ChargePointConnector other) {
		if ( !(other instanceof CentralChargePointConnector) ) {
			return false;
		}
		return super.isSameAs(other);
	}

	/**
	 * Get the charge point ID.
	 * 
	 * @return the charge point ID
	 */
	public Long getChargePointId() {
		ChargePointConnectorKey id = getId();
		return (id != null ? id.getChargePointId() : null);
	}

	/**
	 * Get the EVSE ID.
	 * 
	 * @return the EVSE ID
	 * @since 1.2
	 */
	public Integer getEvseId() {
		ChargePointConnectorKey id = getId();
		return (id != null ? id.getEvseId() : null);
	}

	/**
	 * Get the connector ID.
	 * 
	 * @return the connector ID
	 */
	public Integer getConnectorId() {
		ChargePointConnectorKey id = getId();
		return (id != null ? id.getConnectorId() : null);
	}

	/**
	 * Get the owner user ID.
	 * 
	 * @return the owner user ID
	 */
	@Override
	public Long getUserId() {
		return userId;
	}

}
