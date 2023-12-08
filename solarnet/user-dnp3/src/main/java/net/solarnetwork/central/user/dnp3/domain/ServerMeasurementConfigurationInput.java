/* ==================================================================
 * ServerMeasurementConfigurationInput.java - 8/08/2023 5:48:28 am
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;

/**
 * DTO for DNP3 server control configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerMeasurementConfigurationInput extends
		BaseServerDatumStreamConfigurationInput<ServerMeasurementConfiguration, MeasurementType> {

	@Override
	public ServerMeasurementConfiguration toEntity(UserLongIntegerCompositePK id, Instant date) {
		ServerMeasurementConfiguration conf = new ServerMeasurementConfiguration(
				requireNonNullArgument(id, "id"), date);
		populateConfiguration(conf);
		return conf;
	}

	/**
	 * Get the datum property name.
	 * 
	 * @return the property
	 */
	@NotNull
	@NotBlank
	@Override
	public String getProperty() {
		return super.getProperty();
	}

	/**
	 * Set the datum property name.
	 * 
	 * @param property
	 *        the property to set
	 */
	@Override
	public void setProperty(String property) {
		super.setProperty(property);
	}

}
