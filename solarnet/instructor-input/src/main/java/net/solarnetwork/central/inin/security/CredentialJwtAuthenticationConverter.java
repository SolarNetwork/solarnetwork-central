/* ==================================================================
 * CredentialJwtAuthenticationConverter.java - 12/04/2024 9:43:19 am
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

package net.solarnetwork.central.inin.security;

import static net.solarnetwork.central.inin.security.SecurityUtils.jwtTokenIdentifier;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Convert a JWT into an authentication token.
 *
 * @author matt
 * @version 1.0
 */
public class CredentialJwtAuthenticationConverter
		implements Converter<Jwt, AbstractAuthenticationToken> {

	private final CredentialAuthorizationDao authDao;
	private final Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter;
	private final String principalClaimName;

	/**
	 * Constructor.
	 *
	 * @param authDao
	 *        the authorization DAO
	 */
	public CredentialJwtAuthenticationConverter(CredentialAuthorizationDao authDao) {
		this(authDao, new JwtScopeGrantedAuthoritiesConverter(), JwtClaimNames.SUB);
	}

	/**
	 * Constructor.
	 *
	 * @param authDao
	 *        the authorization DAO
	 */
	public CredentialJwtAuthenticationConverter(CredentialAuthorizationDao authDao,
			Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter,
			String principalClaimName) {
		super();
		this.authDao = requireNonNullArgument(authDao, "authDao");
		this.jwtGrantedAuthoritiesConverter = requireNonNullArgument(jwtGrantedAuthoritiesConverter,
				"jwtGrantedAuthoritiesConverter");
		this.principalClaimName = requireNonNullArgument(principalClaimName, "principalClaimName");
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
		String principalClaimValue = jwt.getClaimAsString(this.principalClaimName);

		String token = jwtTokenIdentifier(jwt.getIssuer(), principalClaimValue);

		EndpointUserDetails info = authDao.oAuthCredentials(token);
		if ( info == null ) {
			throw new BadCredentialsException("Invalid JWT token.");
		}
		return new EndpointJwtAuthenticatedToken(jwt, authorities, principalClaimValue, info);
	}

	private static class EndpointJwtAuthenticatedToken extends JwtAuthenticationToken
			implements SecurityEndpointCredential {

		private static final long serialVersionUID = 4692483558955660840L;

		private final EndpointUserDetails info;

		public EndpointJwtAuthenticatedToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities,
				String name, EndpointUserDetails info) {
			super(jwt, authorities, name);
			this.info = requireNonNullArgument(info, "info");
		}

		@Override
		public boolean isAuthenticatedWithToken() {
			return true;
		}

		@Override
		public Long getUserId() {
			return info.getUserId();
		}

		@Override
		public UUID getEndpointId() {
			return info.getEndpointId();
		}

		@Override
		public String getUsername() {
			return info.getUsername();
		}

		@Override
		public boolean isOauth() {
			return true;
		}

	}

}
