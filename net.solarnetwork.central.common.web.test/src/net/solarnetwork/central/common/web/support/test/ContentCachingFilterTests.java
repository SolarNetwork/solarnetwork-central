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
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
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
import net.solarnetwork.test.Assertion;
import net.solarnetwork.util.StaticOptionalService;

/**
 * Test cases for the {@link ContentCachingFilter} class.
 * 
 * @author matt
 * @version 1.0
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

		filter = new ContentCachingFilter();
		filter.setContentCachingService(new StaticOptionalService<ContentCachingService>(service));
		filter.setLockPoolCapacity(TEST_LOCK_POOL_CAPACITY);
		filter.setRequestLockTimeout(60000);
		filter.afterPropertiesSet();

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
	public void noContentCachingService() throws ServletException, IOException {
		// given
		filter.setContentCachingService(null);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somewhere");

		chain.doFilter(same(request), same(response));

		// when
		replayAll();

		filter.doFilter(request, response, chain);

		// then
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
	public void cacheMiss() throws ServletException, IOException {
		// given
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somewhere");

		final String cacheKey = "test.key";
		expect(service.keyForRequest(request)).andReturn(cacheKey);

		// cache miss
		expect(service.sendCachedResponse(eq(cacheKey), same(request), same(response))).andReturn(false);

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
		Capture<HttpHeaders> headersCaptor = new Capture<>();
		Capture<InputStream> bodyCaptor = new Capture<>();
		service.cacheResponse(eq(cacheKey), same(request), eq(200), capture(headersCaptor),
				capture(bodyCaptor));

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
		expect(service.sendCachedResponse(eq(cacheKey), same(request), same(response))).andReturn(true);

		// when
		replayAll();

		filter.doFilter(request, response, chain);

		// then
	}
}
