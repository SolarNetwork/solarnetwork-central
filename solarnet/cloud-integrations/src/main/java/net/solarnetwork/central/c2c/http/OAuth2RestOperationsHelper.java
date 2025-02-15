/* ==================================================================
 * OAuth2RestOperationsHelper.java - 7/10/2024 8:57:02 am
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

import static net.solarnetwork.central.c2c.http.OAuth2Utils.addOAuthBearerAuthorization;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;

/**
 * Helper for HTTP interactions using {@link RestOperations} with
 * {@link OAuth2AuthorizedClientManager} support.
 *
 * @author matt
 * @version 1.0
 */
public class OAuth2RestOperationsHelper extends RestOperationsHelper {

	/** The OAuth client manager. */
	protected final OAuth2AuthorizedClientManager oauthClientManager;

	/**
	 * Constructor.
	 *
	 * @param log
	 *        the logger
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param restOps
	 *        the REST operations
	 * @param errorEventTags
	 *        the error event tags
	 * @param encryptor
	 *        the sensitive key encryptor
	 * @param sensitiveKeyProvider
	 *        the sensitive key provider
	 * @param oauthClientManager
	 *        the OAuth client manager
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public OAuth2RestOperationsHelper(Logger log, UserEventAppenderBiz userEventAppenderBiz,
			RestOperations restOps, String[] errorEventTags, TextEncryptor encryptor,
			Function<String, Set<String>> sensitiveKeyProvider,
			OAuth2AuthorizedClientManager oauthClientManager) {
		super(log, userEventAppenderBiz, restOps, errorEventTags, encryptor, sensitiveKeyProvider);
		this.oauthClientManager = requireNonNullArgument(oauthClientManager, "oauthClientManager");
	}

	@Override
	public <B, R, C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>, T> T http(
			String description, HttpMethod method, B body, C configuration, Class<R> responseType,
			Function<HttpHeaders, URI> setup, Function<ResponseEntity<R>, T> handler) {
		return super.http(description, method, body, configuration, responseType, (headers) -> {
			if ( configuration instanceof CloudIntegrationConfiguration integration ) {
				final var decrypted = integration.copyWithId(integration.getId());
				decrypted.unmaskSensitiveInformation(sensitiveKeyProvider, encryptor);
				addOAuthBearerAuthorization(decrypted, headers, oauthClientManager,
						userEventAppenderBiz);
			}
			return setup.apply(headers);
		}, handler);
	}

}
