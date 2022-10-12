/* ==================================================================
 * ExternalSystemConfiguration.java - 28/08/2022 7:58:20 am
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

/**
 * Read-only API for an external system configuration.
 * 
 * @author matt
 * @version 1.0
 */
public interface ExternalSystemConfiguration {

	/**
	 * Get a display name for the configuration.
	 * 
	 * @return the name
	 */
	String getName();

	/**
	 * Get the authorization role of the configuration.
	 * 
	 * @return the authorization role
	 */
	AuthRoleInfo getAuthRole();

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID
	 */
	Long getUserId();

	/**
	 * Get the configuration ID.
	 * 
	 * @return the configuration ID
	 */
	Long getConfigId();

	/**
	 * Get the ID of the Flexibility Provider associated with this
	 * configuration.
	 * 
	 * @return the flexibility provider ID
	 */
	Long getFlexibilityProviderId();

	/**
	 * Get the enabled flag.
	 * 
	 * @return the enabled
	 */
	boolean isEnabled();

	/**
	 * Test if OAuth client settings are available.
	 * 
	 * @return {@literal true} if {@link #oauthClientSettings()} would return a
	 *         non-{@literal null} instance
	 */
	boolean hasOauthClientSettings();

	/**
	 * Get OAuth client settings, if available.
	 * 
	 * @return the OAuth client settings, or {@literal null} if not available
	 */
	OAuthClientSettings oauthClientSettings();

	/**
	 * Flag to indicate if asset measurement messages should be used in place of
	 * group measurement messages.
	 * 
	 * @return {@literal true} if asset measurements should be used
	 */
	boolean useGroupAssetMeasurement();

}
