/* ==================================================================
 * RateLimitingFilter.java - 19/04/2025 7:34:28 am
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

package net.solarnetwork.central.web.support;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.MurmurHash2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerExceptionResolver;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.web.RateLimitExceededException;

/**
 * Filter for rate-limiting HTTP requests.
 *
 * @author matt
 * @version 1.0
 */
public final class RateLimitingFilter implements Filter {

	/** HTTP response header to hold the "remaining" rate limit count. */
	public static final String X_SN_RATE_LIMIT_REMAINING_HEADER = "X-SN-Rate-Limit-Remaining";

	/** HTTP request header for a proxied client IP address. */
	public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

	private static final Pattern SNWS_V1_KEY_PATTERN = Pattern.compile("^SolarNetworkWS\s+([^:]+):");
	private static final Pattern SNWS_V2_KEY_PATTERN = Pattern
			.compile("^SNWS2\s+.*Credential=([^,]+)(?:,|$)");

	private static final Long GLOBAL_ANONYMOUS_KEY = -1L;

	private final ProxyManager<Long> proxyManager;
	private final Supplier<BucketConfiguration> bucketConfigurationProvider;

	private HandlerExceptionResolver exceptionResolver;

	/**
	 * Constructor.
	 *
	 * @param proxyManager
	 *        the bucket proxy manager
	 * @param bucketConfigurationProvider
	 *        the bucket configuration provider
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public RateLimitingFilter(ProxyManager<Long> proxyManager,
			Supplier<BucketConfiguration> bucketConfigurationProvider) {
		super();
		this.proxyManager = requireNonNullArgument(proxyManager, "proxyManager");
		this.bucketConfigurationProvider = requireNonNullArgument(bucketConfigurationProvider,
				"bucketConfigurationProvider");
	}

	private static String requestKey(HttpServletRequest request) {
		String key = null;
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if ( authHeader != null ) {
			Matcher m = SNWS_V2_KEY_PATTERN.matcher(authHeader);
			if ( m.find() ) {
				key = m.group(1);
			} else {
				m = SNWS_V1_KEY_PATTERN.matcher(authHeader);
				if ( m.find() ) {
					key = m.group(1);
				}
			}
		}
		if ( key == null ) {
			key = request.getHeader(X_FORWARDED_FOR_HEADER);
			if ( key == null ) {
				key = request.getRemoteAddr();
			}
		}
		return key;
	}

	/**
	 * Generate a 64-bit ID for a given string key.
	 *
	 * @param key
	 *        the key
	 * @return the ID
	 */
	public static Long idForString(String key) {
		return (key != null ? MurmurHash2.hash64(key) : GLOBAL_ANONYMOUS_KEY);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final HttpServletRequest req = (HttpServletRequest) request;
		final HttpServletResponse res = (HttpServletResponse) response;

		final String key = requestKey(req);
		final Long id = idForString(key);
		final Bucket bucket = proxyManager.builder().build(id, bucketConfigurationProvider);

		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if ( probe.isConsumed() ) {
			res.addIntHeader(X_SN_RATE_LIMIT_REMAINING_HEADER, (int) probe.getRemainingTokens());
			chain.doFilter(request, response);
		} else {
			final HandlerExceptionResolver resolver = this.exceptionResolver;
			if ( resolver != null ) {
				resolver.resolveException(req, res, null, new RateLimitExceededException(key, id));
			} else {
				res.sendError(HttpStatus.TOO_MANY_REQUESTS.value());
			}
		}
	}

	/**
	 * Set a handler exception resolver.
	 *
	 * @param exceptionResolver
	 *        the resolver to set
	 */
	public void setExceptionResolver(HandlerExceptionResolver exceptionResolver) {
		this.exceptionResolver = exceptionResolver;
	}

}
