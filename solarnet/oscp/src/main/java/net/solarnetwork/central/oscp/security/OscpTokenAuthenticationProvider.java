/* ==================================================================
 * OscpTokenAuthenticationProvider.java - 17/08/2022 9:51:06 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.AuthTokenAuthorizationDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;

/**
 * Filter to handle OSCP "Token" authorization.
 * 
 * @author matt
 * @version 1.0
 */
public class OscpTokenAuthenticationProvider implements AuthenticationProvider {

	private final AuthTokenAuthorizationDao dao;

	/**
	 * Constructor.
	 * 
	 * @param dao
	 *        the DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public OscpTokenAuthenticationProvider(AuthTokenAuthorizationDao dao) {
		super();
		this.dao = requireNonNullArgument(dao, "dao");
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String token = authentication.getPrincipal().toString();
		UserLongCompositePK authId = dao.idForToken(token);
		if ( authId == null ) {
			throw new BadCredentialsException("Invalid OSCP token.");
		}
		AuthRoleInfo info = dao.roleForAuthorization(authId);
		if ( info == null ) {
			throw new BadCredentialsException("Invalid OSCP token.");
		}
		OscpAuthenticatedToken details = new OscpAuthenticatedToken(info);
		PreAuthenticatedAuthenticationToken result = new PreAuthenticatedAuthenticationToken(token,
				"N/A", details.getAuthorities());
		result.setDetails(details);
		return result;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);
	}

}
