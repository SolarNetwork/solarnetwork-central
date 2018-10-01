/* ==================================================================
 * ContentCachingService.java - 29/09/2018 2:25:51 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.web.support;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

/**
 * Service that supports caching HTTP responses using keys derived from HTTP
 * requests.
 * 
 * @author matt
 * @version 1.0
 */
public interface ContentCachingService {

	/**
	 * Get a cache key from a HTTP request.
	 * 
	 * @param request
	 *        the HTTP request to derive a key from
	 * @return the key, or {@literal null} if the request should not be cached
	 */
	String keyForRequest(HttpServletRequest request);

	/**
	 * Send a cached response for a given cache key, if possible.
	 * 
	 * @param key
	 *        the cache key, returned previously from
	 *        {@link #keyForRequest(HttpServletRequest)}
	 * @param request
	 *        the active HTTP request
	 * @param response
	 *        the active HTTP response to send the cached data to
	 * @return the cached content if the response was successfully handled,
	 *         {@literal null} otherwise (for example a cache miss)
	 */
	CachedContent sendCachedResponse(String key, HttpServletRequest request,
			HttpServletResponse response) throws IOException;

	/**
	 * Cache a response after completing an intercepted response.
	 * 
	 * @param key
	 *        the cache key, returned previously from
	 *        {@link #keyForRequest(HttpServletRequest)}
	 * @param request
	 *        the active HTTP request
	 * @param statusCode
	 *        the resolved HTTP response status code
	 * @param headers
	 *        the resolved HTTP response headers
	 * @param content
	 *        the resolved HTTP response content, or {@literal null} if none
	 */
	void cacheResponse(String key, HttpServletRequest request, int statusCode, HttpHeaders headers,
			InputStream content) throws IOException;
}
