/* ==================================================================
 * ContentCachingFilter.java - 29/09/2018 2:07:21 PM
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

import static net.solarnetwork.central.web.support.ContentCachingService.CONTENT_CACHE_HEADER;
import static net.solarnetwork.central.web.support.ContentCachingService.CONTENT_CACHE_HEADER_MISS;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.web.support.ContentCachingService.CompressionType;
import net.solarnetwork.service.PingTest;
import net.solarnetwork.service.PingTestResult;
import net.solarnetwork.util.StatTracker;

/**
 * Filter for caching HTTP responses, returning cached data when possible.
 *
 * <p>
 * This filter delegates most behavior to a {@link ContentCachingService}.
 * </p>
 *
 * @author matt
 * @version 3.4
 * @since 1.16
 */
public class ContentCachingFilter implements Filter, PingTest {

	/** The default value for the {@code statLogAccessCount} property. */
	public static final int DEFAULT_STAT_LOG_ACCESS_COUNT = 500;

	private static final long EPOCH = 1514764800000L; // 1 Jan 2018 GMT

	private final AtomicLong requestCounter = new AtomicLong(System.currentTimeMillis() - EPOCH / 1000);

	private final ContentCachingService contentCachingService;
	private final BlockingQueue<LockAndCount> lockPool;
	private final ConcurrentMap<String, LockAndCount> requestLocks;
	private final StatTracker stats;
	private final int lockPoolCapacity;
	private final LongAccumulator lockPoolMinSize;

	private Set<String> methodsToCache = Collections.singleton("GET");
	private long requestLockTimeout = TimeUnit.SECONDS.toMillis(240);

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Statistics for the content caching filter.
	 */
	public enum ContentCachingFilterStats {

		/** Requests filtered. */
		RequestsFiltered,

		/** Lock pool borrows. */
		LockPoolBorrows,

		/** Lock pool returns. */
		LockPoolReturns,

		/** Lock pool borrow failures. */
		LockPoolBorrowFailures,

		/** Request lock failures. */
		RequestLockFailures,

		;

	}

	/**
	 * A lock with a corresponding counter.
	 */
	public static class LockAndCount implements Lock {

		private final int id;
		private final AtomicInteger count;
		private final ReentrantLock lock;

		private LockAndCount(int id, ReentrantLock lock) {
			super();
			this.id = id;
			this.lock = lock;
			count = new AtomicInteger(0);
		}

		/**
		 * Get the identifier.
		 *
		 * @return the ID
		 */
		public int getId() {
			return id;
		}

		/**
		 * Get the count value.
		 *
		 * @return the count
		 */
		public int count() {
			return count.get();
		}

		private int incrementCount() {
			return count.incrementAndGet();
		}

		private int decrementCount() {
			return count.decrementAndGet();
		}

		/**
		 * Test if the lock is locked (by any thread).
		 *
		 * @return {@literal true} if the lock is locked by any thread
		 */
		public boolean isLocked() {
			return lock.isLocked();
		}

		@Override
		public void lock() {
			lock.lock();
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			lock.lockInterruptibly();
		}

		@Override
		public boolean tryLock() {
			return lock.tryLock();
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			return lock.tryLock(time, unit);
		}

		@Override
		public void unlock() {
			lock.unlock();
		}

		@Override
		public Condition newCondition() {
			return lock.newCondition();
		}

	}

	/**
	 * Create a look pool.
	 *
	 * @param lockPoolCapacity
	 *        the desired capacity of the pool
	 * @return the pool
	 */
	public static BlockingQueue<LockAndCount> lockPoolWithCapacity(int lockPoolCapacity) {
		List<LockAndCount> locks = new ArrayList<>(lockPoolCapacity);
		for ( int i = 0; i < lockPoolCapacity; i++ ) {
			locks.add(new LockAndCount(i, new ReentrantLock()));
		}
		return new ArrayBlockingQueue<>(lockPoolCapacity, false, locks);
	}

