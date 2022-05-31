/* ==================================================================
 * WithMockSecurityUserContextFactory.java - 30/05/2022 9:22:38 am
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

package net.solarnetwork.central.reg.test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import net.solarnetwork.central.security.AuthenticatedUser;

/**
 * Mock security context factory for use with {@link WithMockSecurityUser}.
 * 
 * @author matt
 * @version 1.0
 */
public class WithMockSecurityUserContextFactory
		implements WithSecurityContextFactory<WithMockSecurityUser> {

	@Override
	public SecurityContext createSecurityContext(WithMockSecurityUser customUser) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		User user = new User(customUser.username(), "password",
				AuthorityUtils.createAuthorityList("ROLE_USER"));
		AuthenticatedUser principal = new AuthenticatedUser(user, Long.valueOf(customUser.userId()),
				customUser.name(), false);
		Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password",
				principal.getAuthorities());
		context.setAuthentication(auth);
		return context;
	}
}
