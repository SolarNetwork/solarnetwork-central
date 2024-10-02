/* ==================================================================
 * OAuth2Utils.java - 3/10/2024 11:21:59 am
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

package net.solarnetwork.central.c2c.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;

/**
 * OAuth2 utilities.
 *
 * @author matt
 * @version 1.0
 */
public final class OAuth2Utils {

	private OAuth2Utils() {
		// not available
	}

	/**
	 * OAuth2 context attributes mapper function for username/password flow
	 * attributes obtained from the request principal.
	 *
	 * @param authReq
	 *        the request to provide the context attributes for
	 * @return the attributes, never {@literal null}
	 */
	public static Map<String, Object> principalCredentialsContextAttributes(
			OAuth2AuthorizeRequest authReq) {
		Map<String, Object> contextAttributes = Collections.emptyMap();
		Authentication principal = authReq.getPrincipal();
		if ( principal.getPrincipal() != null && principal.getCredentials() != null ) {
			contextAttributes = new HashMap<>(4);
			contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME,
					principal.getPrincipal());
			contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME,
					principal.getCredentials());
		}
		return contextAttributes;
	}

}
