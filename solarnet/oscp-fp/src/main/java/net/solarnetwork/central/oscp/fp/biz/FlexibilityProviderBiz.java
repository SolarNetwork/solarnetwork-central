/* ==================================================================
 * FlexibilityProviderBiz.java - 16/08/2022 5:17:58 pm
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

package net.solarnetwork.central.oscp.fp.biz;

import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.OscpUserEvents;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.domain.KeyValuePair;

/**
 * Business service API for Flexibility Provider.
 * 
 * @author matt
 * @version 1.0
 */
public interface FlexibilityProviderBiz extends OscpUserEvents {

	/** User event tags for Capacity Provider registration events. */
	String[] CAPACITY_PROVIDER_REGISTER_TAGS = new String[] { OSCP_EVENT_TAG, CAPACITY_PROVIDER_TAG,
			REGISTER_TAG };

	/** User event tags for Capacity Provider registration error events. */
	String[] CAPACITY_PROVIDER_REGISTER_ERROR_TAGS = new String[] { OSCP_EVENT_TAG,
			CAPACITY_PROVIDER_TAG, REGISTER_TAG, ERROR_TAG };

	/**
	 * Register an external system using an authorization token created in
	 * SolarNetwork and shared with the system through an external process (e.g.
	 * email, phone, etc).
	 * 
	 * @param authInfo
	 *        the authorization info to register
	 * @param externalSystemToken
	 *        the authorization token to use when making requests to the
	 *        external system
	 * @param versionUrl
	 *        the external system's OSCP version and base URL to use
	 * @throws AuthorizationException
	 *         with
	 *         {@link AuthorizationException.Reason#REGISTRATION_NOT_CONFIRMED}
	 *         if the authorization token does not exist
	 */
	void register(AuthRoleInfo authInfo, String externalSystemToken, KeyValuePair versionUrl)
			throws AuthorizationException;

}
