/* ==================================================================
 * RestOpsExternalSystemClient.java - 22/08/2022 10:24:08 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.http;

import static java.lang.String.format;
import static net.solarnetwork.central.oscp.domain.OscpUserEvents.eventForConfiguration;
import static net.solarnetwork.central.oscp.web.OscpWebUtils.tokenAuthorizationHeader;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.UnknownContentTypeException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemConfigurationException;
import net.solarnetwork.central.oscp.domain.ExternalSystemServiceProperties;
import net.solarnetwork.central.oscp.util.TaskContext;
import net.solarnetwork.central.oscp.web.OscpWebUtils;

/**
 * Implementation of {@link ExternalSystemClient} using {@link RestOperations}.
 * 
 * @author matt
 * @version 1.1
 */
public class RestOpsExternalSystemClient implements ExternalSystemClient {

	/**
	 * A thread local map of extra HTTP headers for adding to OAuth token
	 * requests.
	 * 
	 * <p>
	 * For each OAuth token request, this will be set to the value of the
	 * associated system's
	 * {@link BaseOscpExternalSystemConfiguration#extraHttpHeaders()} value.</p.
	 */
	private static final ThreadLocal<Map<String, ?>> OAUTH_EXTRA_HTTP_HEADERS = new ThreadLocal<>();

	private static final Logger log = LoggerFactory.getLogger(RestOpsExternalSystemClient.class);

	private static final String[] SUPPORTED_REQUEST_PARAMETERS = new String[] {
			OscpWebUtils.REQUEST_ID_HEADER, OscpWebUtils.CORRELATION_ID_HEADER,
			OscpWebUtils.ERROR_MESSAGE_HEADER, };

	private final RestOperations restOps;
	private final UserEventAppenderBiz userEventAppenderBiz;
	private OAuth2AuthorizedClientManager oauthClientManager;

