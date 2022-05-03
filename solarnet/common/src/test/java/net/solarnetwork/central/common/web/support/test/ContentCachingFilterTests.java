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

package net.solarnetwork.central.common.web.support.test;

import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import net.solarnetwork.central.web.support.ContentCachingFilter;
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

	private static final int TEST_LOCK_POOL_CAPACITY = 8;

	private FilterChain chain;
	private ContentCachingService service;
	private ContentCachingFilter filter;

	private MockHttpServletResponse response;

	@Before
	public void setup() {
		chain = EasyMock.createMock(FilterChain.class);
		service = EasyMock.createMock(ContentCachingService.class);

		filter = new ContentCachingFilter(service);
		filter.setLockPoolCapacity(TEST_LOCK_POOL_CAPACITY);
		filter.setRequestLockTimeout(60000);
		filter.serviceDidStartup();

		response = new MockHttpServletResponse();
	}

	private void replayAll() {
		EasyMock.replay(chain, service);
	}

	@After
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
	}
}
