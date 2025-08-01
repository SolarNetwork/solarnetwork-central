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
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.CONTENT_PROCESSED_AUDIT_SERVICE;
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.eventForConfiguration;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.slf4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.web.support.ContentLengthTrackingClientHttpRequestInterceptor;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.service.RemoteServiceException;

/**
 * Helper for HTTP interactions using {@link RestOperations}.
 *
 * @author matt
 * @version 1.8
 */
public class RestOperationsHelper implements CloudIntegrationsUserEvents {

	/** A maximum error response body length to include in user events. */
	private static final int USER_EVENT_MAX_ERROR_RESPONSE_BODY_LENGTH = 4096;

	/** The logger. */
	protected final Logger log;

	/** The user event appender service. */
	protected final UserEventAppenderBiz userEventAppenderBiz;

	/** The REST operations. */
	protected final RestOperations restOps;

	/** The error event tags. */
	protected final List<String> errorEventTags;

	/**
	 * The event tags (derived from errorEventTags, minus "error").
	 *
	 * @since 1.5
	 */
	protected final List<String> eventTags;

	/** The sensitive key encryptor. */
	protected final TextEncryptor encryptor;

	/** The sensitive key provider. */
	protected final Function<String, Set<String>> sensitiveKeyProvider;

	/** A thread-local response body length tracker. */
	protected final ThreadLocal<AtomicLong> responseLengthTracker;

	/** An optional user service auditor, for response body counts. */
	protected UserServiceAuditor userServiceAuditor;

	/**
	 * Enable HTTP requests to local host destinations.
	 *
	 * @since 1.5
	 */
	private boolean allowLocalHosts;

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
		super();
		this.log = requireNonNullArgument(log, "log");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.restOps = requireNonNullArgument(restOps, "restOps");
		this.errorEventTags = requireNonNullArgument(errorEventTags, "errorEventTags");
		this.encryptor = requireNonNullArgument(encryptor, "encryptor");
		this.sensitiveKeyProvider = requireNonNullArgument(sensitiveKeyProvider, "sensitiveKeyProvider");

		this.eventTags = this.errorEventTags.stream().filter(t -> !ERROR_TAG.equals(t)).toList();