	/**
	 * A client request interceptor that can inject the extra HTTP headers from
	 * a system's {@link BaseOscpExternalSystemConfiguration#extraHttpHeaders()}
	 * value in OAuth token requests.
	 */
	public static final class ExternalSystemExtraHeadersOAuthInterceptor
			implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			Map<String, ?> extra = OAUTH_EXTRA_HTTP_HEADERS.get();
			if ( extra != null ) {
				HttpHeaders headers = request.getHeaders();
				for ( Entry<String, ?> e : extra.entrySet() ) {
					Object v = e.getValue();
					if ( v != null ) {
						headers.add(e.getKey(), v.toString());
					}
				}
			}
			return execution.execute(request, body);
		}

	}

	/**
	 * Constructor.
	 * 
	 * @param restOps
	 *        the REST operations
	 * @param userEventAppenderBiz
	 *        the user event appender
	 */
	public RestOpsExternalSystemClient(RestOperations restOps,
			UserEventAppenderBiz userEventAppenderBiz) {
		super();
		this.restOps = requireNonNullArgument(restOps, "restOps");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
	}

	@Override
	public void systemExchange(TaskContext<?> context, HttpMethod method, Supplier<String> path,
			Object body) {
		URI uri;
		try {
			uri = context.systemUri(path.get());
			if ( uri == null ) {
				log.debug("[{}] with {} {} will not be sent because the system URI is not configured.",
						context.name(), context.role(), context.config().getId().ident());
				return;
			}
		} catch ( ExternalSystemConfigurationException e ) {
			log.warn("[{}] with {} {} failed because of the system URI could not be resolved: {}",
					context.name(), context.role(), context.config().getId().ident(), e.getMessage());
			if ( userEventAppenderBiz != null && context.errorEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.errorEventTags(),
								format("System URI unresolvable: %s", e.getMessage())));
			}
			throw e;
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		if ( context.parameters() != null ) {
			for ( String header : SUPPORTED_REQUEST_PARAMETERS ) {
				Object v = context.parameters().get(header);
				if ( v != null ) {
					headers.add(header, v.toString());
				}
			}

			// support an "extra" HTTP headers parameter, of a Map<String,String> of arbitrary headers to include
			Object extraHeadersParam = context.parameters()
					.get(ExternalSystemServiceProperties.EXTRA_HTTP_HEADERS);
			if ( extraHeadersParam instanceof Map<?, ?> m ) {
				for ( Entry<?, ?> e : m.entrySet() ) {
					if ( e.getKey() != null && e.getValue() != null ) {
						headers.add(e.getKey().toString(), e.getValue().toString());
					}
				}
			}
		}
		if ( !headers.containsKey(OscpWebUtils.REQUEST_ID_HEADER) ) {
			// assign a unique ID
			headers.add(OscpWebUtils.REQUEST_ID_HEADER, UUID.randomUUID().toString());
		}

		Map<String, ?> extraHttpHeaders = context.config().extraHttpHeaders();

		try {
			// add appropriate authorization header
			if ( context.config().hasOauthClientSettings() && oauthClientManager != null ) {
				AuthRoleInfo role = context.config().getAuthRole();
				OAuth2AuthorizeRequest authReq = OAuth2AuthorizeRequest
						.withClientRegistrationId(role.asIdentifier())
						.principal("%s %s %s".formatted(context.role(), context.config().getId().ident(),
								context.config().getName()))
						.build();

				try {
					OAUTH_EXTRA_HTTP_HEADERS.set(extraHttpHeaders);
					OAuth2AuthorizedClient oauthClient = requireNonNullObject(
							oauthClientManager.authorize(authReq), "oauthClient");
					OAuth2AccessToken accessToken = oauthClient.getAccessToken();
					headers.add("Authorization", "Bearer " + accessToken.getTokenValue());
				} finally {
					OAUTH_EXTRA_HTTP_HEADERS.remove();
				}
			} else {
				String authToken = context.authToken();
				if ( authToken != null ) {
					headers.set(HttpHeaders.AUTHORIZATION, tokenAuthorizationHeader(authToken));
				}
			}

			if ( extraHttpHeaders != null ) {
				for ( Entry<String, ?> e : extraHttpHeaders.entrySet() ) {
					Object v = e.getValue();
					if ( v != null ) {
						headers.add(e.getKey(), v.toString());
					}
				}
			}

			HttpEntity<Object> req = new HttpEntity<>(body, headers);
			restOps.exchange(uri, method, req, Void.class);
			log.info("[{}] with {} {} successful at: {}", context.name(), context.role(),
					context.config().getId().ident(), uri);
			if ( userEventAppenderBiz != null && context.successEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.successEventTags(), "Success"));
			}
		} catch ( ExternalSystemConfigurationException e ) {
			log.warn("[{}] with {} {} failed at [{}] because of a system configuration error: {}",
					context.name(), context.role(), context.config().getId().ident(), uri,
					e.getMessage());
			if ( userEventAppenderBiz != null && context.errorEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.errorEventTags(),
								format("System configuration error: %s", e.getMessage())));
			}
			throw e;
		} catch ( ResourceAccessException e ) {
			log.warn("[{}] with {} {} failed at [{}] because of a communication error: {}",
					context.name(), context.role(), context.config().getId().ident(), uri,
					e.getMessage());
			if ( userEventAppenderBiz != null && context.errorEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.errorEventTags(),
								format("Communication error: %s", e.getMessage())));
			}
			throw e;
		} catch ( RestClientResponseException e ) {
			log.warn(
					"[{}] with {} {} failed at [{}] because the HTTP status {} was returned (expected {}).",
					context.name(), context.role(), context.config().getId().ident(), uri,
					e.getStatusCode(), HttpStatus.NO_CONTENT.value());
			if ( userEventAppenderBiz != null && context.errorEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.errorEventTags(),
								format("Invalid HTTP status returned: %d", e.getStatusCode())));
			}
			throw e;
		} catch ( UnknownContentTypeException e ) {
			log.warn(
					"[{}] with {} {} failed at [{}] because the response Content-Type [{}] is not supported (expected {}).",
					context.name(), context.role(), context.config().getId().ident(), uri,
					e.getContentType(), MediaType.APPLICATION_JSON_VALUE);
			if ( userEventAppenderBiz != null && context.errorEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.errorEventTags(),
								format("Invalid HTTP Content-Type returned: %s", e.getContentType())));
			}
			throw e;
		} catch ( OAuth2AuthorizationException e ) {
			log.warn("[{}] with {} {} failed at [{}] because of an OAuth error: {}", context.name(),
					context.role(), context.config().getId().ident(), uri, e.getMessage());
			if ( userEventAppenderBiz != null && context.errorEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.errorEventTags(),
								format("OAuth error: %s", e.getMessage())));
			}
			throw e;
		} catch ( RuntimeException e ) {
			log.warn("[{}] with {} {} failed at [{}] because of an unknown error: {}", context.name(),
					context.role(), context.config().getId().ident(), uri, e.toString(), e);
			if ( userEventAppenderBiz != null && context.errorEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.errorEventTags(),
								format("Unknown error: %s", e.toString())));
			}
			throw e;
		}
	}

	/**
	 * Set the OAuth client manager.
	 * 
	 * @param oauthClientManager
	 *        the manager
	 */
	public void setOauthClientManager(OAuth2AuthorizedClientManager oauthManager) {
		this.oauthClientManager = oauthManager;
	}

}
