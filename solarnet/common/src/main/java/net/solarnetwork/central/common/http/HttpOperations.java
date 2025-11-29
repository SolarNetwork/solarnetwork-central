/* ==================================================================
 * HttpOperations.java - 13/03/2025 3:20:03 pm
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

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import net.solarnetwork.domain.Result;

/**
 * API for basic HTTP operations.
 * 
 * @author matt
 * @version 1.0
 */
public interface HttpOperations {

	/**
	 * Make an HTTP request and return the result.
	 *
	 * @param <I>
	 *        the request body type
	 * @param <O>
	 *        the response body type
	 * @param method
	 *        the request method
	 * @param uri
	 *        the URL to request
	 * @param headers
	 *        optional HTTP headers to include
	 * @param body
	 *        an optional body to include
	 * @param responseType
	 *        the expected response type, or {@code null} for no body
	 * @param context
	 *        an optional contextual object, such as a user ID or other
	 *        discriminator
	 * @return the result, never {@literal null}
	 */
	<I, O> ResponseEntity<O> http(HttpMethod method, URI uri, HttpHeaders headers, I body,
			Class<O> responseType, Object context);

	/**
	 * Make an HTTP GET request for an object and return the result.
	 *
	 * @param uri
	 *        the URL to request
	 * @param parameters
	 *        optional query parameters to include in the request URL
	 * @param headers
	 *        optional HTTP headers to include
	 * @param responseType
	 *        the expected response type, or {@code null}
	 * @param context
	 *        an optional contextual object, such as a user ID or other
	 *        discriminator
	 * @return the result, never {@literal null}
	 */
	default <O> Result<O> httpGet(String uri, Map<String, ?> parameters, Map<String, ?> headers,
			Class<O> responseType, Object context) {
		URI u = uri(uri, parameters);
		HttpHeaders h = headersForMap(headers);
		ResponseEntity<O> res = http(HttpMethod.GET, u, h, null, responseType, context);
		return new Result<>(res.getBody());
	}

	/**
	 * Create a URI instance from a string and optional query parameters.
	 * 
	 * @param uri
	 *        the URI string
	 * @param parameters
	 *        the optional query parameters
	 * @return the URI
	 */
	static URI uri(String uri, Map<String, ?> parameters) {
		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(uri);
		if ( parameters != null ) {
			for ( Entry<String, ?> e : parameters.entrySet() ) {
				Object v = e.getValue();
				uriBuilder.queryParam(e.getKey(), v);
			}
		}
		return uriBuilder.build(false).toUri();
	}

	/**
	 * Convert a map into a {@code HttpHeaders} instance.
	 * 
	 * @param headers
	 *        the header map to convert
	 * @return the headers instance, or {@code null} if {@code headers} is
	 *         {@code null}
	 */
	static HttpHeaders headersForMap(Map<String, ?> headers) {
		HttpHeaders h = null;
		if ( headers != null ) {
			h = new HttpHeaders();
			for ( Entry<String, ?> e : headers.entrySet() ) {
				Object v = e.getValue();
				if ( v instanceof Collection<?> c ) {
					h.addAll(e.getKey(), c.stream().map(Object::toString).toList());
				} else if ( v instanceof Object[] a ) {
					h.addAll(e.getKey(), Arrays.stream(a).map(Object::toString).toList());
				} else {
					h.add(e.getKey(), v.toString());
				}
			}
		}
		return h;
	}

}