		// look for a ContentLengthTrackingClientHttpRequestInterceptor to track response body length with
		ThreadLocal<AtomicLong> tracker = null;
		if ( restOps instanceof RestTemplate rt ) {
			var interceptors = rt.getInterceptors();
			for ( var interceptor : interceptors ) {
				if ( interceptor instanceof ContentLengthTrackingClientHttpRequestInterceptor t ) {
					tracker = t.countThreadLocal();
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

		// create event data here so exceptions can reference in error events
		final Map<String, Object> eventData = new LinkedHashMap<>(3);
		eventData.put("method", method.toString());

		URI uri = null;
		try {
			final var headers = new HttpHeaders();
			final var req = new HttpEntity<>(body, headers);
			uri = setup.apply(headers);

			eventData.put("uri", uri);
			if ( body != null ) {
				eventData.put("body", body instanceof String s ? s : JsonUtils.getTreeFromObject(body));
			}

			userEventAppenderBiz.addEvent(configuration.getUserId(),
					eventForConfiguration(configuration.getId(), eventTags, description, eventData));

			final ResponseEntity<R> res = restOps.exchange(uri, method, req, responseType);
			return handler.apply(res);
		} catch ( ResourceAccessException e ) {
			log.warn("[{}] for {} {} failed at [{}] because of a communication error: {}", description,
					configuration.getClass().getSimpleName(), configuration.getId().ident(), uri,
					e.getMessage());
			userEventAppenderBiz.addEvent(configuration.getUserId(),
					eventForConfiguration(configuration.getId(), errorEventTags,
							format("Communication error: %s", e.getMessage()), eventData));
			throw new RemoteServiceException("%s failed because of a communication error: %s"
					.formatted(description, e.getMessage()), e);
		} catch ( RestClientResponseException e ) {
			log.warn("[{}] for {} {} failed at [{}] because the HTTP status {} was returned.",
					description, configuration.getClass().getSimpleName(), configuration.getId().ident(),
					uri, e.getStatusCode());

			// try to capture response body
			if ( e.getResponseBodyAsByteArray() != null ) {
				try {
					String respBody = e.getResponseBodyAsString();
					if ( respBody.length() > USER_EVENT_MAX_ERROR_RESPONSE_BODY_LENGTH ) {
						respBody = respBody.substring(0, USER_EVENT_MAX_ERROR_RESPONSE_BODY_LENGTH);
					}
					eventData.put("responseBody", respBody);
				} catch ( Exception e2 ) {
					// forget it, we don't need the drama
				}
			}

			userEventAppenderBiz.addEvent(configuration.getUserId(),
					eventForConfiguration(configuration.getId(), errorEventTags,
							format("Invalid HTTP status returned: %s", e.getStatusCode()), eventData));
			throw new RemoteServiceException("%s failed because an invalid HTTP status was returned: %s"
					.formatted(description, e.getStatusCode()), e);
		} catch ( UnknownContentTypeException e ) {
			if ( e.getStatusCode().is4xxClientError() ) {
				// we see some APIs return text/html on a 404, but our Accept might only expect something like JSON
				// so treat this more like a RestClientResponseException
				log.warn(
						"[{}] for {} {} failed at [{}] because the HTTP status {} was returned (with unexpected Content-Type [{}]).",
						description, configuration.getClass().getSimpleName(),
						configuration.getId().ident(), uri, e.getStatusCode(), e.getContentType());
				userEventAppenderBiz.addEvent(configuration.getUserId(),
						eventForConfiguration(configuration.getId(), errorEventTags, format(
								"Invalid HTTP status returned (with unexpected Content-Type [{}]): %s",
								e.getContentType(), e.getStatusCode()), eventData));
				throw new RemoteServiceException(
						"%s failed because an invalid HTTP status (with unexpected Content-Type [%s]) was returned: %s"
								.formatted(description, e.getContentType(), e.getStatusCode()),
						HttpClientErrorException.create(e.getMessage(), e.getStatusCode(),
								e.getStatusText(), e.getResponseHeaders(), e.getResponseBody(), null));
			} else {
				log.warn(
						"[{}] for {} {} failed at [{}] because the response Content-Type [{}] is not supported.",
						description, configuration.getClass().getSimpleName(),
						configuration.getId().ident(), uri, e.getContentType());
				userEventAppenderBiz.addEvent(configuration.getUserId(),
						eventForConfiguration(configuration.getId(), errorEventTags,
								format("Invalid HTTP Content-Type returned: %s", e.getContentType()),
								eventData));
				throw new RemoteServiceException(
						"%s failed because the respones Content-Type is not supported: %s"
								.formatted(description, e.getContentType()),
						e);
			}
		} catch ( OAuth2AuthorizationException e ) {
			log.warn("[{}] for {} {} failed at [{}] because of an OAuth error: {}", description,
					configuration.getClass().getSimpleName(), configuration.getId().ident(), uri,
					e.getMessage());
			userEventAppenderBiz.addEvent(configuration.getUserId(),
					eventForConfiguration(configuration.getId(), errorEventTags,
							format("OAuth error: %s", e.getMessage()), eventData));
			throw new RemoteServiceException("%s failed because of an authorization error: %s"
					.formatted(description, e.getMessage()), e);
		} catch ( RemoteServiceException e ) {
			// assume already logged
			throw e;
		} catch ( RuntimeException e ) {
			log.warn("[{}] for {} {} failed at [{}] because of an unknown error: {}", description,
					configuration.getClass().getSimpleName(), configuration.getId().ident(), uri,
					e.toString(), e);
			userEventAppenderBiz.addEvent(configuration.getUserId(), eventForConfiguration(
					configuration.getId(), errorEventTags, format("Unknown error: %s", e), eventData));
			throw e;
		} finally {
			if ( responseLengthTracker != null ) {
				long len = responseLengthTracker.get().get();
				log.debug("[{}] for {} {} tracked {} response body length: {}", description,
						configuration.getClass().getSimpleName(), configuration.getId().ident(), uri,
						len);
				if ( userServiceAuditor != null ) {
					userServiceAuditor.auditUserService(configuration.getUserId(),
							CONTENT_PROCESSED_AUDIT_SERVICE, (int) len);
				}
			}
		}
	}

	/**
	 * Make an HTTP request and return the result.
	 *
	 * @param <I>
	 *        the request body type
	 * @param <O>
	 *        the response body type
	 * @param req
	 *        the request
	 * @param responseType
	 *        the expected response type, or {@code null} for no body
	 * @param context
	 *        an optional user ID
	 * @return the result, never {@literal null}
	 */
	public <I, O> ResponseEntity<O> http(RequestEntity<I> req, Class<O> responseType, Object context) {
		validateRequest(req);
		Long userId = context instanceof Long u ? u : null;
		if ( responseLengthTracker != null && userId != null ) {
			responseLengthTracker.get().set(0);
		}

		final Map<String, Object> eventData = new LinkedHashMap<>(3);
		eventData.put("method", req.getMethod().toString());
		eventData.put("uri", req.getUrl().toString());
		if ( req.getBody() != null ) {
			eventData.put("body",
					req.getBody() instanceof String s ? s : JsonUtils.getTreeFromObject(req.getBody()));
		}

		if ( userId != null ) {
			userEventAppenderBiz.addEvent(userId,
					event(eventTags, "HTTP request", getJSONString(eventData)));
		}
		try {
			return restOps.exchange(req, responseType);
		} catch ( RuntimeException e ) {
			if ( userId != null ) {
				userEventAppenderBiz.addEvent(userId, event(errorEventTags,
						format("HTTP request error: %s", e), getJSONString(eventData)));
			}
			throw e;
		} finally {
			if ( responseLengthTracker != null && userId != null ) {
				long len = responseLengthTracker.get().get();
				log.debug("Tracked [{}] response body length: {}", req.getUrl(), len);
				if ( userServiceAuditor != null ) {
					userServiceAuditor.auditUserService(userId, CONTENT_PROCESSED_AUDIT_SERVICE,
							(int) len);
				}
			}
		}
	}

	private void validateRequest(RequestEntity<?> req) {
		if ( allowLocalHosts ) {
			return;
		}
		String host = req.getUrl().getHost();
		try {
			InetAddress addr = InetAddress.getByName(host);
			if ( addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress() ) {
				throw new IllegalArgumentException("Host [" + host + "] is not allowed");
			}
		} catch ( UnknownHostException e ) {
			throw new IllegalArgumentException("Unknown host [" + host + "]");
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

	/**
	 * Get the user service auditor.
	 *
	 * @return the auditor, or {@literal null}
	 * @since 1.3
	 */
	public final UserServiceAuditor getUserServiceAuditor() {
		return userServiceAuditor;
	}

	/**
	 * Set the user service auditor.
	 *
	 * @param userServiceAuditor
	 *        the auditor to set, or {@literal null}
	 * @since 1.3
	 */
	public final void setUserServiceAuditor(UserServiceAuditor userServiceAuditor) {
		this.userServiceAuditor = userServiceAuditor;
	}

	/**
	 * Get the "allow local hosts" mode.
	 *
	 * @return {@code true} to allow HTTP requests to local hosts; defaults to
	 *         {@code false}
	 * @since 1.5
	 */
	public final boolean isAllowLocalHosts() {
		return allowLocalHosts;
	}

	/**
	 * Set the "allow local hosts" mode.
	 *
	 * @param allowLocalHosts
	 *        {@code true} to allow HTTP requests to local hosts
	 * @since 1.5
	 */
	public final void setAllowLocalHosts(boolean allowLocalHosts) {
		this.allowLocalHosts = allowLocalHosts;
	}

}
