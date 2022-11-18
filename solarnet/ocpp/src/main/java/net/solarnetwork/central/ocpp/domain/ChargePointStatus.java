/* ==================================================================
 * ChargePointStatus.java - 16/11/2022 5:08:46 pm
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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.util.ObjectUtils;

/**
 * OCPP charger overall status.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "created", "userId", "chargePointId", "connectedTo", "connectedDate" })
public class ChargePointStatus extends BasicEntity<UserLongCompositePK>
		implements UserRelatedEntity<UserLongCompositePK> {

	private static final long serialVersionUID = 1717488657665528060L;

	/** The SolarIn instance connected to. */
	private String connectedTo;

	/** The connection date. */
	private Instant connectedDate;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param chargePointId
	 *        the Charge Point ID
	 * @param created
	 *        the creation date
	 * @param connectedTo
	 *        the name of the SolarIn instance the charger is connected to, or
	 *        {@literal null} if not connected
	 * @param connectedDate
	 *        the date the connection was established
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if {@code userId} or {@code chargePointId} is {@literal null}
	 */
	public ChargePointStatus(Long userId, Long chargePointId, Instant created, String connectedTo,
			Instant connectedDate) {
		this(new UserLongCompositePK(userId, chargePointId), created, connectedTo, connectedDate);
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param connectedTo
	 *        the name of the SolarIn instance the charger is connected to, or
	 *        {@literal null} if not connected
	 * @param connectedDate
	 *        the date the connection was established
	 * @return the new instance
	 * @throws IllegalArgumentException
	 *         if {@code id} is {@literal null}
	 */
	public ChargePointStatus(UserLongCompositePK id, Instant created, String connectedTo,
			Instant connectedDate) {
		super(ObjectUtils.requireNonNullArgument(id, "id"), created);
		this.connectedTo = connectedTo;
		this.connectedDate = connectedDate;

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
	public Long getChargePointId() {
		return getId().getEntityId();
	}

	/**
	 * Get the SolarIn instance name the charger is connected to.
	 * 
	 * @return the instance name, or {@literal null} if not connected
	 */
	public String getConnectedTo() {
		return connectedTo;
	}

	/**
	 * Set the SolarIn instance name the charger is connected to.
	 * 
	 * @param connectedTo
	 *        the instance name, or {@literal null} if not connected
	 */
	public void setConnectedTo(String connectedTo) {
		this.connectedTo = connectedTo;
	}

	/**
	 * Get the date the connection was last established.
	 * 
	 * @return the date, or {@literal null} if never connected
	 */
	public Instant getConnectedDate() {
		return connectedDate;
	}

	/**
	 * Set the date the connection was last established.
	 * 
	 * @param connectedDate
	 *        the date to set
	 */
	public void setConnectedDate(Instant connectedDate) {
		this.connectedDate = connectedDate;
	}

}
