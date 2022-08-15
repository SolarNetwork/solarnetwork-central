/* ==================================================================
 * BaseOscpConfigurationInput.java - 15/08/2022 1:01:44 pm
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

package net.solarnetwork.central.user.oscp.domain;

import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import net.solarnetwork.central.oscp.domain.BaseOscpConfigurationEntity;
import net.solarnetwork.util.ObjectUtils;

/**
 * Base DTO for OSCP configuration.
 * 
 * @param <T>
 *        the configuration type
 * @author matt
 * @version 1.0
 */
public abstract class BaseOscpConfigurationInput<T extends BaseOscpConfigurationEntity<T>>
		implements OscpConfigurationInput<T> {

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String name;

	private boolean enabled;

	private Map<String, Object> serviceProps;

	/**
	 * Populate input properties onto a configuration instance.
	 * 
	 * @param conf
	 *        the configuration to populate
	 */
	protected void populateConfiguration(T conf) {
		ObjectUtils.requireNonNullArgument(conf, "conf");
		if ( conf.getId().entityIdIsAssigned() ) {
			conf.setModified(conf.getCreated());
		}
		conf.setName(name);
		conf.setEnabled(enabled);
		conf.setServiceProps(serviceProps);
	}

	/**
	 * Get a display name for the configuration.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set a display name for the configuration.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the enabled flag.
	 * 
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled flag.
	 * 
	 * @param enabled
	 *        the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Get the service properties.
	 * 
	 * @return the serviceProps
	 */
	public Map<String, Object> getServiceProps() {
		return serviceProps;
	}

	/**
	 * Set the service properties.
	 * 
	 * @param serviceProps
	 *        the serviceProps to set
	 */
	public void setServiceProps(Map<String, Object> serviceProps) {
		this.serviceProps = serviceProps;
	}

}
