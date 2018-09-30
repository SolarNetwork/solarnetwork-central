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

import static org.springframework.util.Assert.notNull;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.GenericFilterBean;
import net.solarnetwork.util.OptionalService;

/**
 * Filter for caching HTTP responses, returning cached data when possible.
 * 
 * <p>
 * This filter delegates most behavior to a {@link ContentCachingService}. If a
 * service is not available, caching will be disabled and HTTP request
 * processing will proceed without any further processing by this filter.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ContentCachingFilter extends GenericFilterBean implements Filter {

	private OptionalService<ContentCachingService> contentCachingService;
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

	@Override
	public void afterPropertiesSet() {
		notNull(contentCachingService, "A ContentCachingService is required");
		List<LockAndCount> locks = new ArrayList<>(lockPoolCapacity);
		for ( int i = 0; i < lockPoolCapacity; i++ ) {
			locks.add(new LockAndCount(i, new ReentrantLock()));
		}
		lockPool = new ArrayBlockingQueue<>(lockPoolCapacity, false, locks);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final ContentCachingService service = (contentCachingService != null
				? contentCachingService.service()
				: null);
		if ( service == null ) {
			log.debug("ContentCachingService not available; caching disabled");
			chain.doFilter(request, response);
			return;
		}
		if ( !(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse) ) {
			log.debug("Not HTTP request; caching disabled");
			chain.doFilter(request, response);
			return;
		}

		final HttpServletRequest origRequest = (HttpServletRequest) request;
		final HttpServletResponse origResponse = (HttpServletResponse) response;

		final String method = origRequest.getMethod().toUpperCase();
		if ( !methodsToCache.contains(method) ) {
			log.debug("HTTP method {} not supported; caching disabled for {}", method,
					origRequest.getRequestURI());
			chain.doFilter(request, response);
			return;
		}

		// get cache key for this request
		final String key = service.keyForRequest(origRequest);
		if ( key == null ) {
			log.debug("HTTP request {} not cachable", origRequest.getRequestURI());
			chain.doFilter(request, response);
			return;
		}

		// get a lock for this key
		final LockAndCount lock = requestLocks.computeIfAbsent(key, k -> {
			try {
				LockAndCount l = lockPool.poll(requestLockTimeout, TimeUnit.MILLISECONDS);
				log.trace("Borrowed lock {} from pool for {}", l.getId(), origRequest.getRequestURI());
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

		log.trace("Using lock {} for key {} for {}", lock.getId(), key, origRequest.getRequestURI());

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
			if ( service.sendCachedResponse(key, origRequest, origResponse) ) {
				log.debug("Sent cached response for {}", origRequest.getRequestURI());
				return;
			}

			// cache miss: pass on request and capture result for cache
			final ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(
					origResponse);
			chain.doFilter(origRequest, wrappedResponse);

			// cache the response
			service.cacheResponse(key, origRequest, wrappedResponse.getStatus(),
					wrappedResponse.getHeaders(), wrappedResponse.getContentInputStream());

			// send the response body
			wrappedResponse.copyBodyToResponse();
		} finally {
			lock.unlock();
			int count = lock.decrementCount();
			if ( count < 1 ) {
				if ( requestLocks.remove(key, lock) ) {
					log.trace("Removed lock for key {} for {}", key, origRequest.getRequestURI());
					if ( lockPool.offer(lock) ) {
						log.trace("Lock {} returned to pool for {}", lock.getId(),
								origRequest.getRequestURI());
					}
				}
			}
		}
	}

	/**
	 * Set the caching service to use.
	 * 
	 * @param contentCachingService
	 *        the caching service
	 */
	public void setContentCachingService(OptionalService<ContentCachingService> contentCachingService) {
		this.contentCachingService = contentCachingService;
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
	 *        the lock pool capacity
	 */
	public void setLockPoolCapacity(int lockPoolCapacity) {
		this.lockPoolCapacity = lockPoolCapacity;
	}

}
