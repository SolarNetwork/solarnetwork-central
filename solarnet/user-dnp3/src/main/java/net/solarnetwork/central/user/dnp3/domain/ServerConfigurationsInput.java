/* ==================================================================
 * ServerConfigurations.java - 12/08/2023 8:39:05 am
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

import java.util.List;
import javax.validation.Valid;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Measurement and control configurations combined.
 * 
 * @author matt
 * @version 1.0
 */
public class ServerConfigurationsInput {

	@Valid
	private List<ServerMeasurementConfigurationInput> measurementConfigs;

	@Valid
	private List<ServerControlConfigurationInput> controlConfigs;

	/**
	 * Constructor.
	 */
	public ServerConfigurationsInput() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param measurementConfigs
	 *        the measurement configurations
	 * @param controlConfigs
	 *        the control configurations
	 */
	public ServerConfigurationsInput(@Valid List<ServerMeasurementConfigurationInput> measurementConfigs,
			@Valid List<ServerControlConfigurationInput> controlConfigs) {
		super();
		this.measurementConfigs = measurementConfigs;
		this.controlConfigs = controlConfigs;
	}

	/**
	 * Return {@literal true} if there are no measurement or control
	 * configurations.
	 * 
	 * @return {@literal true} if empty
	 */
	@JsonIgnore
	public boolean isEmpty() {
		return ((measurementConfigs == null || measurementConfigs.isEmpty())
				&& (controlConfigs == null || controlConfigs.isEmpty()));
	}

	/**
	 * Get the measurement configurations.
	 * 
	 * @return the configurations
	 */
	public List<ServerMeasurementConfigurationInput> getMeasurementConfigs() {
		return measurementConfigs;
	}

	/**
	 * Set the measurement configurations.
	 * 
	 * @param measurementConfigs
	 *        the configurations to set
	 */
	public void setMeasurementConfigs(List<ServerMeasurementConfigurationInput> measurementConfigs) {
		this.measurementConfigs = measurementConfigs;
	}

	/**
	 * Get the control configurations.
	 * 
	 * @return the configurations
	 */
	public List<ServerControlConfigurationInput> getControlConfigs() {
		return controlConfigs;
	}

	/**
	 * Set the control configurations.
	 * 
	 * @param controlConfigs
	 *        the configurations to set
	 */
	public void setControlConfigs(List<ServerControlConfigurationInput> controlConfigs) {
		this.controlConfigs = controlConfigs;
	}

}
