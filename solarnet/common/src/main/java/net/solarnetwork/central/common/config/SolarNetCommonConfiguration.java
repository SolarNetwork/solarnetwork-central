/* ==================================================================
 * SolarNetCommonConfiguration.java - 4/10/2021 4:20:43 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Marker interface for the common application configuration package.
 * 
 * @author matt
 * @version 1.3
 */
@Configuration
@ComponentScan
public class SolarNetCommonConfiguration {

	/** A qualifier to use for OAuth client registration. */
	public static final String OAUTH_CLIENT_REGISTRATION = "oauth-client-reg";

	/** A qualifier for audit JDBC access. */
	public static final String AUDIT = "audit";

	/** A qualifier for caching support. */
	public static final String CACHING = "caching";

	/**
	 * A qualifier for HTTP trace support.
	 * 
	 * @since 1.2
	 */
	public static final String HTTP_TRACE = "http-trace";

	/**
	 * A qualifier for disabled HTTP trace support.
	 * 
	 * @since 1.2
	 */
	public static final String NOT_HTTP_TRACE = "!http-trace";

	/**
	 * A qualifier for user service auditor.
	 * 
	 * @since 1.3
	 */
	public static final String USER_SERVICE_AUDITOR = "user-service-auditor";

}
