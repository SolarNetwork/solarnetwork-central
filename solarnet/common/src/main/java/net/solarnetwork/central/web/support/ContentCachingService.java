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
 * @version 1.1
 * @since 1.16
 */
public interface ContentCachingService {

	/**
	 * A HTTP response header for the content cache status.
	 * 
	 * @since 1.1
	 */
	String CONTENT_CACHE_HEADER = "X-SN-Content-Cache";

	/**
	 * The {@link #CONTENT_CACHE_HEADER} value for a cache hit.
	 * 
	 * @since 1.1
	 */
	String CONTENT_CACHE_HEADER_HIT = "HIT";

	/**
	 * The {@link #CONTENT_CACHE_HEADER} value for a cache miss.
	 * 
	 * @since 1.1
	 */
	String CONTENT_CACHE_HEADER_MISS = "MISS";

	/**
	 * Enumeration of supported compression types.
	 * 
	 * @since 1.1
	 */
	enum CompressionType {

		/** The gzip compression type. */
		GZIP("gzip");

		private final String contentEncoding;

		private CompressionType(String contentEncoding) {
			this.contentEncoding = contentEncoding;
		}

		/**
		 * Get the HTTP {@literal Content-Encoding} value associated with this
		 * compression type.
		 * 
		 * @return the content encoding
		 */
		public String getContentEncoding() {
			return contentEncoding;
		}

	}

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
	 * @throws IOException
	 *         if any IO error occurs
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
	 * @param compression
	 *        if {@code content} has been compressed then the compression type,
	 *        otherwise {@literal null}
	 * @throws IOException
	 *         if any IO error occurs
	 */
	default void cacheResponse(String key, HttpServletRequest request, int statusCode,
			HttpHeaders headers, InputStream content) throws IOException {
		cacheResponse(key, request, statusCode, headers, content, null);
	}

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
	 * @param compression
	 *        if {@code content} has been compressed then the compression type,
	 *        otherwise {@literal null}
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	void cacheResponse(String key, HttpServletRequest request, int statusCode, HttpHeaders headers,
			InputStream content, CompressionType compression) throws IOException;
}
