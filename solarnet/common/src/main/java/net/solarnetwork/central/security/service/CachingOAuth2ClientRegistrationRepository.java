/* ==================================================================
 * CachingOAuth2ClientRegistrationRepository.java - 14/10/2024 6:04:58 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import javax.cache.Cache;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Implementation of {@link ClientRegistrationRepository} with caching support.
 * 
 * @author matt
 * @version 1.0
 */
public class CachingOAuth2ClientRegistrationRepository implements ClientRegistrationRepository {

	private final Cache<String, ClientRegistration> cache;
	private final ClientRegistrationRepository delegate;

	/**
	 * Constructor.
	 * 
	 * @param cache
	 *        the cache
	 * @param delegate
	 *        the service to delegate to
	 */
	public CachingOAuth2ClientRegistrationRepository(Cache<String, ClientRegistration> cache,
			ClientRegistrationRepository delegate) {
		super();
		this.cache = requireNonNullArgument(cache, "cache");
		this.delegate = requireNonNullArgument(delegate, "delegate");
	}

	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		ClientRegistration result = cache.get(registrationId);
		if ( result != null ) {
			return result;
		}
		result = delegate.findByRegistrationId(registrationId);
		if ( result != null ) {
			cache.put(registrationId, result);
		}
		return result;
	}

}
