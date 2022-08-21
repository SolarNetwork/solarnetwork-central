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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.oscp.util.SystemTaskContext;

/**
 * Implementation of {@link ExternalSystemClient} using {@link RestOperations}.
 * 
 * @author matt
 * @version 1.0
 */
public class RestOpsExternalSystemClient implements ExternalSystemClient {

	private static final Logger log = LoggerFactory.getLogger(RestOpsExternalSystemClient.class);

	private final RestOperations restOps;
	private final UserEventAppenderBiz userEventAppenderBiz;

	/**
	 * Constructor.
	 * 
	 * @param restOps
	 *        the REST operations
	 * @param userEventAppenderBiz
	 *        the user event appender
	 */
	public RestOpsExternalSystemClient(RestOperations restOps, UserEventAppenderBiz userEventAppenderBiz) {
		super();
		this.restOps = requireNonNullArgument(restOps, "restOps");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
	}

	@Override
	public void systemExchange(SystemTaskContext<?> context, HttpMethod method, String path,
			Object body) {
		URI uri = context.systemUri(path);
		HttpHeaders headers = new HttpHeaders();

		// add auth token header
		String authToken = context.authToken();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, tokenAuthorizationHeader(authToken));

		HttpEntity<Object> req = new HttpEntity<>(body, headers);
		ResponseEntity<Void> result = restOps.exchange(uri, method, req, Void.class);
		if ( result.getStatusCode() == HttpStatus.NO_CONTENT ) {
			log.info("[{}] with {} {} successful at: {}", context.name(), context.role(),
					context.config().getId().ident(), uri);
			if ( userEventAppenderBiz != null && context.successEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.successEventTags(), "Success"));
			}
		} else {
			log.warn(
					"[{}] with{} {} failed at [{}] because the HTTP status {} was returned (expected {}).",
					context.role(), context.config().getId().ident(), uri, result.getStatusCodeValue(),
					HttpStatus.NO_CONTENT.value());
			if ( userEventAppenderBiz != null && context.errorEventTags() != null ) {
				userEventAppenderBiz.addEvent(context.config().getId().getUserId(),
						eventForConfiguration(context.config(), context.errorEventTags(), format(
								"Invalid HTTP status returned: %d", result.getStatusCodeValue())));
			}
		}
	}

}
