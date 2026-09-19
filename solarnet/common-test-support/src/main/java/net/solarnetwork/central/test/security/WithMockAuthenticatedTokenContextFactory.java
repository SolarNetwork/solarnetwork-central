/* ==================================================================
 * WithMockAuthenticatedTokenContextFactory.java - 23/03/2026 11:22:10 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test.security;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import net.solarnetwork.central.security.AuthenticatedToken;

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
	public SecurityContext createSecurityContext(WithMockAuthenticatedToken annotation) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();

		var token = new AuthenticatedToken(
				new User("user", "", true, true, true, true, AuthorityUtils.NO_AUTHORITIES),
				annotation.type(), Long.valueOf(annotation.userId()), null);
		var auth = new TestingAuthenticationToken(token, annotation.token(), "ROLE_USER");

		context.setAuthentication(auth);
		return context;
	}

}
