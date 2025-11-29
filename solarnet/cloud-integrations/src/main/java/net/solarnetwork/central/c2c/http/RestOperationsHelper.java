/* ==================================================================
 * RestOperationsHelper.java - 7/10/2024 8:18:33 am
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

import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.CONTENT_PROCESSED_AUDIT_SERVICE;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.common.http.BasicHttpOperations;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;

/**
 * Helper for HTTP interactions using {@link RestOperations}.
 *
 * @author matt
 * @version 1.9
 */
public class RestOperationsHelper extends BasicHttpOperations {

	/** The sensitive key encryptor. */
	protected final TextEncryptor encryptor;

	/** The sensitive key provider. */
	protected final Function<String, Set<String>> sensitiveKeyProvider;

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
	public RestOperationsHelper(Logger log, UserEventAppenderBiz userEventAppenderBiz,
			RestOperations restOps, List<String> errorEventTags, TextEncryptor encryptor,
			Function<String, Set<String>> sensitiveKeyProvider) {
		super(log, userEventAppenderBiz, restOps, errorEventTags);
		this.encryptor = requireNonNullArgument(encryptor, "encryptor");
		this.sensitiveKeyProvider = requireNonNullArgument(sensitiveKeyProvider, "sensitiveKeyProvider");
		setUserServiceKey(CONTENT_PROCESSED_AUDIT_SERVICE);
	}

	/**
	 * Make an HTTP GET request.
	 *
	 * @param <R>
	 *        the HTTP response type
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 *        the configuration primary key type
	 * @param <T>
	 *        the result type
	 * @param description
	 *        a description of the operation, for example "List sites"
	 * @param configuration
	 *        the integration making the request on behalf of
	 * @param responseType
	 *        the HTTP response type
	 * @param setup
	 *        function to customize the HTTP request headers, for example to
	 *        populate authorization values
	 * @param handler
	 *        function to parse the HTTP response
	 * @return the parsed response object
	 * @throws IllegalArgumentException
	 *         if {@code integration} is {@literal null}
	 */
	public <R, C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>, T> T httpGet(
			String description, C configuration, Class<R> responseType, Function<HttpHeaders, URI> setup,
			Function<ResponseEntity<R>, T> handler) {
		return http(description, HttpMethod.GET, null, configuration, responseType, setup, handler);
	}

	/**
	 * Make an HTTP request.
	 *
	 * @param <B>
	 *        the HTTP request body type
	 * @param <R>
	 *        the HTTP response type
	 * @param <C>
	 *        the configuration type
	 * @param <K>
	 *        the configuration primary key type
	 * @param <T>
	 *        the result type
	 * @param description
	 *        a description of the operation, for example "List sites"
	 * @param method
	 *        the HTTP method
	 * @param configuration
	 *        the integration making the request on behalf of
	 * @param responseType
	 *        the HTTP response type
	 * @param setup
	 *        function to customize the HTTP request headers, for example to
	 *        populate authorization values
	 * @param handler
	 *        function to parse the HTTP response
	 * @return the parsed response object
	 * @throws IllegalArgumentException
	 *         if {@code integration} is {@literal null}
	 * @since 1.2
	 */
	public <B, R, C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>, T> T http(
			String description, HttpMethod method, B body, C configuration, Class<R> responseType,
			Function<HttpHeaders, URI> setup, Function<ResponseEntity<R>, T> handler) {
		requireNonNullArgument(configuration, "configuration");

		// execute request
		final ResponseEntity<R> res = exchange(() -> {
			// resolve URI and headers
			final var headers = new HttpHeaders();
			URI uri = setup.apply(headers);
			return RequestEntity.method(method, uri).headers(headers).body(body);
		}, responseType, configuration, () -> description,
				BasicHttpOperations::defaultRequestErrorEventMessage);
		return handler.apply(res);
	}

}
