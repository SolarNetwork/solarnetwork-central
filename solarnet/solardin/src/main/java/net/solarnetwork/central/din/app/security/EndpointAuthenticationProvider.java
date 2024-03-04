/* ==================================================================
 * EndpointAuthenticationProvider.java - 23/02/2024 1:30:06 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.app.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import net.solarnetwork.central.din.security.CredentialAuthorizationDao;
import net.solarnetwork.central.din.security.EndpointUserDetails;
import net.solarnetwork.util.ObjectUtils;

/**
 * Authentication provider for endpoint credentials.
 *
 * @author matt
 * @version 1.0
 */
public class EndpointAuthenticationProvider implements AuthenticationProvider {

	private final CredentialAuthorizationDao authDao;
	private final PasswordEncoder passwordEncoder;

	/**
	 * Constructor.
	 *
	 * @param authDao
	 *        the authorization DAO
	 * @param passwordEncoder
	 *        the password encoder
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public EndpointAuthenticationProvider(CredentialAuthorizationDao authDao,
			PasswordEncoder passwordEncoder) {
		super();
		this.authDao = ObjectUtils.requireNonNullArgument(authDao, "authDao");
		this.passwordEncoder = ObjectUtils.requireNonNullArgument(passwordEncoder, "passwordEncoder");
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		if ( authentication.getDetails() instanceof EndpointAuthenticationDetails endpointDetails
				&& endpointDetails.getEndpointId() != null
				&& authentication.getPrincipal() instanceof String username
				&& authentication.getCredentials() instanceof String password ) {
			EndpointUserDetails user = authDao.credentialsForEndpoint(endpointDetails.getEndpointId(),
					username);
			if ( user == null ) {
				throw new BadCredentialsException("Bad credentials");
			}
			if ( user != null ) {
				if ( !passwordEncoder.matches(password, user.getPassword()) ) {
					throw new BadCredentialsException("Bad credentials");
				}

				UsernamePasswordAuthenticationToken result = UsernamePasswordAuthenticationToken
						.authenticated(username, password, user.getAuthorities());
				result.setDetails(user);
				result.eraseCredentials();

				return result;
			}
		}
		return null;
	}

}