	/**
	 * Constructor.
	 *
	 * @param contentCachingService
	 *        the caching service to use
	 * @param lockPoolCapacity
	 *        the lock pool capacity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ContentCachingFilter(ContentCachingService contentCachingService, int lockPoolCapacity) {
		this(contentCachingService, lockPoolWithCapacity(lockPoolCapacity),
				new ConcurrentHashMap<>(128));
	}

	/**
	 * Constructor.
	 *
	 * @param contentCachingService
	 *        the caching service to use
	 * @param lockPool
	 *        the request lock pool to use; this must be pre-populated with the
	 *        desired number of locks to use
	 * @param requestLocks
	 *        the request lock map to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null} or the lock pool is empty
	 */
	public ContentCachingFilter(ContentCachingService contentCachingService,
			BlockingQueue<LockAndCount> lockPool, ConcurrentMap<String, LockAndCount> requestLocks) {
		super();
		this.contentCachingService = requireNonNullArgument(contentCachingService,
				"contentCachingService");
		this.lockPool = requireNonNullArgument(lockPool, "lockPool");
		this.requestLocks = requireNonNullArgument(requestLocks, "requestLocks");
		if ( lockPool.isEmpty() ) {
			throw new IllegalArgumentException("The lock pool must not be empty.");
		}
		this.stats = new StatTracker("ContentCacheFilter", null, log, DEFAULT_STAT_LOG_ACCESS_COUNT);
		this.lockPoolCapacity = lockPool.size();
		this.lockPoolMinSize = new LongAccumulator(Math::min, Long.MAX_VALUE);
		this.lockPoolMinSize.accumulate(this.lockPoolCapacity);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if ( !(request instanceof HttpServletRequest origRequest)
				|| !(response instanceof HttpServletResponse origResponse) ) {
			log.debug("Not HTTP request; caching disabled");
			chain.doFilter(request, response);
			return;
		}

		final String requestUri = origRequest.getRequestURI();
		final Long requestId = requestCounter.incrementAndGet();

		final String method = origRequest.getMethod().toUpperCase(Locale.ENGLISH);
		if ( !methodsToCache.contains(method) ) {
			log.debug("{} [{}] HTTP method {} not supported; caching disabled", requestId, requestUri,
					method);
			chain.doFilter(request, response);
			return;
		}

		stats.increment(ContentCachingFilterStats.RequestsFiltered);

		// get cache key for this request
		final String key = contentCachingService.keyForRequest(origRequest);
		if ( key == null ) {
			log.debug("[{}] HTTP request not cachable", requestUri);
			chain.doFilter(request, response);
			return;
		}

		// get a lock for this key
		final LockAndCount lock = requestLocks.computeIfAbsent(key, _ -> {
			try {
				LockAndCount l = lockPool.poll(requestLockTimeout, TimeUnit.MILLISECONDS);
				if ( l == null ) {
					stats.increment(ContentCachingFilterStats.LockPoolBorrowFailures);
				} else {
					stats.increment(ContentCachingFilterStats.LockPoolBorrows, true);
					lockPoolMinSize.accumulate(lockPool.size());
					if ( log.isTraceEnabled() ) {
						log.trace("{} {} [{}] Borrowed lock {} from pool", requestId, key, requestUri,
								l.id);
					}
				}
				return l;
			} catch ( InterruptedException e ) {
				return null;
			}
		});
		if ( lock == null ) {
			// TODO: handle JSON response explicitly
			log.trace("{} {} [{}] Timeout obtaining lock", requestId, key, requestUri);
			origResponse.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
					"Timeout waiting for cache lock");
			return;
		}

		log.trace("{} {} [{}] Using lock {}", requestId, key, requestUri, lock.id);

		// increment concurrent count for key
		lock.incrementCount();

