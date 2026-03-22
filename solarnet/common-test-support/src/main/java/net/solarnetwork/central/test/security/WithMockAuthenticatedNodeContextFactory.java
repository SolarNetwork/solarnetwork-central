/* ==================================================================
 * WithMockAuthenticatedNodeContextFactory.java - 23/03/2026 11:23:26 am
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

import static java.util.Collections.emptyList;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import net.solarnetwork.central.security.AuthenticatedNode;

/**
 * Mock security context factory for use with {@link WithMockAuthenticatedNode}.
 *
 * @author matt
 * @version 1.0
 */
public class WithMockAuthenticatedNodeContextFactory
		implements WithSecurityContextFactory<WithMockAuthenticatedNode> {

	@Override
	public SecurityContext createSecurityContext(WithMockAuthenticatedNode annotation) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();

		var node = new AuthenticatedNode(Long.valueOf(annotation.nodeId()), emptyList(), false);
		var auth = new TestingAuthenticationToken(node, annotation.nodeId(), "ROLE_NODE");

		context.setAuthentication(auth);
		return context;
	}

}
