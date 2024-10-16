/* ==================================================================
 * RetryingOAuth2AuthorizedClientManager.java - 16/10/2024 12:28:17 pm
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

package net.solarnetwork.central.security.service;

import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import net.solarnetwork.util.ObjectUtils;

/**
 * {@link OAuth2AuthorizedClientManager} that re-tries when authorization fails,
 * after removing the authorized client from the client service.
 * 
 * <p>
 * The reason for this implementation is that sometimes a refresh token is used
 * but it turns out to be invalidated. By removing the authorized client, a new
 * token is forced.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class RetryingOAuth2AuthorizedClientManager implements OAuth2AuthorizedClientManager {

	private final OAuth2AuthorizedClientManager delegate;
	private final OAuth2AuthorizedClientService clientService;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate manager
	 * @param clientService
	 *        the client service
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public RetryingOAuth2AuthorizedClientManager(OAuth2AuthorizedClientManager delegate,
			OAuth2AuthorizedClientService clientService) {
		super();
		this.delegate = ObjectUtils.requireNonNullArgument(delegate, "delegate");
		this.clientService = ObjectUtils.requireNonNullArgument(clientService, "clientService");
	}

	@Override
	public OAuth2AuthorizedClient authorize(OAuth2AuthorizeRequest authorizeRequest) {
		try {
			return delegate.authorize(authorizeRequest);
		} catch ( ClientAuthorizationException e ) {
			// try resetting authorized client, maybe refresh token invalid
			clientService.removeAuthorizedClient(e.getClientRegistrationId(),
					authorizeRequest.getPrincipal().getName());
			return delegate.authorize(authorizeRequest);
		}
	}

}