		// acquire lock for key
		try {
			if ( !lock.tryLock(requestLockTimeout, TimeUnit.MILLISECONDS) ) {
				try {
					log.trace("{} {} [{}] Timeout acquiring lock {}", requestId, key, requestUri,
							lock.id);
					origResponse.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
							"Timeout acquiring cache lock");
				} finally {
					stats.increment(ContentCachingFilterStats.RequestLockFailures);
					returnLock(key, lock, requestId, requestUri);
				}
				return;
			}
		} catch ( InterruptedException e ) {
			// TODO: handle JSON response explicitly
			try {
				origResponse.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
						"Interrupted acquiring cache lock");
			} finally {
				stats.increment(ContentCachingFilterStats.RequestLockFailures);
				returnLock(key, lock, requestId, requestUri);
			}
			return;
		}

		// process request
		try {
			if ( contentCachingService.sendCachedResponse(key, origRequest, origResponse) != null ) {
				log.debug("{} {} [{}] Sent cached response", requestId, key, requestUri);
				return;
			}

			// cache miss: pass on request and capture result for cache
			origResponse.setHeader(CONTENT_CACHE_HEADER, CONTENT_CACHE_HEADER_MISS);
			final ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(
					origResponse, true);
			log.debug("{} {} [{}] Cache miss, passing on for processing", requestId, key, requestUri);
			chain.doFilter(origRequest, wrappedResponse);

			// cache the response, if OK range
			HttpStatus status = HttpStatus.valueOf(origResponse.getStatus());
			if ( status.is2xxSuccessful() ) {
				log.debug("{} {} [{}] Caching response", requestId, key, requestUri);
				HttpHeaders headers = wrappedResponse.getHttpHeaders();
				if ( origResponse.getContentType() != null ) {
					headers.setContentType(MediaType.parseMediaType(origResponse.getContentType()));
				}
				try {
					contentCachingService.cacheResponse(key, origRequest, wrappedResponse.getStatus(),
							headers, wrappedResponse.getContentInputStream(), CompressionType.GZIP);
				} catch ( IOException e ) {
					log.warn("{} {} [{}] {} during response processing, not caching: {}", requestId, key,
							requestUri, e.getClass().getName(), e.getMessage());
					return;
				}

			}

			// send the response body
			if ( log.isDebugEnabled() ) {
				if ( status.is2xxSuccessful() ) {
					log.debug("{} {} [{}] Response cached and sent", requestId, key, requestUri);
				} else {
					log.debug("{} {} [{}] Response sent without caching", requestId, key, requestUri);
				}
			}
		} finally {
			lock.unlock();
			returnLock(key, lock, requestId, requestUri);
		}
	}

	private void returnLock(String key, LockAndCount lock, Long requestId, String requestUri) {
		final int count = lock.decrementCount();
		if ( count < 1 ) {
			if ( requestLocks.remove(key, lock) ) {
				log.trace("{} {} [{}] Removed request lock {}", requestId, key, requestUri, lock.id);
				returnLockToPool(key, lock, requestId, requestUri);
			}
		}
	}

	private void returnLockToPool(String key, LockAndCount lock, Long requestId, String requestUri) {
		if ( lockPool.offer(lock) ) {
			stats.increment(ContentCachingFilterStats.LockPoolReturns, true);
			log.trace("{} {} [{}] Lock {} returned to pool", requestId, key, requestUri, lock.id);
		}
	}

	@Override
	public String getPingTestId() {
		return "net.solarnetwork.central.web.support.ContentCachingFilter";
	}

	@Override
	public String getPingTestName() {
		return "Content Caching Filter";
	}

	@Override
	public long getPingTestMaximumExecutionMilliseconds() {
		return 1000;
	}

	@Override
	public Result performPingTest() throws Exception {
		Map<String, Number> statMap = new LinkedHashMap<>(ContentCachingFilterStats.values().length + 3);
		statMap.putAll(stats.allCounts());
		long activeRequests = requestLocks.values().stream().mapToLong(LockAndCount::count).sum();
		statMap.put("LockPoolCapacity", lockPoolCapacity);
		statMap.put("LockPoolAvailable", lockPool.size());
		statMap.put("LockPoolWatermark", lockPoolMinSize.get());
		statMap.put("ActiveRequests", activeRequests);

		return new PingTestResult(true, null, statMap);
	}

	/**
	 * Configure the HTTP methods that can be cached.
	 *
	 * <p>
	 * The method names should be all upper case.
	 * </p>
	 *
	 * @param methodsToCache
	 *        the methods to cache; defaults to {@literal GET} only
	 */
	public void setMethodsToCache(Set<String> methodsToCache) {
		this.methodsToCache = methodsToCache;
	}

	/**
	 * A timeout for waiting for a request lock.
	 *
	 * @param requestLockTimeout
	 *        the timeout to use, in milliseconds; defaults to 4 minutes
	 */
	public void setRequestLockTimeout(long requestLockTimeout) {
		this.requestLockTimeout = requestLockTimeout;
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
	 * @since 3.0
	 */
	public void setStatLogAccessCount(int statLogAccessCount) {
		this.stats.setLogFrequency(statLogAccessCount);
	}
}
