/* ==================================================================
 * ExternalSystemConfigurationException.java - 20/08/2022 1:52:22 pm
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

package net.solarnetwork.central.oscp.domain;

import net.solarnetwork.central.domain.LogEventInfo;

/**
 * Exception dealing with an external system configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class ExternalSystemConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1509475900790689710L;

	private final OscpRole role;
	private final BaseOscpExternalSystemConfiguration<?> config;
	private final LogEventInfo event;

	/**
	 * Constructor.
	 * 
	 * @param role
	 *        the OSCP role
	 * @param config
	 *        the external system configuration
	 * @param event
	 *        the event
	 * @param message
	 *        the message
	 */
	public ExternalSystemConfigurationException(OscpRole role,
			BaseOscpExternalSystemConfiguration<?> config, LogEventInfo event, String message) {
		this(role, config, event, message, null);
	}

	/**
	 * @param role
	 *        the OSCP role
	 * @param config
	 *        the external system configuration
	 * @param event
	 *        the event
	 * @param message
	 *        the message
	 * @param cause
	 *        the cause
	 */
	public ExternalSystemConfigurationException(OscpRole role,
			BaseOscpExternalSystemConfiguration<?> config, LogEventInfo event, String message,
			Throwable cause) {
		super(message, cause);
		this.role = role;
		this.config = config;
		this.event = event;
	}

	/**
	 * Get the OSCP role.
	 * 
	 * @return the role
	 */
	public OscpRole getRole() {
		return role;
	}

	/**
	 * Get the external system configuration.
	 * 
	 * @return the configuration
	 */
	public BaseOscpExternalSystemConfiguration<?> getConfig() {
		return config;
	}

	/**
	 * Get the event.
	 * 
	 * @return the event
	 */
	public LogEventInfo getEvent() {
		return event;
	}

}
