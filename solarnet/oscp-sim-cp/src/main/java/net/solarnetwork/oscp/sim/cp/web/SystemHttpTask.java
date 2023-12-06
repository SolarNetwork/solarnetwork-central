/* ==================================================================
 * SystemHttpTask.java - 23/08/2022 3:14:14 pm
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

package net.solarnetwork.oscp.sim.cp.web;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.UnknownContentTypeException;
import net.solarnetwork.central.oscp.web.OscpWebUtils;

/**
 * Task to send a HTTP request.
 * 
 * @author matt
 * @version 1.1
 */
public class SystemHttpTask<T> implements Runnable {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected final String name;
	protected final RestOperations restOps;
	protected final HttpMethod method;
	protected final URI uri;
	protected final T body;
	protected final BiConsumer<T, HttpHeaders> headersCustomizer;
	protected final CompletableFuture<?> condition;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        the task name
	 * @param restOps
	 *        the REST operations
	 * @param condition
	 *        an optional future that must be completed before sending the
	 *        request
	 * @param method
	 *        the HTTP method
	 * @param uri
	 *        the URI supplier
	 * @param body
	 *        the body content
	 * @param headersCustomizer
	 *        the optional HTTP headers customizer
	 */
	public SystemHttpTask(String name, RestOperations restOps, CompletableFuture<?> condition,
			HttpMethod method, URI uri, T body, BiConsumer<T, HttpHeaders> headersCustomizer) {
		super();
		this.name = requireNonNullArgument(name, "name");
		this.restOps = requireNonNullArgument(restOps, "restOps");
		this.method = requireNonNullArgument(method, "method");
		this.uri = requireNonNullArgument(uri, "uri");
		this.body = requireNonNullArgument(body, "body");
		this.headersCustomizer = headersCustomizer;
		this.condition = condition;
	}

	@Override
	public final void run() {
		if ( condition != null && !condition.isDone() ) {
			log.info("[{}] waiting for precondition to complete...", name);
			try {
				condition.get(10, TimeUnit.SECONDS);
			} catch ( TimeoutException e ) {
				log.warn("[{}] to [{}] failed from timeout waiting for precondition to complete", name,
						uri);
				return;
			} catch ( ExecutionException | CancellationException e ) {
				log.error("[{}] to [{}] failed with {} while waiting for precondition to complete", name,
						uri, e.getClass().getSimpleName(), e);
				return;
			} catch ( InterruptedException e ) {
				// ignore
			}
		}

		log.info("[{}] initiating request to [{}]", name, uri);

		var headers = new HttpHeaders();
		if ( headersCustomizer != null ) {
			headersCustomizer.accept(body, headers);
		}
		if ( !headers.containsKey(OscpWebUtils.REQUEST_ID_HEADER) ) {
			headers.add(OscpWebUtils.REQUEST_ID_HEADER, UUID.randomUUID().toString());
		}
		try {
			var req = new HttpEntity<T>(body, headers);
			doExchange(req);
			log.info("[{}] to [{}] successful", name, uri);
		} catch ( ResourceAccessException e ) {
			log.warn("[{}] to [{}] because of a communication error: {}", name, uri, e.getMessage());
			throw e;
		} catch ( RestClientResponseException e ) {
			log.warn("[{}] to [{}] failed because the HTTP status {} was returned (expected {})", name,
					uri, e.getStatusCode(), HttpStatus.NO_CONTENT.value());
			throw e;
		} catch ( UnknownContentTypeException e ) {
			log.warn(
					"[{}] to [{}] failed because the response Content-Type [{}] is not supported (expected {})",
					name, uri, e.getContentType(), MediaType.APPLICATION_JSON_VALUE);
			throw e;
		} catch ( RuntimeException e ) {
			log.warn("[{}] to [{}] failed of an unknown error: {}", name, uri, e.toString(), e);
			throw e;
		}
	}

	protected void doExchange(HttpEntity<T> req) {
		restOps.exchange(uri, method, req, Void.class);
	}

}
