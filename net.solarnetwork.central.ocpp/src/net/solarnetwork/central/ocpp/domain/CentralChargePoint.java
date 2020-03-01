/* ==================================================================
 * CentralChargePoint.java - 25/02/2020 6:56:59 pm
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
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.user.dao.UserNodeRelatedEntity;
import net.solarnetwork.ocpp.domain.ChargePoint;
import net.solarnetwork.ocpp.domain.ChargePointInfo;

/**
 * A Charge Point entity.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "userNodeId" })
@JsonPropertyOrder({ "id", "created", "userId", "nodeId", "enabled", "registrationStatus",
		"connectorCount", "info" })
public class CentralChargePoint extends ChargePoint implements UserNodeRelatedEntity<Long> {

	private final Long userId;
	private final Long nodeId;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the owner user ID
	 * @param nodeId
	 *        the owner node ID
	 */
	public CentralChargePoint(Long userId, Long nodeId) {
		super();
		this.userId = userId;
		this.nodeId = nodeId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param userId
	 *        the owner user ID
	 * @param nodeId
	 *        the owner node ID
	 */
	public CentralChargePoint(Long id, Long userId, Long nodeId) {
		super(id);
		this.userId = userId;
		this.nodeId = nodeId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param userId
	 *        the owner user ID
	 * @param nodeId
	 *        the owner node ID
	 * @param created
	 *        the created date
	 */
	@JsonCreator
	public CentralChargePoint(@JsonProperty("id") Long id,
			@JsonProperty(value = "userId", required = true) Long userId,
			@JsonProperty(value = "nodeId", required = true) Long nodeId,
			@JsonProperty("created") Instant created) {
		super(id, created);
		this.userId = userId;
		this.nodeId = nodeId;
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param userId
	 *        the owner user ID
	 * @param nodeId
	 *        the owner node ID
	 * @param created
	 *        the created date
	 * @param info
	 *        the info
	 */
	public CentralChargePoint(Long id, Long userId, Long nodeId, Instant created, ChargePointInfo info) {
		super(id, created, info);
		this.userId = userId;
		this.nodeId = nodeId;
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the owner user ID
	 * @param nodeId
	 *        the owner node ID
	 * @param created
	 *        the created date
	 * @param identifier
	 *        the identifier
	 * @param chargePointVendor
	 *        the vendor
	 * @param chargePointModel
	 *        the model
	 */
	public CentralChargePoint(Long userId, Long nodeId, Instant created, String identifier,
			String chargePointVendor, String chargePointModel) {
		super(created, identifier, chargePointVendor, chargePointModel);
		this.userId = userId;
		this.nodeId = nodeId;
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the object to copy
	 */
	public CentralChargePoint(ChargePoint other) {
		super(other);
		if ( other instanceof CentralChargePoint ) {
			CentralChargePoint ccp = (CentralChargePoint) other;
			this.userId = ccp.userId;
			this.nodeId = ccp.nodeId;
		} else {
			this.userId = null;
			this.nodeId = null;
		}
	}

	@Override
	public boolean isSameAs(ChargePoint other) {
		if ( !(other instanceof CentralChargePoint) ) {
			return false;
		}
		boolean result = super.isSameAs(other);
		if ( result ) {
			CentralChargePoint ccp = (CentralChargePoint) other;
			result = Objects.equals(this.userId, ccp.userId) && Objects.equals(this.nodeId, ccp.nodeId);
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ChargePoint{");
		if ( getId() != null ) {
			builder.append("id=");
			builder.append(getId());
			builder.append(", ");
		}
		builder.append("userId=").append(userId).append(", ");
		builder.append("nodeId=").append(nodeId).append(", ");
		if ( getRegistrationStatus() != null ) {
			builder.append("registrationStatus=");
			builder.append(getRegistrationStatus());
			builder.append(", ");
		}
		builder.append("enabled=");
		builder.append(isEnabled());
		builder.append(", connectorCount=");
		builder.append(getConnectorCount());
		builder.append(", info=");
		builder.append(getInfo());
		builder.append("}");
		return builder.toString();
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

	/**
	 * The associated node ID.
	 * 
	 * @return the node ID
	 */
	@Override
	public Long getNodeId() {
		return nodeId;
	}

}
