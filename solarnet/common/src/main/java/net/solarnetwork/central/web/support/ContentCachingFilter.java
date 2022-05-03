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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import net.solarnetwork.central.web.support.ContentCachingService.CompressionType;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.ObjectUtils;

/**
 * Filter for caching HTTP responses, returning cached data when possible.
 * 
 * <p>
 * This filter delegates most behavior to a {@link ContentCachingService}.
 * </p>
 * 
 * @author matt
 * @version 2.1
 * @since 1.16
 */
public class ContentCachingFilter implements Filter, ServiceLifecycleObserver {

	private static final long EPOCH = 1514764800000L; // 1 Jan 2018 GMT

	private final AtomicLong requestCounter = new AtomicLong(System.currentTimeMillis() - EPOCH / 1000);

	private final ContentCachingService contentCachingService;
	private Set<String> methodsToCache = Collections.singleton("GET");
	private BlockingQueue<LockAndCount> lockPool;
	private int lockPoolCapacity = 128;
	private long requestLockTimeout = TimeUnit.SECONDS.toMillis(240);

	private final ConcurrentMap<String, LockAndCount> requestLocks = new ConcurrentHashMap<>(128);

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static class LockAndCount implements Lock {

		private final int id;
		private final AtomicInteger count;
		private final Lock lock;

		private LockAndCount(int id, Lock lock) {
			super();
			this.id = id;
			this.lock = lock;
			count = new AtomicInteger(0);
		}

		public int getId() {
			return id;
		}

		public int incrementCount() {
			return count.incrementAndGet();
		}

		public int decrementCount() {
			return count.decrementAndGet();
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
	 * Constructor.
	 * 
	 * @param contentCachingService
	 *        the caching service to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ContentCachingFilter(ContentCachingService contentCachingService) {
		super();
		this.contentCachingService = ObjectUtils.requireNonNullArgument(contentCachingService,
				"contentCachingService");
	}

	@Override
	public void serviceDidStartup() {
		List<LockAndCount> locks = new ArrayList<>(lockPoolCapacity);
		for ( int i = 0; i < lockPoolCapacity; i++ ) {
			locks.add(new LockAndCount(i, new ReentrantLock()));
		}
		lockPool = new ArrayBlockingQueue<>(lockPoolCapacity, false, locks);
	}

	@Override
	public void serviceDidShutdown() {
		// nothing
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if ( !(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse) ) {
			log.debug("Not HTTP request; caching disabled");
			chain.doFilter(request, response);
			return;
		}

		final HttpServletRequest origRequest = (HttpServletRequest) request;
		final String requestUri = origRequest.getRequestURI();
		final Long requestId = requestCounter.incrementAndGet();
		final HttpServletResponse origResponse = (HttpServletResponse) response;

		final String method = origRequest.getMethod().toUpperCase();
		if ( !methodsToCache.contains(method) ) {
			log.debug("{} [{}] HTTP method {} not supported; caching disabled", requestId, requestUri,
					method);
			chain.doFilter(request, response);
			return;
		}

		// get cache key for this request
		final String key = contentCachingService.keyForRequest(origRequest);
		if ( key == null ) {
			log.debug("[{}] HTTP request not cachable", requestUri);
			chain.doFilter(request, response);
			return;
		}

		// get a lock for this key
		final LockAndCount lock = requestLocks.computeIfAbsent(key, k -> {
			try {
				LockAndCount l = lockPool.poll(requestLockTimeout, TimeUnit.MILLISECONDS);
				log.trace("{} [{}] Borrowed lock {} from pool", requestId, requestUri, l.getId());
				return l;
			} catch ( InterruptedException e ) {
				return null;
			}
		});
		if ( lock == null ) {
			// TODO: handle JSON response explicitly
			origResponse.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					"Timeout waiting for cache lock");
			return;
		}

		log.trace("{} [{}] Using lock {} for key {}", requestId, requestUri, lock.getId(), key);

		// increment concurrent count for key
		lock.incrementCount();

		// acquire lock for key
		try {
			if ( !lock.tryLock(requestLockTimeout, TimeUnit.MILLISECONDS) ) {
				origResponse.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
						"Timeout acquiring cache lock");
				return;
			}
		} catch ( InterruptedException e ) {
			// TODO: handle JSON response explicitly
			origResponse.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
					"Interrupted acquiring cache lock");
			return;
		}

		// process request
		try {
			if ( contentCachingService.sendCachedResponse(key, origRequest, origResponse) != null ) {
				log.debug("{} [{}] Sent cached response", requestId, requestUri);
				return;
			}

			// cache miss: pass on request and capture result for cache
			final ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(
					origResponse, true);
			log.debug("{} [{}] Cache miss, passing on for processing", requestId, requestUri);
			chain.doFilter(origRequest, wrappedResponse);

			// cache the response, if OK range
			HttpStatus status = HttpStatus.valueOf(origResponse.getStatus());
			if ( status.is2xxSuccessful() ) {
				log.debug("{} [{}] Caching response", requestId, requestUri);
				HttpHeaders headers = wrappedResponse.getHttpHeaders();
				if ( origResponse.getContentType() != null ) {
					headers.setContentType(MediaType.parseMediaType(origResponse.getContentType()));
				}
				contentCachingService.cacheResponse(key, origRequest, wrappedResponse.getStatus(),
						headers, wrappedResponse.getContentInputStream(), CompressionType.GZIP);
			}

			// send the response body
			if ( log.isDebugEnabled() ) {
				if ( status.is2xxSuccessful() ) {
					log.debug("{} [{}] Response cached and sent", requestId, requestUri);
				} else {
					log.debug("{} [{}] Response sent without caching", requestId, requestUri);
				}
			}
		} finally {
			lock.unlock();
			int count = lock.decrementCount();
			if ( count < 1 ) {
				if ( requestLocks.remove(key, lock) ) {
					log.trace("{} [{}] Removed lock for key {}", requestId, requestUri, key);
					if ( lockPool.offer(lock) ) {
						log.trace("{} [{}] Lock {} returned to pool", requestId, requestUri,
								lock.getId());
					}
				}
			}
		}
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
	 * Set the size of the lock pool, which limits concurrency.
	 * 
	 * @param lockPoolCapacity
	 *        the lock pool capacity; defaults to 128
	 */
	public void setLockPoolCapacity(int lockPoolCapacity) {
		this.lockPoolCapacity = lockPoolCapacity;
	}

}
