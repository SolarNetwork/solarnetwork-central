/* ==================================================================
 * ServerControlConfigurationInput.java - 8/08/2023 5:48:28 am
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

package net.solarnetwork.central.user.dnp3.domain;

import static java.time.Instant.now;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;

/**
 * DTO for DNP3 server control configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerControlConfigurationInput
		extends BaseDnp3ConfigurationInput<ServerControlConfiguration, UserLongIntegerCompositePK> {

	@NotNull
	private Long nodeId;

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String controlId;

	@NotNull
	private ControlType type;

	@Override
	public ServerControlConfiguration toEntity(UserLongIntegerCompositePK id) {
		ServerControlConfiguration conf = new ServerControlConfiguration(
				requireNonNullArgument(id, "id"), now());
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(ServerControlConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setNodeId(nodeId);
		conf.setControlId(controlId);
		conf.setType(type);
	}

	/**
	 * Get the datum node ID.
	 * 
	 * @return the nodeId
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the datum node ID.
	 * 
	 * @param nodeId
	 *        the nodeId to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the control ID.
	 * 
	 * @return the controlId
	 */
	public String getControlId() {
		return controlId;
	}

	/**
	 * Set the control ID.
	 * 
	 * @param controlId
	 *        the controlId to set
	 */
	public void setControlId(String controlId) {
		this.controlId = controlId;
	}

	/**
	 * Set the control type.
	 * 
	 * @return the type
	 */
	public ControlType getType() {
		return type;
	}

	/**
	 * Get the control type.
	 * 
	 * @param type
	 *        the control type to set
	 */
	public void setType(ControlType type) {
		this.type = type;
	}

}
