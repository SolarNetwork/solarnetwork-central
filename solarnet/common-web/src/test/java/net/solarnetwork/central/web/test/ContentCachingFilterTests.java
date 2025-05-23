/* ==================================================================
 * ContentCachingFilterTests.java - 30/09/2018 9:45:46 AM
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

package net.solarnetwork.central.web.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.synchronizedList;
import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.FileCopyUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.web.support.CachedContent;
import net.solarnetwork.central.web.support.ContentCachingFilter;
import net.solarnetwork.central.web.support.ContentCachingFilter.LockAndCount;
import net.solarnetwork.central.web.support.ContentCachingService;
import net.solarnetwork.central.web.support.ContentCachingService.CompressionType;
import net.solarnetwork.central.web.support.SimpleCachedContent;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link ContentCachingFilter} class.
 *
 * @author matt
 * @version 2.0
 */
public class ContentCachingFilterTests {

	private static final Logger log = LoggerFactory.getLogger(ContentCachingFilterTests.class);

	private static final int TEST_LOCK_POOL_CAPACITY = 8;

	private FilterChain chain;
	private ContentCachingService service;
	private BlockingQueue<LockAndCount> lockPool;
	private ConcurrentMap<String, LockAndCount> requestLocks;
	private ContentCachingFilter filter;

	private MockHttpServletResponse response;

	@BeforeEach
	public void setup() {
		chain = EasyMock.createMock(FilterChain.class);
		service = EasyMock.createMock(ContentCachingService.class);
		lockPool = ContentCachingFilter.lockPoolWithCapacity(TEST_LOCK_POOL_CAPACITY);
		requestLocks = new ConcurrentHashMap<>(32);

		filter = new ContentCachingFilter(service, lockPool, requestLocks);
		filter.setRequestLockTimeout(60000);

		response = new MockHttpServletResponse();
	}

	private void replayAll() {
		EasyMock.replay(chain, service);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(chain, service);
	}

	@Test
	public void unsupportedMethod() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/somewhere");

		chain.doFilter(same(request), same(response));

		// when
		replayAll();

		filter.doFilter(request, response, chain);

