/* ==================================================================
 * ContentCachingFilterTests2.java - 17/01/2025 9:13:44 am
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

package net.solarnetwork.central.common.web.support.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.will;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import net.solarnetwork.central.web.support.ContentCachingFilter;
import net.solarnetwork.central.web.support.ContentCachingFilter.LockAndCount;
import net.solarnetwork.central.web.support.ContentCachingResponseWrapper;
import net.solarnetwork.central.web.support.ContentCachingService;
import net.solarnetwork.central.web.support.ContentCachingService.CompressionType;

/**
 * Test casses for the {@link ContentCachingFilter} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("static-access")
public class ContentCachingFilterTests2 {

	private static final int TEST_LOCK_POOL_CAPACITY = 8;

	@Mock
	private FilterChain chain;

	@Mock
	private ContentCachingService service;

	@Captor
	private ArgumentCaptor<HttpServletResponse> responseCaptor;

	private BlockingQueue<LockAndCount> lockPool;
	private ConcurrentMap<String, LockAndCount> requestLocks;
	private ContentCachingFilter filter;

	private MockHttpServletResponse response;

	@BeforeEach
	public void setup() {
		lockPool = ContentCachingFilter.lockPoolWithCapacity(TEST_LOCK_POOL_CAPACITY);
		requestLocks = new ConcurrentHashMap<>(32);

		filter = new ContentCachingFilter(service, lockPool, requestLocks);
		filter.setRequestLockTimeout(60000);

		response = new MockHttpServletResponse();
	}

	private void assertLockPoolSize(int count) {
		and.then(lockPool).as("Look pool size").hasSize(count);
	}

	private void assertRequestLock(String key, boolean locked) {
		// @formatter:off
		and.then(requestLocks)
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
		and.then(requestLocks)
				.as("Request lock not present")
				.doesNotContainKey(key)
				;
		// @formatter:on
	}

	@Test
	public void cacheMiss_IOException() throws ServletException, IOException {
		// GIVEN
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/somewhere");
		request.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");

		final String cacheKey = "test.key";
		given(service.keyForRequest(request)).willReturn(cacheKey);

		// cache miss
		given(service.sendCachedResponse(eq(cacheKey), same(request), same(response))).willReturn(null);

		// handle request
		final var ex = new IOException("Boom!");
		will(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				assertRequestLock(cacheKey, true);
				assertLockPoolSize(TEST_LOCK_POOL_CAPACITY - 1);
				HttpServletResponse resp = (HttpServletResponse) invocation.getArgument(1);
				resp.setStatus(200);
				resp.setContentType("text/plain");
				resp.getOutputStream().write("Hello".getBytes(UTF_8));
				resp.getOutputStream().write(", world".getBytes(UTF_8));
				resp.getOutputStream().write(".".getBytes(UTF_8));
				throw ex;
			}
		}).given(chain).doFilter(same(request), any());

		// cache response
		service.cacheResponse(eq(cacheKey), same(request), eq(200), any(), any(),
				eq(CompressionType.GZIP));

		// WHEN
		thenThrownBy(() -> {
			filter.doFilter(request, response, chain);
		}, "IOException thrown").isSameAs(ex);

		// THEN
		and.then(response.getStatus()).as("Response status OK").isEqualTo(200);
		and.then(response.getHeader(HttpHeaders.CONTENT_TYPE)).as("Response content type")
				.isEqualTo("text/plain");
		and.then(response.getContentAsString()).as("Response body").isEqualTo("Hello, world.");
		assertNoRequestLock(cacheKey);
		assertLockPoolSize(TEST_LOCK_POOL_CAPACITY);

		then(chain).should().doFilter(any(), responseCaptor.capture());
		and.then(responseCaptor.getValue()).as("Caching servlet response wrapper used")
				.isInstanceOf(ContentCachingResponseWrapper.class)
				.asInstanceOf(type(ContentCachingResponseWrapper.class));
	}

}
