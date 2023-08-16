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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
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
		extends BaseServerDatumStreamConfigurationInput<ServerControlConfiguration, ControlType> {

	@Override
	public ServerControlConfiguration toEntity(UserLongIntegerCompositePK id, Instant date) {
		ServerControlConfiguration conf = new ServerControlConfiguration(
				requireNonNullArgument(id, "id"), date);
		populateConfiguration(conf);
		return conf;
	}

	/**
	 * Get the control ID.
	 * <p>
	 * This is an alias for {@link #getSourceId()}.
	 * </p>
	 * 
	 * @return the control ID
	 */
	public String getControlId() {
		return getSourceId();
	}

	/**
	 * Set the control ID.
	 * <p>
	 * This is an alias for {@link #setSourceId(String)}.
	 * </p>
	 * 
	 * @param controlId
	 *        the control ID to set
	 */
	public void setControlId(String controlId) {
		setSourceId(controlId);
	}

}
