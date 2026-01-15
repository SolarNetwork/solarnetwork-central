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

package net.solarnetwork.central.common.http;

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.service.RemoteServiceException;

/**
 * OAuth2 utilities.
 *
 * @author matt
 * @version 1.1
 */
public final class OAuth2Utils {

	/**
	 * The (deprecated) password authorization grant type.
	 * 
	 * @since 1.1
	 */
	public static final AuthorizationGrantType PASSWORD_GRANT_TYPE = new AuthorizationGrantType(
			"password");

	/**
	 * The name of an attribute in the context associated to the value for the
	 * resource owner's username.
	 * 
	 * @since 1.1
	 */
	public static final String USERNAME_ATTRIBUTE_NAME = OAuth2AuthorizationContext.class.getName()
			.concat(".USERNAME");

	/**
	 * The name of an attribute in the context associated to the value for the
	 * resource owner's password.
	 * 
	 * @since 1.1
	 */
	public static final String PASSWORD_ATTRIBUTE_NAME = OAuth2AuthorizationContext.class.getName()
			.concat(".PASSWORD");

	/**
	 * {@code username} - used in Access Token Request.
	 * 
	 * @since 1.1
	 */
	public static final String USERNAME_PARAMETER_NAME = "username";

	/**
	 * {@code password} - used in Access Token Request.
	 * 
	 * @since 1.1
	 */
	public static final String PASSWORD_PARAMETER_NAME = "password";

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
			contextAttributes.put(USERNAME_ATTRIBUTE_NAME, principal.getPrincipal());
			contextAttributes.put(PASSWORD_ATTRIBUTE_NAME, principal.getCredentials());
		}
		return contextAttributes;
	}

	/**
	 * Generate an OAuth authorization header value.
	 *
	 * @param configId
	 *        the configuration ID related to the authorization
	 * @param registrationId
	 *        the client registration ID
	 * @param principalName
	 *        the user name
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @param lockProvider
	 *        if provided then obtain a lock before acquiring the token; this
	 *        can be used in prevent concurrent requests using the same
	 *        {@code config} from making multiple token requests
	 * @return an entry for an HTTP authorization header
	 * @throws RemoteServiceException
	 *         if authorization fails
	 * @throws net.solarnetwork.central.security.AuthorizationException
	 *         if an OAuth client is not returned by the
	 *         {@code oauthClientManager}
	 */
	public static Entry<String, String> oauthBearerAuthorization(final UserLongCompositePK configId,
			final String registrationId, final String principalName,
			final OAuth2AuthorizedClientManager oauthClientManager,
			final Function<UserLongCompositePK, Lock> lockProvider) {
		final OAuth2AuthorizeRequest.Builder authReq = OAuth2AuthorizeRequest
				.withClientRegistrationId(registrationId).principal(principalName);

		final Lock lock = (lockProvider != null ? lockProvider.apply(configId) : null);
		try {
			if ( lock != null ) {
				lock.lock();
			}
			OAuth2AuthorizedClient oauthClient = requireNonNullObject(
					oauthClientManager.authorize(authReq.build()), "oauthClient");
			OAuth2AccessToken accessToken = oauthClient.getAccessToken();
			return Map.entry("Authorization", "Bearer " + accessToken.getTokenValue());
		} catch ( OAuth2AuthorizationException e ) {
			throw new RemoteServiceException(
					"Error authenticating %s: %s".formatted(configId.ident(), e.getMessage()), e);
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
	}

}
