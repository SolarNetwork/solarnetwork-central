/* ==================================================================
 * AppConfiguration.java - 2/10/2017 10:17:49 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import java.util.Map;

/**
 * API for common application configuration elements.
 * 
 * @author matt
 * @version 1.0
 * @since 1.35
 */
public interface AppConfiguration {

	/** Service URL name for the SolarUser app. */
	String SOLARUSER_SERVICE_NAME = "solaruser";

	/** Service URL name for the SolarQuery app. */
	String SOLARQUERY_SERVICE_NAME = "solarquery";

	/**
	 * Service URL name for a user-facing "dashboard" specific to a single
	 * SolarNode.
	 * 
	 * <p>
	 * This URL can reasonably be expected to support a <code>nodeId</code>
	 * variable.
	 * </p>
	 */
	String SOALRNODE_DASHBAORD_SERVICE_NAME = "node-dashboard";

	/**
	 * Service URL name for a user-facing "dashboard" specific to a single
	 * SolarNode.
	 * 
	 * <p>
	 * This URL can reasonably be expected to support a <code>nodeId</code>
	 * variable.
	 * </p>
	 */
	String SOLARNDOE_DATAVIEW_SERVICE_NAME = "node-dataview";

	/**
	 * Get a mapping of named service URLs.
	 * 
	 * <p>
	 * The keys of the returned maps represent logical names for the associated
	 * URL values. The keys will be application-dependent, and should include
	 * values for well-defined application services. For example a URL to the
	 * application terms of service might be included under a key
	 * {@literal tos}.
	 * </p>
	 * 
	 * <p>
	 * URL values are permitted to contain <em>variables</em> in the form
	 * <code>{var}</code> that consumers of the URLs can replace with
	 * appropriate values. The variable names must be named so their intended
	 * use is obvious, for example <code>nodeId</code> for a SolarNode ID.
	 * </p>
	 * 
	 * <p>
	 * Some common service URL names are defined as constants on this interface.
	 * Implementations are recommended to use these keys when it makes sense,
	 * and to add any other values needed by the application.
	 * </p>
	 * 
	 * @return named service URLs, never {@literal null}
	 */
	Map<String, String> getServiceUrls();

}
