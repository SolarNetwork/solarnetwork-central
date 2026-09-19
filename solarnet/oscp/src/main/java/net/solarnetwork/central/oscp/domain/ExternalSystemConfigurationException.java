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

import java.io.Serial;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;

/**
 * Exception dealing with an external system configuration.
 *
 * @author matt
 * @version 1.1
 */
public class ExternalSystemConfigurationException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 6434518762591209261L;

	private final OscpRole role;
	private final @Nullable BaseOscpExternalSystemConfiguration<?> config;
	private final LogEventInfo event;
	private final UserRelatedCompositeKey<?> configId;

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
	 * @param cause
	 *        the cause
	 */
	public ExternalSystemConfigurationException(OscpRole role,
			BaseOscpExternalSystemConfiguration<?> config, LogEventInfo event, String message,
			@Nullable Throwable cause) {
		super(message, cause);
		this.role = role;
		this.config = config;
		this.event = event;
		this.configId = config.id();
	}

	/**
	 * Constructor.
	 *
	 * @param role
	 *        the OSCP role
	 * @param configId
	 *        the external system configuration ID
	 * @param event
	 *        the event
	 * @param message
	 *        the message
	 * @param cause
	 *        the cause
	 */
	public ExternalSystemConfigurationException(OscpRole role, UserRelatedCompositeKey<?> configId,
			LogEventInfo event, String message, @Nullable Throwable cause) {
		super(message, cause);
		this.role = role;
		this.config = null;
		this.event = event;
		this.configId = configId;
	}

	/**
	 * Get the OSCP role.
	 *
	 * @return the role
	 */
	public final OscpRole getRole() {
		return role;
	}

	/**
	 * Get the external system configuration.
	 *
	 * @return the configuration
	 */
	public final @Nullable BaseOscpExternalSystemConfiguration<?> getConfig() {
		return config;
	}

	/**
	 * Test if the external system configuration is available.
	 *
	 * @return {@code true} if the configuration is available
	 * @since 1.1
	 */
	public final boolean hasConfig() {
		return (config != null);
	}

	/**
	 * Get the external system configuration.
	 *
	 * <p>
	 * This is a nullability shortcut, to be used after {@link #hasConfig()}
	 * returns {@code true}.
	 * </p>
	 *
	 * @return the configuration (presumed non-null)
	 */
	@SuppressWarnings("NullAway")
	public final BaseOscpExternalSystemConfiguration<?> config() {
		return config;
	}

	/**
	 * Get the event.
	 *
	 * @return the event
	 */
	public final LogEventInfo getEvent() {
		return event;
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the config ID
	 * @since 1.1
	 */
	public final UserRelatedCompositeKey<?> getConfigId() {
		return configId;
	}

}