		// then
	}

	@Test
	public void noCacheKeyProvided() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somewhere");

		expect(service.keyForRequest(request)).andReturn(null);

		chain.doFilter(same(request), same(response));

		// when
		replayAll();

		filter.doFilter(request, response, chain);

		// then
	}

	private void assertLockPoolSize(int count) {
		then(lockPool).as("Look pool size").hasSize(count);
	}

	private void assertRequestLock(String key, boolean locked) {
		// @formatter:off
		then(requestLocks)
				.as("Request lock present")
				.containsKey(key)
				.extractingByKey(key)
				//.as("Has count").returns(count, from(LockAndCount::count))
				.as("Lock %s locked", locked ? "is" : "is not")
				.returns(locked, from(LockAndCount::isLocked))
				;
		// @formatter:on
	}

	private void assertNoRequestLock(String key) {
		// @formatter:off
		then(requestLocks)
				.as("Request lock not present")
				.doesNotContainKey(key)
				;
		// @formatter:on
	}

	@Test
	public void non200Result() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somewhere");

		final String cacheKey = "test.key";
		expect(service.keyForRequest(request)).andReturn(cacheKey);

		// cache miss
		expect(service.sendCachedResponse(eq(cacheKey), same(request), same(response))).andReturn(null);

		// handle request
		chain.doFilter(same(request), assertWith(new Assertion<ServletResponse>() {

			@Override
			public void check(ServletResponse argument) throws Throwable {
				assertRequestLock(cacheKey, true);
				assertLockPoolSize(TEST_LOCK_POOL_CAPACITY - 1);
				HttpServletResponse resp = (HttpServletResponse) argument;
				resp.setStatus(500);
				resp.setContentType("text/html");
				resp.getWriter().print("<html>Error!</html>");
			}

		}));

		// when
		replayAll();

		filter.doFilter(request, response, chain);

		// then
		assertThat("Response status", response.getStatus(), equalTo(500));
		assertThat("Response content type", response.getHeader(HttpHeaders.CONTENT_TYPE),
				equalTo("text/html"));
		assertThat("Response body", response.getContentAsString(), equalTo("<html>Error!</html>"));
		assertNoRequestLock(cacheKey);
		assertLockPoolSize(TEST_LOCK_POOL_CAPACITY);
	}

	@Test
	public void cacheMiss() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somewhere");

		final String cacheKey = "test.key";
		expect(service.keyForRequest(request)).andReturn(cacheKey);

		// cache miss
		expect(service.sendCachedResponse(eq(cacheKey), same(request), same(response))).andReturn(null);

		// handle request
		chain.doFilter(same(request), assertWith(new Assertion<ServletResponse>() {

			@Override
			public void check(ServletResponse argument) throws Throwable {
				assertRequestLock(cacheKey, true);
				assertLockPoolSize(TEST_LOCK_POOL_CAPACITY - 1);
				HttpServletResponse resp = (HttpServletResponse) argument;
				resp.setStatus(200);
				resp.setContentType("text/plain");
				resp.getWriter().print("Hello, world.");
			}

		}));

		// cache response
		Capture<InputStream> bodyCaptor = new Capture<>();
		service.cacheResponse(eq(cacheKey), same(request), eq(200), anyObject(HttpHeaders.class),
				capture(bodyCaptor), eq(CompressionType.GZIP));

		// when
		replayAll();

		filter.doFilter(request, response, chain);

		// then
		assertThat("Response status", response.getStatus(), equalTo(200));
		assertThat("Response content type", response.getHeader(HttpHeaders.CONTENT_TYPE),
				equalTo("text/plain"));
		assertThat("Response body", response.getContentAsString(), equalTo("Hello, world."));
		assertNoRequestLock(cacheKey);
		assertLockPoolSize(TEST_LOCK_POOL_CAPACITY);
	}

	@Test
	public void cacheHit() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somewhere");

		final String cacheKey = "test.key";
		expect(service.keyForRequest(request)).andReturn(cacheKey);

		// cache hit
		final SimpleCachedContent content = new SimpleCachedContent(new HttpHeaders(), new byte[0]);
		expect(service.sendCachedResponse(eq(cacheKey), same(request), same(response)))
				.andReturn(content);

		// when
		replayAll();

		filter.doFilter(request, response, chain);

		// then
		assertNoRequestLock(cacheKey);
		assertLockPoolSize(TEST_LOCK_POOL_CAPACITY);
	}

	@Test
	public void cacheMissConcurrent() throws ServletException, IOException, InterruptedException {
		// given
		MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/somewhere");
		MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/somewhere");

		final String cacheKey = "test.key";
		expect(service.keyForRequest(request1)).andReturn(cacheKey);
		expect(service.keyForRequest(request2)).andReturn(cacheKey);

		// cache miss
		expect(service.sendCachedResponse(eq(cacheKey), same(request1), same(response))).andReturn(null);

		MockHttpServletResponse response2 = new MockHttpServletResponse();
		final SimpleCachedContent content = new SimpleCachedContent(new HttpHeaders(), new byte[0]);
		expect(service.sendCachedResponse(eq(cacheKey), same(request2), same(response2)))
				.andReturn(content);

		// handle request 1
		chain.doFilter(same(request1), assertWith(new Assertion<ServletResponse>() {

			private final AtomicBoolean handled = new AtomicBoolean(false);

			@Override
			public void check(ServletResponse argument) throws Throwable {
				if ( !handled.compareAndSet(false, true) ) {
					throw new RuntimeException("Response should only be called once.");
				}

				assertRequestLock(cacheKey, true);
				assertLockPoolSize(TEST_LOCK_POOL_CAPACITY - 1);
				HttpServletResponse resp = (HttpServletResponse) argument;
				resp.setStatus(200);
				resp.setContentType("text/plain");
				resp.getWriter().print("Hello, world.");

				// sleep for a spell to make other threads wait
				Thread.sleep(2000);
			}

		}));

		// cache response
		Capture<InputStream> bodyCaptor = new Capture<>();
		service.cacheResponse(eq(cacheKey), same(request1), eq(200), anyObject(HttpHeaders.class),
				capture(bodyCaptor), eq(CompressionType.GZIP));

		// when
		replayAll();

		// start request 1
		Thread req1Thread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					filter.doFilter(request1, response, chain);
				} catch ( Exception e ) {
					throw new RuntimeException(e);
				}

			}
		}, "Request 1");

		Thread req2Thread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(100); // give req1 a head start
					filter.doFilter(request2, response2, chain);
				} catch ( Exception e ) {
					throw new RuntimeException(e);
				}

			}
		}, "Request 2");

		req1Thread.start();
		req2Thread.start();

		// wait for requests to complete
		req1Thread.join(10000);
		req2Thread.join(10000);

		// then
		assertThat("Response status", response.getStatus(), equalTo(200));
		assertThat("Response content type", response.getHeader(HttpHeaders.CONTENT_TYPE),
				equalTo("text/plain"));
		assertThat("Response body", response.getContentAsString(), equalTo("Hello, world."));

		assertNoRequestLock(cacheKey);
		assertLockPoolSize(TEST_LOCK_POOL_CAPACITY);
	}

	@Test
	public void cacheMissTimeout() throws ServletException, IOException, InterruptedException {
		// GIVEN
		filter.setRequestLockTimeout(200L);
		MockHttpServletRequest request1 = new MockHttpServletRequest("GET", "/somewhere");
		MockHttpServletRequest request2 = new MockHttpServletRequest("GET", "/somewhere");

		final String cacheKey = "test.key";
		expect(service.keyForRequest(request1)).andReturn(cacheKey);
		expect(service.keyForRequest(request2)).andReturn(cacheKey);

		// cache miss
		expect(service.sendCachedResponse(eq(cacheKey), same(request1), same(response))).andReturn(null);

		MockHttpServletResponse response2 = new MockHttpServletResponse();

		final String responseContentType = "text/plain";
		final String responseContent = "Hello, world.";

		// handle request 1
		chain.doFilter(same(request1), assertWith(new Assertion<ServletResponse>() {

			private final AtomicBoolean handled = new AtomicBoolean(false);

			@Override
			public void check(ServletResponse argument) throws Throwable {
				if ( !handled.compareAndSet(false, true) ) {
					throw new RuntimeException("Response should only be called once.");
				}
				assertRequestLock(cacheKey, true);
				assertLockPoolSize(TEST_LOCK_POOL_CAPACITY - 1);
				HttpServletResponse resp = (HttpServletResponse) argument;
				resp.setStatus(200);
				resp.setContentType(responseContentType);
				resp.getWriter().print(responseContent);

				// sleep for a spell to make other threads wait
				Thread.sleep(1000);
			}

		}));

		// cache response
		Capture<InputStream> bodyCaptor = new Capture<>();
		service.cacheResponse(eq(cacheKey), same(request1), eq(200), anyObject(HttpHeaders.class),
				capture(bodyCaptor), eq(CompressionType.GZIP));

		// WHEN
		replayAll();

		// start request 1
		Thread req1Thread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					filter.doFilter(request1, response, chain);
				} catch ( Exception e ) {
					throw new RuntimeException(e);
				}

			}
		}, "Request 1");

		Thread req2Thread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(100); // give req1 a head start
					filter.doFilter(request2, response2, chain);
				} catch ( Exception e ) {
					throw new RuntimeException(e);
				}

			}
		}, "Request 2");

		req1Thread.start();
		req2Thread.start();

		// wait for requests to complete
		req1Thread.join(10000);
		req2Thread.join(10000);

		// THEN
		// @formatter:off
		then(response)
			.as("First response status OK")
			.returns(HttpStatus.OK.value(), from(HttpServletResponse::getStatus))
			.as("First response content type from target")
			.returns(responseContentType, from((r) -> r.getHeader(HttpHeaders.CONTENT_TYPE)))
			.as("First response body from target")
			.returns(responseContent, from((r) -> {
				try {
					return r.getContentAsString();
				} catch ( UnsupportedEncodingException e ) {
					throw new RuntimeException(e);
				}
			}))
			;
		then(response2)
			.as("Second response status 429")
			.returns(HttpStatus.TOO_MANY_REQUESTS.value(), from(HttpServletResponse::getStatus))
			;
		// @formatter:on

		assertNoRequestLock(cacheKey);
		assertLockPoolSize(TEST_LOCK_POOL_CAPACITY);
	}

	@Test
	public void highlyConcurrent() throws ServletException, IOException, InterruptedException {
		final int requestCount = 100;
		final RandomGenerator rng = new SecureRandom();
		final int urlCount = 5;
		final ConcurrentMap<String, String> uriToKeyMap = new ConcurrentHashMap<>(urlCount);
		final ConcurrentMap<String, CachedContent> keyToContentMap = new ConcurrentHashMap<>(urlCount);
		for ( int i = 0; i < urlCount; i++ ) {
			String url = "/" + i;
			String key = "key." + i;
			uriToKeyMap.put(url, key);

			String content = "hit! " + key;
			HttpHeaders headers = new HttpHeaders();
			keyToContentMap.put(key, new SimpleCachedContent(headers, content.getBytes(UTF_8)));
		}

		expect(service.keyForRequest(anyObject())).andAnswer(() -> {
			// req
			final Object[] args = EasyMock.getCurrentArguments();
			final HttpServletRequest req = (HttpServletRequest) args[0];
			final String uri = req.getRequestURI();
			return uriToKeyMap.get(uri);
		}).anyTimes();

		// cache miss
		expect(service.sendCachedResponse(anyObject(), anyObject(), anyObject())).andAnswer(() -> {
			// args are key, req, res
			final Object[] args = EasyMock.getCurrentArguments();
			// randomly cache hit vs miss
			if ( rng.nextBoolean() ) {
				// cache hit
				CachedContent content = keyToContentMap.get(args[0]);

				HttpServletResponse resp = (HttpServletResponse) args[2];
				resp.setStatus(200);
				resp.setContentType("text/plain");
				FileCopyUtils.copy(content.getContent(), resp.getOutputStream());
				return keyToContentMap.get(args[0]);
			}
			// cache miss
			return null;
		}).anyTimes();

		final List<String> msgLog = synchronizedList(new ArrayList<>(requestCount));

		chain.doFilter(anyObject(), assertWith(new Assertion<ServletResponse>() {

			@Override
			public void check(ServletResponse argument) throws Throwable {
				// req, res
				final Object[] args = EasyMock.getCurrentArguments();
				HttpServletRequest req = (HttpServletRequest) args[0];

				// sleep for a spell to make other threads wait
				Thread.sleep(rng.nextLong(500));

				HttpServletResponse resp = (HttpServletResponse) argument;
				resp.setStatus(200);
				resp.setContentType("text/plain");

				String url = req.getRequestURI();
				String key = uriToKeyMap.get(url);

				msgLog.add("Generating content for URL %s key %s".formatted(url, key));

				FileCopyUtils.copy(("miss! " + key).getBytes(UTF_8), resp.getOutputStream());
			}

		}));

		expectLastCall().anyTimes();

		filter.setRequestLockTimeout(200L);

		// WHEN
		replayAll();

		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for ( int i = 0; i < requestCount; i++ ) {
				final int reqNum = i;
				executor.submit(() -> {
					final String url = "/" + rng.nextInt(urlCount);
					log.debug("Requesting URL {}: {}", reqNum, url);
					MockHttpServletRequest req = new MockHttpServletRequest("GET", url);
					MockHttpServletResponse res = new MockHttpServletResponse();
					try {
						filter.doFilter(req, res, chain);
					} catch ( IOException | ServletException e ) {
						log.info("Exception handling request {}: {}", reqNum, e);
					}
					if ( res.getStatus() == 200 ) {
						try {
							msgLog.add("Got URL %d %s OK response: %s".formatted(reqNum, url,
									res.getContentAsString()));
						} catch ( UnsupportedEncodingException e ) {
							// blah
						}
					} else {
						log.debug("URL {} response: {}", reqNum, res.getStatus());
					}
				});
			}
		}

		// THEN
		then(requestLocks).as("No request locks active").isEmpty();
		assertLockPoolSize(TEST_LOCK_POOL_CAPACITY);
		log.info("Message log: [{}]", msgLog.stream().collect(Collectors.joining("\n\t", "\n\t", "\n")));
	}

}
