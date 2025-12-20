/* ==================================================================
 * OauthHttpOperations.java - 3/12/2025 8:38:56 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static net.solarnetwork.service.OptionalService.service;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import javax.cache.Cache;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.Result;
import net.solarnetwork.service.OptionalService;

/**
 * {@link HttpOperations} that adds OAuth2 authentication support based on a
 * specific {@link OAuth2ClientIdentity}.
 * 
 * @author matt
 * @version 1.0
 */
public class Oauth2HttpOperations implements HttpOperations {

	private final HttpOperations delegate;
	private final OAuth2AuthorizedClientManager oauthClientManager;
	private final OAuth2ClientIdentity identity;
	private final OptionalService<Cache<UserLongCompositePK, Lock>> locksCache;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delegate
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @param locksCache
	 *        the optional locks cache; this cache is assumed to have
	 *        read-through semantics, in that any request for a key not already
	 *        present in the map will return a new lock instance
	 * @param identity
	 *        the OAuth identity
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public Oauth2HttpOperations(HttpOperations delegate,
			OAuth2AuthorizedClientManager oauthClientManager,
			OptionalService<Cache<UserLongCompositePK, Lock>> locksCache,
			OAuth2ClientIdentity identity) {
		super();
		this.delegate = requireNonNullArgument(delegate, "delegate");
		this.oauthClientManager = requireNonNullArgument(oauthClientManager, "oauthClientManager");
		this.locksCache = requireNonNullArgument(locksCache, "locksCache");
		this.identity = requireNonNullArgument(identity, "identity");
	}

	@Override
	public <I, O> ResponseEntity<O> http(final HttpMethod method, final URI uri,
			final HttpHeaders headers, final I body, final Class<O> responseType, final Object context,
			final Map<String, ?> runtimeData) {
		final Cache<UserLongCompositePK, Lock> lockCache = service(locksCache);

		// @formatter:off
		final Entry<String, String> authHeader = OAuth2Utils.oauthBearerAuthorization(
				identity.configId(),
				identity.registrationId(),
				identity.principal(),
				oauthClientManager,
				(lockCache != null ? (id) -> lockCache.get(id) : null)
				);
		// @formatter:on

		HttpHeaders h = headers;
		if ( authHeader != null ) {
			HttpHeaders newHeaders = new HttpHeaders();
			if ( headers != null ) {
				newHeaders.putAll(headers);
			}
			newHeaders.set(authHeader.getKey(), authHeader.getValue());
			h = newHeaders;
		}

		return delegate.http(method, uri, h, body, responseType, context, runtimeData);
	}

	@Override
	public <O> Result<O> httpGet(final String uri, final Map<String, ?> parameters,
			final Map<String, ?> headers, final Class<O> responseType, final Object context,
			final Map<String, ?> runtimeData) {
		final Cache<UserLongCompositePK, Lock> lockCache = service(locksCache);

		// @formatter:off
		final Entry<String, String> authHeader = OAuth2Utils.oauthBearerAuthorization(
				identity.configId(),
				identity.registrationId(),
				identity.principal(),
				oauthClientManager,
				(lockCache != null ? (id) -> lockCache.get(id) : null)
				);
		// @formatter:on

		Map<String, ?> h = headers;
		if ( authHeader != null ) {
			Map<String, Object> newHeaders = new LinkedHashMap<>(headers != null ? headers : Map.of());
			newHeaders.put(authHeader.getKey(), authHeader.getValue());
			h = newHeaders;
		}

		return delegate.httpGet(uri, parameters, h, responseType, context, runtimeData);
	}

}
