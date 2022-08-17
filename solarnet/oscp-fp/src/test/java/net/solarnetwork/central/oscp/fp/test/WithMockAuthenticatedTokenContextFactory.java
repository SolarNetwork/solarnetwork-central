/* ==================================================================
 * WithMockAuthenticatedTokenContextFactory.java - 30/05/2022 9:22:38 am
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

package net.solarnetwork.central.oscp.fp.test;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.security.OscpAuthenticatedToken;

/**
 * Mock security context factory for use with
 * {@link WithMockAuthenticatedToken}.
 * 
 * @author matt
 * @version 1.0
 */
public class WithMockAuthenticatedTokenContextFactory
		implements WithSecurityContextFactory<WithMockAuthenticatedToken> {

	@Override
	public SecurityContext createSecurityContext(WithMockAuthenticatedToken mockToken) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();

		AuthRoleInfo info = new AuthRoleInfo(new UserLongCompositePK(Long.valueOf(mockToken.userId()),
				Long.valueOf(mockToken.entityId())), OscpRole.forAlias(mockToken.role()));
		OscpAuthenticatedToken details = new OscpAuthenticatedToken(info);
		PreAuthenticatedAuthenticationToken auth = new PreAuthenticatedAuthenticationToken(
				mockToken.token(), "N/A", details.getAuthorities());
		auth.setDetails(details);

		context.setAuthentication(auth);
		return context;
	}
}
