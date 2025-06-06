/* ==================================================================
 * SolcastRestOperationsHelper.java - 30/10/2024 6:00:12 am
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.API_KEY_SETTING;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.service.IdentifiableConfiguration;

/**
 * Extension of {@link RestOperationsHelper} with support for Solcast style
 * authentication.
 *
 * @author matt
 * @version 1.1
 */
public class SolcastRestOperationsHelper extends RestOperationsHelper {

	private static final List<MediaType> ACCEPT_JSON = List.of(MediaType.APPLICATION_JSON);

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
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SolcastRestOperationsHelper(Logger log, UserEventAppenderBiz userEventAppenderBiz,
			RestOperations restOps, List<String> errorEventTags, TextEncryptor encryptor,
			Function<String, Set<String>> sensitiveKeyProvider) {
		super(log, userEventAppenderBiz, restOps, errorEventTags, encryptor, sensitiveKeyProvider);
	}

	@Override
	public <R, C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>, T> T httpGet(
			String description, C configuration, Class<R> responseType, Function<HttpHeaders, URI> setup,
			Function<ResponseEntity<R>, T> handler) {
		return super.httpGet(description, configuration, responseType, (headers) -> {
			if ( configuration instanceof IdentifiableConfiguration c
					&& c.hasServiceProperty(API_KEY_SETTING) ) {
				final var decrypted = configuration.copyWithId(configuration.getId());
				decrypted.unmaskSensitiveInformation(sensitiveKeyProvider, encryptor);
				final String apiKey = ((IdentifiableConfiguration) decrypted)
						.serviceProperty(API_KEY_SETTING, String.class);
				if ( apiKey != null ) {
					headers.setBearerAuth(apiKey);
				}
			}
			headers.setAccept(ACCEPT_JSON);
			return setup.apply(headers);
		}, handler);
	}

}
