/* ==================================================================
 * JCacheContentCachingService.java - 1/10/2018 7:23:19 AM
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.cache.Cache;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMap;
import net.solarnetwork.web.security.AuthenticationScheme;

/**
 * Caching service backed by a {@link javax.cache.Cache}.
 * 
 * @author matt
 * @version 1.0
 */
public class JCacheContentCachingService implements ContentCachingService {

	private static final Pattern SNWS_V1_KEY_PATTERN = Pattern
			.compile("^" + AuthenticationScheme.V1.getSchemeName() + "\\s+([^:]+):");
	private static final Pattern SNWS_V2_KEY_PATTERN = Pattern.compile("Credential=([^,]+)(?:,|$)");

	private final Cache<String, CachedContent> cache;
	private Set<MediaType> compressibleMediaTypes = new HashSet<>(
			MediaType.parseMediaTypes("text/*, application/json, application/xml"));
	private int compressMinimumLength = 2048;

	/**
	 * Constructor.
	 * 
	 * @param cache
	 *        the cache to use
	 */
	public JCacheContentCachingService(Cache<String, CachedContent> cache) {
		super();
		this.cache = cache;
	}

	private void addAuthorization(HttpServletRequest request, MessageDigest digest) {
		AuthenticationScheme scheme = null;
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if ( header != null ) {
			for ( AuthenticationScheme aScheme : AuthenticationScheme.values() ) {
				if ( header.startsWith(aScheme.getSchemeName()) ) {
					scheme = aScheme;
					break;
				}
			}
		}
		Matcher m = null;
		if ( scheme != null ) {
			switch (scheme) {
				case V1:
					m = SNWS_V1_KEY_PATTERN.matcher(header);
					break;

				case V2:
					m = SNWS_V2_KEY_PATTERN.matcher(header);
					break;
			}
		}
		if ( m != null && m.find() ) {
			digest.update(m.group(1).getBytes());
			digest.update((byte) '@');
		}
	}

	/**
	 * Apply some normalization rules to the query parameters of a request.
	 * 
	 * <p>
	 * This is done so that different ordering of parameters on the request URL
	 * result in a consistent cache key.
	 * </p>
	 * 
	 * @param request
	 * @return
	 */
	private void addNormalizedQueryParameters(HttpServletRequest request, MessageDigest digest) {
		Set<String> paramKeys = request.getParameterMap().keySet();
		if ( paramKeys.size() < 1 ) {
			return;
		}
		String[] keys = paramKeys.toArray(new String[paramKeys.size()]);
		Arrays.sort(keys);
		boolean first = true;
		for ( String key : keys ) {
			String[] vals = request.getParameterValues(key);
			if ( vals != null ) {
				for ( String val : vals ) {
					if ( first ) {
						digest.update((byte) '?');
						first = false;
					} else {
						digest.update((byte) '&');
					}
					digest.update(key.getBytes());
					digest.update((byte) '=');
					digest.update(val.getBytes());
				}
			}
		}
	}

	/**
	 * Get a cache key for a given request.
	 * 
	 * <p>
	 * This implementation uses the following components to generate the cache
	 * key:
	 * </p>
	 * 
	 * <ol>
	 * <li>SolarNetwork authorization user, from the {@literal Authorization}
	 * HTTP header)</li>
	 * <li>request method (via {@link HttpServletRequest#getMethod()})</li>
	 * <li>request URI (via {@link HttpServletRequest#getRequestURI()})</li>
	 * <li>request query parameters</li>
	 * </ol>
	 */
	@Override
	public String keyForRequest(HttpServletRequest request) {
		MessageDigest digest = org.apache.commons.codec.digest.DigestUtils.getMd5Digest();
		addAuthorization(request, digest);
		digest.update(request.getMethod().getBytes());
		digest.update(request.getRequestURI().getBytes());
		addNormalizedQueryParameters(request, digest);
		return Hex.encodeHexString(digest.digest());
	}

	@Override
	public boolean sendCachedResponse(String key, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		CachedContent content = cache.get(key);
		if ( content == null ) {
			return false;
		}

		response.reset();
		response.setStatus(200);

		MultiValueMap<String, String> headers = content.getHeaders();
		if ( headers != null ) {
			for ( Map.Entry<String, List<String>> me : headers.entrySet() ) {
				for ( String value : me.getValue() ) {
					response.addHeader(me.getKey(), value);
				}
			}
		}

		InputStream in = content.getContent();
		if ( in != null ) {
			String contentEncoding = content.getContentEncoding();
			String accept = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
			if ( accept != null && accept.contains("gzip")
					&& "application/gzip".equals(contentEncoding) ) {
				// send already compressed content
				response.setHeader(HttpHeaders.CONTENT_ENCODING, contentEncoding);
				response.setContentLength((int) content.getContentLength());
				FileCopyUtils.copy(in, response.getOutputStream());
			} else if ( "application/gzip".equals(contentEncoding) ) {
				// send decompressed content
				FileCopyUtils.copy(new GZIPInputStream(in), response.getOutputStream());
			} else {
				// send raw content
				response.setContentLength((int) content.getContentLength());
				FileCopyUtils.copy(in, response.getOutputStream());
			}
		}

		return true;
	}

	@Override
	public void cacheResponse(String key, HttpServletRequest request, int statusCode,
			HttpHeaders headers, InputStream content) throws IOException {
		byte[] data = FileCopyUtils.copyToByteArray(content);
		String contentEncoding = headers.getFirst(HttpHeaders.CONTENT_ENCODING);
		MediaType type = headers.getContentType();
		if ( contentEncoding == null && type != null && data.length >= compressMinimumLength ) {
			for ( MediaType t : compressibleMediaTypes ) {
				if ( t.includes(type) ) {
					try (ByteArrayOutputStream byos = new ByteArrayOutputStream();
							GZIPOutputStream out = new GZIPOutputStream(byos)) {
						FileCopyUtils.copy(data, out);
						data = byos.toByteArray();
						contentEncoding = "application/gzip";
					}
					break;
				}
			}
		}
		cache.put(key, new SimpleCachedContent(HttpHeaders.readOnlyHttpHeaders(headers), data,
				contentEncoding));
	}

	/**
	 * Configure a set of compressible media types.
	 * 
	 * @param compressibleMediaTypes
	 *        compressible media types
	 */
	public void setCompressibleMediaTypes(Set<MediaType> compressibleMediaTypes) {
		if ( compressibleMediaTypes == null ) {
			throw new IllegalArgumentException("compressibleMediaTypes set must not be null");
		}
		this.compressibleMediaTypes = compressibleMediaTypes;
	}

	/**
	 * A minimum size content must be to qualify for storing compressed.
	 * 
	 * @param compressMinimumLength
	 *        the minimum length, in bytes
	 */
	public void setCompressMinimumLength(int compressMinimumLength) {
		this.compressMinimumLength = compressMinimumLength;
	}

}
