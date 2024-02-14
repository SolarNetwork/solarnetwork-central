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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.cache.Cache;
import javax.cache.configuration.FactoryBuilder.SingletonFactory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.web.jakarta.security.AuthenticationScheme;

/**
 * Caching service backed by a {@link javax.cache.Cache}.
 * 
 * @author matt
 * @version 1.2
 */
public class JCacheContentCachingService
		implements ContentCachingService, PingTest, CacheEntryCreatedListener<String, CachedContent>,
		CacheEntryExpiredListener<String, CachedContent> {

	/** The default value for the {@code statLogAccessCount} property. */
	public static final int DEFAULT_STAT_LOG_ACCESS_COUNT = 500;

	private static final Pattern SNWS_V1_KEY_PATTERN = Pattern
			.compile("^" + AuthenticationScheme.V1.getSchemeName() + "\\s+([^:]+):");
	private static final Pattern SNWS_V2_KEY_PATTERN = Pattern.compile("Credential=([^,]+)(?:,|$)");

	private final Cache<String, CachedContent> cache;
	private final ContentCacheStatCounter stats;
	private final String pingTestId;

	private Set<MediaType> compressibleMediaTypes = new HashSet<>(
			MediaType.parseMediaTypes("text/*, application/cbor, application/json, application/xml"));
	private int compressMinimumLength = 2048;

	/**
	 * Constructor.
	 * 
	 * @param cache
	 *        the cache to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JCacheContentCachingService(Cache<String, CachedContent> cache) {
		super();
		this.cache = ObjectUtils.requireNonNullArgument(cache, "cache");
		this.stats = new ContentCacheStatCounter(cache.getName(), DEFAULT_STAT_LOG_ACCESS_COUNT);
		this.pingTestId = String.format("%s-%s", JCacheContentCachingService.class.getName(),
				cache.getName());
		MutableCacheEntryListenerConfiguration<String, CachedContent> listenerConfiguration = new MutableCacheEntryListenerConfiguration<>(
				new SingletonFactory<CacheEntryListener<String, CachedContent>>(this), null, false,
				false);
		cache.registerCacheEntryListener(listenerConfiguration);
	}

	@Override
	public String getPingTestId() {
		return pingTestId;
	}

	@Override
	public String getPingTestName() {
		return "Content Cache";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 500;
	}

	@Override
	public Result performPingTest() throws Exception {
		Map<String, Number> statMap = new LinkedHashMap<>(ContentCacheStats.values().length);
		for ( ContentCacheStats s : ContentCacheStats.values() ) {
			statMap.put(s.toString(), stats.get(s));
		}
		statMap.put("HitRate", (int) (stats.getHitRate() * 100));
		return new PingTestResult(true, "Cache active.", statMap);
	}

	@Override
	public void onExpired(Iterable<CacheEntryEvent<? extends String, ? extends CachedContent>> events)
			throws CacheEntryListenerException {
		for ( CacheEntryEvent<? extends String, ? extends CachedContent> event : events ) {
			long size = event.getValue().getContentLength();
			stats.addAndGet(ContentCacheStats.EntryCount, -1L);
			stats.addAndGet(ContentCacheStats.ByteSize, -size);
		}
	}

	@Override
	public void onCreated(Iterable<CacheEntryEvent<? extends String, ? extends CachedContent>> events)
			throws CacheEntryListenerException {
		for ( CacheEntryEvent<? extends String, ? extends CachedContent> event : events ) {
			long size = event.getValue().getContentLength();
			stats.incrementAndGet(ContentCacheStats.EntryCount);
			stats.addAndGet(ContentCacheStats.ByteSize, size);
		}
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

	private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv");
	private static final byte[] CSV_MEDIA_TYPE_COMPONENT = "+csv".getBytes();
	private static final byte[] JSON_MEDIA_TYPE_COMPONENT = "+json".getBytes();
	private static final byte[] XML_MEDIA_TYPE_COMPONENT = "+xml".getBytes();

	public List<MediaType> getAccept(HttpServletRequest request) {
		Enumeration<String> acceptHeader = request.getHeaders(HttpHeaders.ACCEPT);
		StringBuilder buf = new StringBuilder();
		while ( acceptHeader.hasMoreElements() ) {
			if ( buf.length() > 0 ) {
				buf.append(",");
			}
			buf.append(acceptHeader.nextElement());
		}
		String value = buf.toString();
		return (value != null && value.length() > 0 ? MediaType.parseMediaTypes(value)
				: Collections.emptyList());
	}

	private void addNormalizedAccept(HttpServletRequest request, MessageDigest digest) {
		List<MediaType> types = getAccept(request);
		if ( types == null || types.isEmpty() ) {
			return;
		}
		for ( MediaType type : types ) {
			if ( type.isWildcardType() ) {
				continue;
			}
			if ( MediaType.APPLICATION_JSON.isCompatibleWith(type) ) {
				digest.update(JSON_MEDIA_TYPE_COMPONENT);
				return;
			} else if ( CSV_MEDIA_TYPE.isCompatibleWith(type) ) {
				digest.update(CSV_MEDIA_TYPE_COMPONENT);
				return;
			} else if ( MediaType.APPLICATION_XML.isCompatibleWith(type)
					|| MediaType.TEXT_XML.isCompatibleWith(type) ) {
				digest.update(XML_MEDIA_TYPE_COMPONENT);
				return;
			} else if ( type.getType() != null && type.getSubtype() != null ) {
				digest.update((byte) '+');
				digest.update(type.getType().getBytes());
				digest.update((byte) '/');
				digest.update(type.getSubtype().getBytes());
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
	 * <li>Accept header value</li>
	 * </ol>
	 */
	@Override
	public String keyForRequest(HttpServletRequest request) {
		MessageDigest digest = org.apache.commons.codec.digest.DigestUtils.getMd5Digest();
		addAuthorization(request, digest);
		digest.update(request.getMethod().getBytes());
		digest.update(request.getRequestURI().getBytes());
		addNormalizedQueryParameters(request, digest);
		addNormalizedAccept(request, digest);
		return Hex.encodeHexString(digest.digest());
	}

	@Override
	public CachedContent sendCachedResponse(String key, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		CachedContent content = cache.get(key);
		if ( content == null ) {
			stats.incrementAndGet(ContentCacheStats.Miss);
			return null;
		}

		stats.incrementAndGet(ContentCacheStats.Hit);
		response.setStatus(200);

		MultiValueMap<String, String> headers = content.getHeaders();
		if ( headers != null ) {
			for ( Map.Entry<String, List<String>> me : headers.entrySet() ) {
				for ( String value : me.getValue() ) {
					response.addHeader(me.getKey(), value);
				}
			}
		}

		response.setHeader(CONTENT_CACHE_HEADER, CONTENT_CACHE_HEADER_HIT);

		InputStream in = content.getContent();
		if ( in != null ) {
			String contentEncoding = content.getContentEncoding();
			String accept = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
			if ( accept != null && accept.contains(CompressionType.GZIP.getContentEncoding())
					&& CompressionType.GZIP.getContentEncoding().equals(contentEncoding) ) {
				// send already compressed content
				response.setHeader(HttpHeaders.CONTENT_ENCODING, contentEncoding);
				response.setContentLength(content.getContentLength());
				String vary = response.getHeader(HttpHeaders.VARY);
				if ( vary == null ) {
					response.setHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
				} else {
					Collection<String> varies = response.getHeaders(HttpHeaders.VARY);
					boolean addVary = true;
					for ( String v : varies ) {
						if ( "*".equals(v) || HttpHeaders.ACCEPT_ENCODING.equals(v) ) {
							addVary = false;
							break;
						}
					}
					if ( addVary ) {
						response.addHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
					}
				}
				FileCopyUtils.copy(in, response.getOutputStream());
			} else if ( CompressionType.GZIP.getContentEncoding().equals(contentEncoding) ) {
				// send decompressed content
				FileCopyUtils.copy(new GZIPInputStream(in), response.getOutputStream());
			} else {
				// send raw content
				response.setContentLength(content.getContentLength());
				FileCopyUtils.copy(in, response.getOutputStream());
			}
		}

		return content;
	}

	@Override
	public void cacheResponse(String key, HttpServletRequest request, int statusCode,
			HttpHeaders headers, InputStream content, CompressionType compressionType)
			throws IOException {
		byte[] data = FileCopyUtils.copyToByteArray(content);

		String contentEncoding = headers.getFirst(HttpHeaders.CONTENT_ENCODING);
		if ( compressionType != null ) {
			// content already compressed for us
			contentEncoding = compressionType.getContentEncoding();
		} else {
			MediaType type = headers.getContentType();
			if ( type != null && data.length >= compressMinimumLength ) {
				// compress the content if possible
				for ( MediaType t : compressibleMediaTypes ) {
					if ( t.includes(type) ) {
						try (ByteArrayOutputStream byos = new ByteArrayOutputStream();
								GZIPOutputStream out = new GZIPOutputStream(byos)) {
							FileCopyUtils.copy(data, out);
							data = byos.toByteArray();
							contentEncoding = CompressionType.GZIP.getContentEncoding();
						}
						break;
					}
				}
			}
		}
		Map<String, ?> metadata = getCacheContentMetadata(key, request, statusCode, headers);
		cache.put(key, new SimpleCachedContent(new LinkedMultiValueMap<>(headers), data, contentEncoding,
				metadata));
		stats.incrementAndGet(ContentCacheStats.Stored);
	}

	/**
	 * Get metadata for the cache content.
	 * 
	 * <p>
	 * This method returns {@literal null}, so extending classes can override.
	 * Note that the returned object must implement {@link Serializable}, along
	 * with all values in the map.
	 * </p>
	 * 
	 * @param key
	 *        the cache key
	 * @param request
	 *        the active request
	 * @param statusCode
	 *        the HTTP status code
	 * @param headers
	 *        the HTTP headers
	 * @return the metadata, or {@literal null} if none
	 */
	protected Map<String, ?> getCacheContentMetadata(String key, HttpServletRequest request,
			int statusCode, HttpHeaders headers) {
		return null;
	}

	/**
	 * Configure a set of compressible media types.
	 * 
	 * @param compressibleMediaTypes
	 *        compressible media types
	 */
	public void setCompressibleMediaTypes(Set<MediaType> compressibleMediaTypes) {
		this.compressibleMediaTypes = requireNonNullArgument(compressibleMediaTypes,
				"compressibleMediaTypes");
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

	/**
	 * Set the statistic log update count.
	 * 
	 * <p>
	 * Setting this to something greater than {@literal 0} will cause
	 * {@literal INFO} level statistic log entries to be emitted every
	 * {@code statLogAccessCount} times a cachable request has been processed.
	 * </p>
	 * 
	 * @param statLogAccessCount
	 *        the access count the access count; defaults to
	 *        {@link #DEFAULT_STAT_LOG_ACCESS_COUNT}
	 */
	public void setStatLogAccessCount(int statLogAccessCount) {
		this.stats.setLogFrequency(statLogAccessCount);
	}

}
