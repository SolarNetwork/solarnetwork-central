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

import static java.lang.String.format;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.eventForConfiguration;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.web.support.ContentLengthTrackingClientHttpRequestInterceptor;
import net.solarnetwork.service.RemoteServiceException;

/**
 * Helper for HTTP interactions using {@link RestOperations}.
 *
 * @author matt
 * @version 1.2
 */
public class RestOperationsHelper implements CloudIntegrationsUserEvents {

	/** The logger. */
	protected final Logger log;

	/** The user event appender service. */
	protected final UserEventAppenderBiz userEventAppenderBiz;

	/** The REST operations. */
	protected final RestOperations restOps;

	/** The error event tags. */
	protected final String[] errorEventTags;

	/** The sensitive key encryptor. */
	protected final TextEncryptor encryptor;

	/** The sensitive key provider. */
	protected final Function<String, Set<String>> sensitiveKeyProvider;

	protected final ThreadLocal<AtomicLong> responseLengthTracker;

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
			RestOperations restOps, String[] errorEventTags, TextEncryptor encryptor,
			Function<String, Set<String>> sensitiveKeyProvider) {
		super();
		this.log = requireNonNullArgument(log, "log");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.restOps = requireNonNullArgument(restOps, "restOps");
		this.errorEventTags = requireNonNullArgument(errorEventTags, "errorEventTags");
		this.encryptor = requireNonNullArgument(encryptor, "encryptor");
		this.sensitiveKeyProvider = requireNonNullArgument(sensitiveKeyProvider, "sensitiveKeyProvider");

		// look for a ContentLengthTrackingClientHttpRequestInterceptor to track response body length with
		ThreadLocal<AtomicLong> tracker = null;
		if ( restOps instanceof RestTemplate rt ) {
			var interceptors = rt.getInterceptors();
			if ( interceptors != null ) {
				for ( var interceptor : interceptors ) {
					if ( interceptor instanceof ContentLengthTrackingClientHttpRequestInterceptor t ) {
						tracker = t.countThreadLocal();
					}
				}
			}
		}
		this.responseLengthTracker = tracker;
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
		if ( responseLengthTracker != null ) {
			responseLengthTracker.get().set(0);
		}
		URI uri = null;
		try {
			final var headers = new HttpHeaders();
			final var req = new HttpEntity<B>(body, headers);
			uri = setup.apply(headers);
			final ResponseEntity<R> res = restOps.exchange(uri, method, req, responseType);
			return handler.apply(res);
		} catch ( ResourceAccessException e ) {
			log.warn("[{}] for {} {} failed at [{}] because of a communication error: {}", description,
					configuration.getClass().getSimpleName(), configuration.getId().ident(), uri,
					e.getMessage());
			userEventAppenderBiz.addEvent(configuration.getUserId(), eventForConfiguration(configuration,
					errorEventTags, format("Communication error: %s", e.getMessage())));
			throw new RemoteServiceException("%s failed because of a communication error: %s"
					.formatted(description, e.getMessage()), e);
		} catch ( RestClientResponseException e ) {
			log.warn("[{}] for {} {} failed at [{}] because the HTTP status {} was returned.",
					description, configuration.getClass().getSimpleName(), configuration.getId().ident(),
					uri, e.getStatusCode());
			userEventAppenderBiz.addEvent(configuration.getUserId(), eventForConfiguration(configuration,
					errorEventTags, format("Invalid HTTP status returned: %s", e.getStatusCode())));
			throw new RemoteServiceException("%s failed because an invalid HTTP status was returned: %s"
					.formatted(description, e.getStatusCode()), e);
		} catch ( UnknownContentTypeException e ) {
			log.warn(
					"[{}] for {} {} failed at [{}] because the response Content-Type [{}] is not supported.",
					configuration.getClass().getSimpleName(), configuration.getId().ident(), uri,
					e.getContentType());
			userEventAppenderBiz.addEvent(configuration.getUserId(),
					eventForConfiguration(configuration, errorEventTags,
							format("Invalid HTTP Content-Type returned: %s", e.getContentType())));
			throw new RemoteServiceException(
					"%s failed because the respones Content-Type is not supported: %s"
							.formatted(description, e.getContentType()),
					e);
		} catch ( OAuth2AuthorizationException e ) {
			log.warn("[{}] for {} {} failed at [{}] because of an OAuth error: {}",
					configuration.getClass().getSimpleName(), configuration.getId().ident(), uri,
					e.getMessage());
			userEventAppenderBiz.addEvent(configuration.getUserId(), eventForConfiguration(configuration,
					errorEventTags, format("OAuth error: %s", e.getMessage())));
			throw new RemoteServiceException("%s failed because of an authorization error: %s"
					.formatted(description, e.getMessage()), e);
		} catch ( RuntimeException e ) {
			log.warn("[{}] for {} {} failed at [{}] because of an unknown error: {}",
					configuration.getClass().getSimpleName(), configuration.getId().ident(), uri,
					e.toString(), e);
			userEventAppenderBiz.addEvent(configuration.getUserId(), eventForConfiguration(configuration,
					errorEventTags, format("Unknown error: %s", e.toString())));
			throw e;
		} finally {
			if ( responseLengthTracker != null ) {
				long len = responseLengthTracker.get().get();
				log.info("[{}] for {} {} tracked {} response body length: {}", description,
						configuration.getClass().getSimpleName(), configuration.getId().ident(), uri,
						len);
			}
		}
	}

	/**
	 * Get the REST operations.
	 *
	 * @return the REST operations, never {@literal null}
	 */
	public final RestOperations getRestOps() {
		return restOps;
	}

}
