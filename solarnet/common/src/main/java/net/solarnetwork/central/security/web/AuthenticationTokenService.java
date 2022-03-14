/* ==================================================================
 * AuthenticationTokenService.java - 30/05/2018 9:37:31 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.web;

import java.util.Map;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.web.security.AuthenticationScheme;

/**
 * API for a service that performs functions on authentication data tokens.
 * 
 * @author matt
 * @version 2.0
 * @since 1.10
 */
public interface AuthenticationTokenService {

	/** An {@link java.time.Instant} property to be used as the signing date. */
	String SIGN_DATE_PROP = "signDate";

	/**
	 * Compute a token signing key from a token and properties.
	 * 
	 * <p>
	 * The following properties are used:
	 * </p>
	 * 
	 * <dl>
	 * <dt>{@link #SIGN_DATE_PROP}</dt>
	 * <dd>For the V2 scheme, the signing date to use. Must be a
	 * {@link java.time.Instant} object.</dd>
	 * </dl>
	 * 
	 * @param scheme
	 *        the token scheme to use
	 * @param token
	 *        the token
	 * @param properties
	 *        the signing properties
	 * @return the new signing key to use
	 */
	byte[] computeAuthenticationTokenSigningKey(AuthenticationScheme scheme, SecurityToken token,
			Map<String, ?> properties);

}
