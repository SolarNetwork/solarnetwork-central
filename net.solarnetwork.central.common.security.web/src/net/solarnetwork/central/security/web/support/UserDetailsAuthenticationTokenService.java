/* ==================================================================
 * UserDetailsAuthenticationTokenService.java - 30/05/2018 7:40:34 AM
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

package net.solarnetwork.central.security.web.support;

import java.util.Date;
import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.web.AuthenticationTokenService;
import net.solarnetwork.web.security.AuthenticationScheme;
import net.solarnetwork.web.security.AuthorizationV2Builder;

/**
 * Implementation of {@link AuthenticationTokenService} that uses a
 * {@link UserDetailsService} to access token data.
 * 
 * @author matt
 * @version 1.0
 * @since 1.10
 */
public class UserDetailsAuthenticationTokenService implements AuthenticationTokenService {

	private final UserDetailsService userDetailsService;

	public UserDetailsAuthenticationTokenService(UserDetailsService userDetailsService) {
		super();
		this.userDetailsService = userDetailsService;
	}

	@Override
	public byte[] computeAuthenticationTokenSigningKey(AuthenticationScheme scheme, SecurityToken token,
			Map<String, ?> properties) {
		UserDetails user = userDetailsService.loadUserByUsername(token.getToken());
		switch (scheme) {
			case V1:
				return user.getPassword().getBytes();

			case V2: {
				if ( !(properties.get(SIGN_DATE_PROP) instanceof Date) ) {
					throw new IllegalArgumentException(
							"The " + SIGN_DATE_PROP + " property must be a Date instance");
				}
				return AuthorizationV2Builder.computeSigningKey(user.getPassword(),
						(Date) properties.get(SIGN_DATE_PROP));
			}

			default:
				throw new UnsupportedOperationException("Scheme " + scheme + " not supported");

		}
	}

}
