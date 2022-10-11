/* ==================================================================
 * TaskContext.java - 1/09/2022 5:09:49 pm
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

package net.solarnetwork.central.oscp.util;

import static net.solarnetwork.central.oscp.domain.OscpUserEvents.eventForConfiguration;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.oscp.dao.ExternalSystemConfigurationDao;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemConfigurationException;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * API for system tasks.
 * 
 * @author matt
 * @version 1.0
 */
public interface TaskContext<C extends BaseOscpExternalSystemConfiguration<C>> {

	/**
	 * Get a task name.
	 * 
	 * @return the task name
	 */
	String name();

	/**
	 * Get the role.
	 * 
	 * @return the role
	 */
	OscpRole role();

	/**
	 * Get the configuration for the system the task is for.
	 * 
	 * @return the configuration
	 */
	C config();

	/**
	 * Get event tags to use for error events.
	 * 
	 * @return the error tags
	 */
	String[] errorEventTags();

	/**
	 * Get event tags to use for success events.
	 * 
	 * @return the success tags
	 */
	String[] successEventTags();

	/**
	 * The configuration DAO.
	 * 
	 * @return the DAO
	 */
	ExternalSystemConfigurationDao<C> dao();

	/**
	 * Get task request parameters.
	 * 
	 * @return the parameters
	 */
	Map<String, ?> parameters();

	/**
	 * Get a URI for the configuration URL.
	 * 
	 * @param path
	 *        the URL path, to add to the base URL
	 * @param extraErrorTags
	 *        error tags to include in a user event if an error occurs
	 * @return the URI, or {@literal null} if none configured
	 * @throws ExternalSystemConfigurationException
	 *         if an error occurs
	 */
	default URI systemUri(String path, String... extraErrorTags) {
		String baseUrl = config().getBaseUrl();
		if ( baseUrl == null || baseUrl.isBlank() ) {
			return null;
		}
		try {
			return URI.create(config().getBaseUrl() + path);
		} catch ( IllegalArgumentException | NullPointerException e ) {
			var msg = "[%s] task with %s %s failed because the OSCP URL [%s] is not valid: %s".formatted(
					name(), role(), config().getId().ident(), config().getBaseUrl(), e.getMessage());
			LogEventInfo event = eventForConfiguration(config(), errorEventTags(), "Invalid URL",
					extraErrorTags);
			throw new ExternalSystemConfigurationException(role(), config(), event, msg);
		}
	}

	/**
	 * Get the configuration authorization token.
	 * 
	 * @param dao
	 *        the DAO to use
	 * @param extraErrorTags
	 *        error tags to include in a user event if an error occurs
	 * @return the token
	 */
	default String authToken(String... extraErrorTags) {
		String authToken = dao().getExternalSystemAuthToken(config().getId());
		if ( authToken == null ) {
			var msg = "[%s] task with %s %s failed because the authorization token is not available."
					.formatted(name(), role(), config().getId().ident());
			LogEventInfo event = eventForConfiguration(config(), errorEventTags(),
					"Missing authorization token");
			throw new ExternalSystemConfigurationException(role(), config(), event, msg);
		}
		return authToken;
	}

	/**
	 * Verify the external system uses a supported version.
	 * 
	 * @param supportedVersions
	 *        the supported OSCP versions
	 * @param extraErrorTags
	 *        error tags to include in a user event if an error occurs
	 */
	default void verifySystemOscpVersion(Set<String> supportedVersions, String... extraErrorTags) {
		if ( !supportedVersions.contains(config().getOscpVersion()) ) {
			var msg = "[%s] task with %s %s failed because the OSCP version %s is not supported."
					.formatted(name(), role(), config().getId().ident(), config().getOscpVersion());
			LogEventInfo event = eventForConfiguration(config(), errorEventTags(),
					"Unsupported OSCP version");
			throw new ExternalSystemConfigurationException(role(), config(), event, msg);
		}
	}

}
