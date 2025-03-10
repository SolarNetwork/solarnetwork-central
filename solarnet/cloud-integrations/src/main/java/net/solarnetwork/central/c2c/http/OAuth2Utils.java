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

import static java.lang.String.format;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_SECRET_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.PASSWORD_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.USERNAME_SETTING;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.eventForConfiguration;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.service.RemoteServiceException;

/**
 * OAuth2 utilities.
 *
 * @author matt
 * @version 1.1
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

	/**
	 * Add appropriate OAuth authorization header values to a given
	 * {@link HttpHeaders}.
	 *
	 * <p>
	 * If the {@link CloudIntegrationService#USERNAME_SETTING} and
	 * {@link CloudIntegrationService#PASSWORD_SETTING} service properties are
	 * available, they will be used to create a
	 * {@link UsernamePasswordAuthenticationToken} principal. Otherwise, a
	 * string will be created like {@code "I N"} where {@code I} is the
	 * configuration's ID's identifier and {@code N} is the configuration name.
	 * </p>
	 *
	 * @param config
	 *        the configuration to authorize
	 * @param headers
	 *        the headers to add authorization to
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param lockProvider
	 *        if provided then obtain a lock before acquiring the token; this
	 *        can be used in prevent concurrent requests using the same
	 *        {@code config} from making multiple token requests
	 * @throws RemoteServiceException
	 *         if authorization fails
	 * @throws net.solarnetwork.central.security.AuthorizationException
	 *         if an OAuth client is not returned by the
	 *         {@code oauthClientManager}
	 */
	public static void addOAuthBearerAuthorization(CloudIntegrationConfiguration config,
			HttpHeaders headers, OAuth2AuthorizedClientManager oauthClientManager,
			UserEventAppenderBiz userEventAppenderBiz,
			Function<UserLongCompositePK, Lock> lockProvider) {
		final String username = config.serviceProperty(USERNAME_SETTING, String.class);
		final String password = config.serviceProperty(PASSWORD_SETTING, String.class);
		final OAuth2AuthorizeRequest.Builder authReq = OAuth2AuthorizeRequest
				.withClientRegistrationId(config.systemIdentifier());
		if ( username != null && !username.isEmpty() && password != null && !password.isEmpty() ) {
			authReq.principal(new UsernamePasswordAuthenticationToken(username, password));
		} else if ( config.hasServiceProperty(OAUTH_CLIENT_ID_SETTING, String.class) ) {
			if ( config.hasServiceProperty(OAUTH_CLIENT_SECRET_SETTING, String.class) ) {
				authReq.principal(new UsernamePasswordAuthenticationToken(
						config.serviceProperty(OAUTH_CLIENT_ID_SETTING, String.class),
						config.serviceProperty(OAUTH_CLIENT_SECRET_SETTING, String.class)));
			} else {
				authReq.principal(config.serviceProperty(OAUTH_CLIENT_ID_SETTING, String.class));
			}
		} else {
			authReq.principal("%s %s".formatted(config.getId().ident(), config.getName()));
		}

		final Lock lock = (lockProvider != null ? lockProvider.apply(config.getId()) : null);
		try {
			if ( lock != null ) {
				lock.lock();
			}
			OAuth2AuthorizedClient oauthClient = requireNonNullObject(
					oauthClientManager.authorize(authReq.build()), "oauthClient");
			OAuth2AccessToken accessToken = oauthClient.getAccessToken();
			headers.add("Authorization", "Bearer " + accessToken.getTokenValue());
		} catch ( OAuth2AuthorizationException e ) {
			userEventAppenderBiz.addEvent(config.getUserId(),
					eventForConfiguration(config.getId(),
							CloudIntegrationsUserEvents.INTEGRATION_AUTH_ERROR_TAGS,
							format("OAuth error: %s", e.getMessage())));
			throw new RemoteServiceException("Error authenticating to cloud integration %d: %s"
					.formatted(config.getConfigId(), e.getMessage()), e);
		} finally {
			if ( lock != null ) {
				lock.unlock();
			}
		}
	}

}
