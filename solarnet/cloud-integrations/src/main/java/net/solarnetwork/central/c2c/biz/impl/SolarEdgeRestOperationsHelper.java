/* ==================================================================
 * SolarEdgeRestOperationsHelper.java - 7/10/2024 10:49:34 am
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

import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeCloudIntegrationService.ACCOUNT_KEY_HEADER;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeCloudIntegrationService.ACCOUNT_KEY_SETTING;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeCloudIntegrationService.API_KEY_HEADER;
import static net.solarnetwork.central.c2c.biz.impl.SolarEdgeCloudIntegrationService.API_KEY_SETTING;
import java.net.URI;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.http.RestOperationsHelper;

/**
 * Extension of {@link RestOperationsHelper} with support for SolarEdge style
 * authentication.
 *
 * @author matt
 * @version 1.0
 */
public class SolarEdgeRestOperationsHelper extends RestOperationsHelper {

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
	public SolarEdgeRestOperationsHelper(Logger log, UserEventAppenderBiz userEventAppenderBiz,
			RestOperations restOps, String[] errorEventTags, TextEncryptor encryptor,
			Function<String, Set<String>> sensitiveKeyProvider) {
		super(log, userEventAppenderBiz, restOps, errorEventTags, encryptor, sensitiveKeyProvider);
	}

	@Override
	public <R, T> T httpGet(String description, CloudIntegrationConfiguration integration,
			Class<R> responseType, Function<HttpHeaders, URI> setup,
			Function<ResponseEntity<R>, T> handler) {
		return super.httpGet(description, integration, responseType, (headers) -> {
			final var decrypted = integration.clone();
			decrypted.unmaskSensitiveInformation(sensitiveKeyProvider, encryptor);
			final String accountKey = decrypted.serviceProperty(ACCOUNT_KEY_SETTING, String.class);
			if ( accountKey != null ) {
				headers.add(ACCOUNT_KEY_HEADER, accountKey);
			}
			final String apiKey = decrypted.serviceProperty(API_KEY_SETTING, String.class);
			if ( apiKey != null ) {
				headers.add(API_KEY_HEADER, apiKey);
			}
			return setup.apply(headers);
		}, handler);
	}

}
