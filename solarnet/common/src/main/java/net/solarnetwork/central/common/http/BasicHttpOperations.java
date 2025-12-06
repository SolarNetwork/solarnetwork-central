/* ==================================================================
 * BasicHttpOperations.java - 19/11/2025 9:52:42 am
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

import static java.lang.String.format;
import static net.solarnetwork.central.domain.LogEventInfo.event;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.cache.Cache;
import org.slf4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.domain.CommonUserEvents;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.Result;
import net.solarnetwork.service.RemoteServiceException;

/**
 * Basic implementation of {@link HttpOperations}.
 * 
 * <p>
 * The {@link #httpGet(String, Map, Map, Class, Object, Map)} has caching
 * support, if {@link #setHttpCache(Cache)} is configured.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class BasicHttpOperations implements HttpOperations, CommonUserEvents, HttpUserEvents {

	/** A maximum error response body length to include in user events. */
	public static final int USER_EVENT_MAX_RESPONSE_BODY_LENGTH = 4096;

	/**
	 * A runtime data key for a runtime-specific {@link UserEventAppenderBiz} to
	 * use.
	 * 
	 * <p>
	 * This can be used to provide a request-specific event appender to use, for
	 * example during a simulation.
	 * </p>
	 */
	public static final String USER_EVENT_APPENDER_RUNTIME = "userEventAppender";

	/** A logger. */
	protected final Logger log;

	/** The user event appender. */
	protected final UserEventAppenderBiz userEventAppenderBiz;

	/** The REST operations. */
	protected final RestOperations restOps;

	private final ThreadLocal<AtomicLong> responseLengthTracker;
	private final List<String> errorEventTags;
	private final List<String> eventTags;

	private boolean allowLocalHosts;
	private Cache<CachableRequestEntity, Result<?>> httpCache;

	/**
	 * An optional user service key to audit, if {@code userServiceAuditor}
	 * configured.
	 */
	private String userServiceKey;

	/**
	 * An optional user service auditor, for response body counts, if
	 * {@code userServiceKey} configured.
	 */
	private UserServiceAuditor userServiceAuditor;

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
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BasicHttpOperations(Logger log, UserEventAppenderBiz userEventAppenderBiz,
			RestOperations restOps, List<String> errorEventTags) {
		super();
		this.log = requireNonNullArgument(log, "log");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.restOps = requireNonNullArgument(restOps, "restOps");
		this.errorEventTags = requireNonNullArgument(errorEventTags, "errorEventTags");

		this.eventTags = this.errorEventTags.stream().filter(t -> !ERROR_TAG.equals(t)).toList();

		// look for a ContentLengthTrackingClientHttpRequestInterceptor to track response body length
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

	@Override
	public <I, O> ResponseEntity<O> http(HttpMethod method, URI uri, HttpHeaders headers, I body,
			Class<O> responseType, Object context, Map<String, ?> runtimeData) {
		final RequestEntity<I> req = RequestEntity.method(method, uri).headers(headers).body(body);
		return exchange(() -> req, responseType, context, runtimeData,
				BasicHttpOperations::defaultRequestEventMessage,
				BasicHttpOperations::defaultRequestErrorEventMessage);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <O> Result<O> httpGet(String uri, Map<String, ?> parameters, Map<String, ?> headers,
			Class<O> responseType, Object context, Map<String, ?> runtimeData) {
		URI u = HttpOperations.uri(uri, parameters);
		HttpHeaders h = HttpOperations.headersForMap(headers);
		CachableRequestEntity req = new CachableRequestEntity(context, h, HttpMethod.GET, u);

		Result<O> result = null;
		if ( httpCache != null ) {
			result = (Result<O>) httpCache.get(req);
		}
		if ( result == null ) {
			ResponseEntity<O> res = exchange(() -> req, responseType, context, runtimeData,
					BasicHttpOperations::defaultRequestEventMessage,
					BasicHttpOperations::defaultRequestErrorEventMessage);
			result = Result.success(res.getBody());
			if ( httpCache != null ) {
				httpCache.put(req, result);
			}
		}

		return result;
	}

	/**
	 * A default event message supplier.
	 * 
	 * @return the default event message
	 */
	public static String defaultRequestEventMessage() {
		return "HTTP request";
	}

	/**
	 * A default error event message function.
	 * 
	 * @param t
	 *        the exception
	 * @return the error message to use
	 */
	public static String defaultRequestErrorEventMessage(Throwable t) {
		return switch (t) {
			case ResourceAccessException e -> format("Communication error: %s", e.getMessage());
			case RestClientResponseException e -> format("Invalid HTTP status returned: %s",
					e.getStatusCode());
			case OAuth2AuthorizationException e -> format("OAuth error: %s", e.getMessage());
			case UnknownContentTypeException e -> {
				if ( e.getStatusCode().is4xxClientError() ) {
					yield format("Invalid HTTP status returned (with unexpected Content-Type [%s]): %s",
							e.getContentType(), e.getStatusCode());
				}
				yield format("Invalid HTTP Content-Type returned: %s", e.getContentType());
			}
			default -> format("HTTP request error: %s", t);
		};
	}

	/**
	 * Make an HTTP request, with standard user event logging and exception
	 * handling.
	 * 
	 * @param <I>
	 *        the input body type
	 * @param <O>
	 *        the output body type
	 * @param reqProvider
	 *        the request provider
	 * @param responseType
	 *        the desired response type
	 * @param context
	 *        a user-related context item, such as the user ID or a user-related
	 * @param runtimeData
	 *        optional runtime data entity or primary key
	 * @param eventMessageProvider
	 *        an event message supplier, for example
	 *        {@link #defaultRequestEventMessage()}
	 * @param errorEventMessageProvider
	 *        an error event message resolver, for example
	 *        {@link #defaultRequestErrorEventMessage(Throwable)}
	 * @return the HTTP response entity
	 */
	protected final <I, O> ResponseEntity<O> exchange(final Supplier<RequestEntity<I>> reqProvider,
			final Class<O> responseType, final Object context, final Map<String, ?> runtimeData,
			final Supplier<String> eventMessageProvider,
			final Function<Throwable, String> errorEventMessageProvider) {
		// cache some log/event details
		final Long userId = contextUserId(context);
		final Object confId = (context instanceof Entity<?> e ? e.getId() : null);
		final String confIdDesc = (confId instanceof CompositeKey pk ? pk.ident()
				: confId != null ? confId.toString() : null);
		final String eventDescription = eventMessageProvider.get();

		final Map<String, Object> eventData = new LinkedHashMap<>(3);
		if ( confId instanceof UserRelatedCompositeKey<?> pk ) {
			CommonUserEvents.populateUserRelatedKeyEventParameters(pk, eventData);
		}

		final UserEventAppenderBiz eventAppender = (runtimeData != null
				&& runtimeData.get(USER_EVENT_APPENDER_RUNTIME) instanceof UserEventAppenderBiz b ? b
						: this.userEventAppenderBiz);

		List<String> tags = eventTags;
		String eventMsg = eventDescription;

		RequestEntity<I> req = null;
		try {
			req = reqProvider.get();

			eventData.put(HTTP_METHOD_DATA_KEY, req.getMethod().toString());
			eventData.put(HTTP_URI_DATA_KEY, req.getUrl().toString());
			if ( req.getBody() != null ) {
				eventData.put(HTTP_BODY_DATA_KEY, req.getBody() instanceof String s ? s
						: JsonUtils.getTreeFromObject(req.getBody()));
			}

			validateRequest(req);

			ResponseEntity<O> result = restOps.exchange(req, responseType);

			eventData.put(HTTP_STATUS_CODE_DATA_KEY, result.getStatusCode().value());
			populateResponseBodyEventData(result, eventData);

			return result;
		} catch ( ResourceAccessException e ) {
			log.warn("[{}] for {} {} failed at [{}] because of a communication error: {}",
					eventDescription, context.getClass().getSimpleName(), confIdDesc,
					req != null ? req.getUrl() : null, e.getMessage());
			tags = errorEventTags;
			eventMsg = errorEventMessageProvider.apply(e);
			throw new RemoteServiceException("%s failed because of a communication error: %s"
					.formatted(eventDescription, e.getMessage()), e);
		} catch ( RestClientResponseException e ) {
			log.warn("[{}] for {} {} failed at [{}] because the HTTP status {} was returned.",
					eventDescription, context.getClass().getSimpleName(), confIdDesc,
					req != null ? req.getUrl() : null, e.getStatusCode());

			// try to capture response body
			if ( e.getResponseBodyAsByteArray() != null ) {
				try {
					String respBody = e.getResponseBodyAsString();
					if ( respBody.length() > USER_EVENT_MAX_RESPONSE_BODY_LENGTH ) {
						respBody = respBody.substring(0, USER_EVENT_MAX_RESPONSE_BODY_LENGTH);
					}
					eventData.put(HTTP_RESPONSE_BODY_DATA_KEY, respBody);
				} catch ( Exception e2 ) {
					// forget it, we don't need the drama
				}
			}
			eventData.put(HTTP_STATUS_CODE_DATA_KEY, e.getStatusCode().value());
			tags = errorEventTags;
			eventMsg = errorEventMessageProvider.apply(e);
			throw new RemoteServiceException("%s failed because an invalid HTTP status was returned: %s"
					.formatted(eventDescription, e.getStatusCode()), e);
		} catch ( UnknownContentTypeException e ) {
			eventData.put(HTTP_STATUS_CODE_DATA_KEY, e.getStatusCode().value());
			tags = errorEventTags;
			eventMsg = errorEventMessageProvider.apply(e);
			if ( e.getStatusCode().is4xxClientError() ) {
				// we see some APIs return text/html on a 404, but our Accept might only expect something like JSON
				// so treat this more like a RestClientResponseException
				log.warn(
						"[{}] for {} {} failed at [{}] because the HTTP status {} was returned (with unexpected Content-Type [{}]).",
						eventDescription, context.getClass().getSimpleName(), confIdDesc,
						req != null ? req.getUrl() : null, e.getStatusCode(), e.getContentType());
				throw new RemoteServiceException(
						"%s failed because an invalid HTTP status (with unexpected Content-Type [%s]) was returned: %s"
								.formatted(eventDescription, e.getContentType(), e.getStatusCode()),
						HttpClientErrorException.create(e.getMessage(), e.getStatusCode(),
								e.getStatusText(), e.getResponseHeaders(), e.getResponseBody(), null));
			} else {
				log.warn(
						"[{}] for {} {} failed at [{}] because the response Content-Type [{}] is not supported.",
						eventDescription, context.getClass().getSimpleName(), confIdDesc,
						req != null ? req.getUrl() : null, e.getContentType());
				throw new RemoteServiceException(
						"%s failed because the respones Content-Type is not supported: %s"
								.formatted(context, e.getContentType()),
						e);
			}
		} catch ( OAuth2AuthorizationException e ) {
			log.warn("[{}] for {} {} failed at [{}] because of an OAuth error: {}", eventDescription,
					context.getClass().getSimpleName(), confIdDesc, req != null ? req.getUrl() : null,
					e.getMessage());
			tags = errorEventTags;
			eventMsg = errorEventMessageProvider.apply(e);
			throw new RemoteServiceException("%s failed because of an authorization error: %s"
					.formatted(eventDescription, e.getMessage()), e);
		} catch ( RemoteServiceException e ) {
			tags = errorEventTags;
			eventMsg = errorEventMessageProvider.apply(e);
			throw e;
		} catch ( RuntimeException e ) {
			tags = errorEventTags;
			eventMsg = errorEventMessageProvider.apply(e);
			throw e;
		} finally {
			if ( responseLengthTracker != null && userId != null ) {
				long len = responseLengthTracker.get().get();
				log.debug("Tracked [{}] response body length for service [{}]: {}",
						req != null ? req.getUrl() : null, userServiceKey, len);
				if ( userServiceKey != null && userServiceAuditor != null ) {
					userServiceAuditor.auditUserService(userId, userServiceKey, (int) len);
				}
				eventData.put(HTTP_RESPONSE_BODY_LENGTH_DATA_KEY, len);
			}
			if ( userId != null ) {
				eventAppender.addEvent(userId, event(tags, eventMsg, getJSONString(eventData)));
			}
		}
	}

	private void populateResponseBodyEventData(ResponseEntity<?> result, Map<String, Object> eventData) {
		String respBody = result.hasBody() ? result.getBody().toString() : null;
		if ( respBody == null ) {
			return;
		}
		if ( respBody.length() <= USER_EVENT_MAX_RESPONSE_BODY_LENGTH ) {
			eventData.put(HTTP_RESPONSE_BODY_DATA_KEY, result.getBody());
		}
	}

	private Long contextUserId(Object context) {
		final Long userId = switch (context) {
			case null -> null;
			case Long id -> id;
			case UserIdRelated u -> u.getUserId();
			default -> null;
		};
		if ( responseLengthTracker != null && userId != null ) {
			responseLengthTracker.get().set(0);
		}
		return userId;
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
	 * Get the error event tags.
	 * 
	 * @return the tags
	 */
	public List<String> getErrorEventTags() {
		return errorEventTags;
	}

	/**
	 * Get the event tags.
	 * 
	 * @return the tags
	 */
	public List<String> getEventTags() {
		return eventTags;
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
	 * Get the user service key, for auditing.
	 * 
	 * @return the key
	 */
	public String getUserServiceKey() {
		return userServiceKey;
	}

	/**
	 * Set the user service key, for auditing.
	 * 
	 * @param userServiceKey
	 *        the key to set; requires
	 *        {@link #setUserServiceAuditor(UserServiceAuditor)} be configured
	 */
	public void setUserServiceKey(String userServiceKey) {
		this.userServiceKey = userServiceKey != null && !userServiceKey.isBlank() ? userServiceKey
				: null;
	}

	/**
	 * Get the user service auditor.
	 *
	 * @return the auditor, or {@literal null}
	 */
	public final UserServiceAuditor getUserServiceAuditor() {
		return userServiceAuditor;
	}

	/**
	 * Set the user service auditor.
	 *
	 * @param userServiceAuditor
	 *        the auditor to set, or {@literal null}; requires
	 *        {@code #setUserServiceKey(String)} be configured
	 */
	public final void setUserServiceAuditor(UserServiceAuditor userServiceAuditor) {
		this.userServiceAuditor = userServiceAuditor;
	}

	/**
	 * Get the HTTP cache.
	 *
	 * @return the cache
	 */
	public final Cache<CachableRequestEntity, Result<?>> getHttpCache() {
		return httpCache;
	}

	/**
	 * Set the HTTP cache.
	 *
	 * @param httpCache
	 *        the cache to set
	 */
	public final void setHttpCache(Cache<CachableRequestEntity, Result<?>> httpCache) {
		this.httpCache = httpCache;
	}

	/**
	 * Get the "allow local hosts" mode.
	 *
	 * @return {@code true} to allow HTTP requests to local hosts; defaults to
	 *         {@code false}
	 */
	public final boolean isAllowLocalHosts() {
		return allowLocalHosts;
	}

	/**
	 * Set the "allow local hosts" mode.
	 *
	 * @param allowLocalHosts
	 *        {@code true} to allow HTTP requests to local hosts
	 */
	public final void setAllowLocalHosts(boolean allowLocalHosts) {
		this.allowLocalHosts = allowLocalHosts;
	}

}
