/* ==================================================================
 * ExternalSystemJwtAuthenticationConverter.java - 26/08/2022 3:23:39 pm
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

package net.solarnetwork.central.oscp.security;

import static net.solarnetwork.central.oscp.security.OscpSecurityUtils.jwtTokenIdentifier;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.AuthTokenAuthorizationDao;
import net.solarnetwork.central.oscp.domain.AuthRoleContainer;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;

/**
 * Convert JWT into an external system token.
 * 
 * @author matt
 * @version 1.0
 */
public class ExternalSystemJwtAuthenticationConverter
		implements Converter<Jwt, AbstractAuthenticationToken> {

	private final AuthTokenAuthorizationDao dao;
	private final Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter;
	private final String principalClaimName;

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ExternalSystemJwtAuthenticationConverter(AuthTokenAuthorizationDao dao) {
		this(dao, new JwtScopeGrantedAuthoritiesConverter(), JwtClaimNames.SUB);
	}

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ExternalSystemJwtAuthenticationConverter(AuthTokenAuthorizationDao dao,
			Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter,
			String principalClaimName) {
		super();
		this.dao = requireNonNullArgument(dao, "dao");
		this.jwtGrantedAuthoritiesConverter = requireNonNullArgument(jwtGrantedAuthoritiesConverter,
				"jwtGrantedAuthoritiesConverter");
		this.principalClaimName = requireNonNullArgument(principalClaimName, "principalClaimName");
	}

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
		String principalClaimValue = jwt.getClaimAsString(this.principalClaimName);

		String token = jwtTokenIdentifier(jwt.getIssuer(), principalClaimValue);

		UserLongCompositePK authId = dao.idForToken(token, true);
		if ( authId == null ) {
			throw new BadCredentialsException("Invalid JWT token.");
		}
		AuthRoleInfo info = dao.roleForAuthorization(authId);
		if ( info == null ) {
			throw new BadCredentialsException("Invalid JWT token.");
		}
		return new AuthInfoJwtAuthenticatedToken(jwt, authorities, principalClaimValue, info);
	}

	private static class AuthInfoJwtAuthenticatedToken extends JwtAuthenticationToken
			implements AuthRoleContainer {

		private static final long serialVersionUID = 4692483558955660840L;

		private final AuthRoleInfo info;

		public AuthInfoJwtAuthenticatedToken(Jwt jwt, Collection<? extends GrantedAuthority> authorities,
				String name, AuthRoleInfo info) {
			super(jwt, authorities, name);
			this.info = requireNonNullArgument(info, "info");
		}

		@Override
		public AuthRoleInfo getAuthRole() {
			return info;
		}

	}

}
