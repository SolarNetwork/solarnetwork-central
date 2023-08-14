/* ==================================================================
 * ServerMeasurementConfiguration.java - 6/08/2023 12:20:33 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.domain;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;

/**
 * DNP3 server control configuration.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id", "sourceId", "valid" })
@JsonPropertyOrder({ "userId", "serverId", "index", "created", "modified", "enabled", "nodeId",
		"controlId", "property", "type", "multiplier", "offset", "scale" })
public final class ServerControlConfiguration
		extends BaseServerDatumStreamConfiguration<ServerControlConfiguration, ControlType> {

	private static final long serialVersionUID = -5228026661855816666L;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ServerControlConfiguration(UserLongIntegerCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the user ID
	 * @param serverId
	 *        the server ID
	 * @param index
	 *        the index
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ServerControlConfiguration(Long userId, Long serverId, Integer index, Instant created) {
		this(new UserLongIntegerCompositePK(userId, serverId, index), created);
	}

	@Override
	public ServerControlConfiguration copyWithId(UserLongIntegerCompositePK id) {
		var copy = new ServerControlConfiguration(id, getCreated());
		copyTo(copy);
		return copy;
	}

	/**
	 * Test if this configuration is valid.
	 * 
	 * <p>
	 * This only checks the existence and non-blankness of the fields necessary
	 * to configure in DNP3.
	 * </p>
	 * 
	 * @return {@literal true} if the configuration is valid
	 */
	@Override
	public boolean isValid() {
		final Long nodeId = getNodeId();
		final String controlId = getControlId();
		final ControlType type = getType();
		return (nodeId != null && controlId != null && type != null && !controlId.isBlank());
	}

	/**
	 * Get the control ID.
	 * 
	 * <p>
	 * This is an alias for {@link #getSourceId()}>
	 * </p>
	 * 
	 * @return the controlId
	 */
	public String getControlId() {
		return getSourceId();
	}

	/**
	 * Set the control ID.
	 * 
	 * <p>
	 * This is an alias for {@link #setSourceId(String)}>
	 * </p>
	 * 
	 * @param controlId
	 *        the controlId to set
	 */
	public void setControlId(String controlId) {
		setSourceId(controlId);
	}

}
