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

import net.solarnetwork.central.security.AuthorizationException;

/**
 * Business service API for Flexibility Provider.
 * 
 * @author matt
 * @version 1.0
 */
public interface FlexibilityProviderBiz {

	/**
	 * Register an external system using an authorization token created in
	 * SolarNetwork and shared with the system through an external process (e.g.
	 * email, phone, etc).
	 * 
	 * @param token
	 *        the authorization token to register
	 * @param externalSystemToken
	 *        the authorization token to use when making requests to the
	 *        external system
	 * @return the resulting new authorization token to give to the external
	 *         system form them to use when making requests to us going forward
	 * @throws AuthorizationException
	 *         with
	 *         {@link AuthorizationException.Reason#REGISTRATION_NOT_CONFIRMED}
	 *         if the authorization token does not exist
	 */
	String register(String token, String externalSystemToken) throws AuthorizationException;

}
