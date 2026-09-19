/* ==================================================================
 * RateLimitingFilterTests.java - 19/04/2025 11:56:05 am
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

package net.solarnetwork.central.web.support.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomBytes;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.web.support.RateLimitingFilter.idForString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.http.HttpMethod.GET;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.web.RateLimitExceededException;
import net.solarnetwork.central.web.support.RateLimitingFilter;

/**
 * Test cases for the {@link RateLimitingFilter} class.
 *
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class RateLimitingFilterTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final int TEST_CAPACITY = 3;
	private static final Duration TEST_DURATION = Duration.ofSeconds(1);
	private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

	@Autowired
	private DataSource dataSource;

	@Mock
	private Filter nextFilter;

	@Mock
	private Servlet servlet;

	@Mock
	private HandlerExceptionResolver handlerExceptionResolver;

	@Captor
	private ArgumentCaptor<Exception> exceptionCaptor;

	private RateLimitingFilter filter;

	@BeforeEach
	public void setup() {
		ExpirationAfterWriteStrategy expiration = ExpirationAfterWriteStrategy
				.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(1));
		ProxyManager<Long> proxyManager = Bucket4jPostgreSQL.advisoryLockBasedBuilder(dataSource)
				.expirationAfterWrite(expiration).table("solarcommon.bucket").build();

		Supplier<BucketConfiguration> configurationProvider = () -> BucketConfiguration.builder()
				.addLimit(Bandwidth.builder().capacity(TEST_CAPACITY).refillGreedy(1, TEST_DURATION)
						.build())
				.build();

		filter = new RateLimitingFilter(proxyManager, configurationProvider);
		filter.setExceptionResolver(handlerExceptionResolver);

		jdbcTemplate.update("DELETE FROM solarcommon.bucket");
	}

	@AfterEach
	public void teardown() {
		jdbcTemplate.update("DELETE FROM solarcommon.bucket");
		SecurityUtils.removeAuthentication();
	}

	// bucket rows outlive the test transaction, so use unique keys per test
	private static String randomIpAddress() {
		final byte[] addr = randomBytes(4);
		return String.format("%d.%d.%d.%d", addr[0] & 0xFF, addr[1] & 0xFF, addr[2] & 0xFF,
				addr[3] & 0xFF);
	}

	@Test
	public void tokenAuth_initialCapacityRequests_ok() throws ServletException, IOException {
		// GIVEN
		final List<MockHttpServletResponse> responses = new ArrayList<>(TEST_CAPACITY);

		final String tokenId = randomString();
		final Long userId = randomLong();
		SecurityUtils.becomeToken(tokenId, SecurityTokenType.ReadNodeData, userId, null);

		// WHEN
		for ( int i = 0; i < TEST_CAPACITY; i++ ) {
			final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");

			final MockHttpServletResponse res = new MockHttpServletResponse();
			responses.add(res);

			final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

			filter.doFilter(req, res, chain);
		}

		List<Map<String, Object>> rows = CommonDbTestUtils.allTableData(log, jdbcTemplate,
				"solarcommon.bucket", "id");

		// THEN
		// @formatter:off
		then(nextFilter).should(times(TEST_CAPACITY)).doFilter(any(), any(), any());

		and.then(responses)
			.satisfies(list -> {
				for (int i = 0; i < TEST_CAPACITY; i++ ) {
					final int reqNum = i + 1;
					and.then(list).element(i)
						.satisfies(res -> {
							and.then(res.getHeader(RateLimitingFilter.X_SN_RATE_LIMIT_REMAINING_HEADER))
								.as("Rate limit remaining header for resopnse %d deducted from capacity", reqNum)
								.isEqualTo(String.valueOf(TEST_CAPACITY - reqNum))
								;
						})
						;
				}
			})
			;

		and.then(rows)
			.as("Bucket row created")
			.hasSize(1)
			.element(0, map(String.class, Object.class))
			.as("ID for token ID")
			.containsEntry("id", RateLimitingFilter.idForString(tokenId))
			;
		// @formatter:on
	}

	@Test
	public void tokenAuth_overLimit() throws ServletException, IOException {
		// GIVEN
		final String tokenId = randomString();
		final Long userId = randomLong();
		SecurityUtils.becomeToken(tokenId, SecurityTokenType.ReadNodeData, userId, null);

		// WHEN
		for ( int i = 0; i < TEST_CAPACITY; i++ ) {
			final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");

			final MockHttpServletResponse res = new MockHttpServletResponse();

			final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

			filter.doFilter(req, res, chain);
		}

		final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

		filter.doFilter(req, res, chain);

		// THEN
		// @formatter:off
		then(handlerExceptionResolver).should().resolveException(any(), any(), any(), exceptionCaptor.capture());
		and.then(exceptionCaptor.getValue())
			.as("Thrown exception is rate limit")
			.isInstanceOf(RateLimitExceededException.class)
			.asInstanceOf(type(RateLimitExceededException.class))
			.as("Exception key is token ID")
			.returns(tokenId, from(RateLimitExceededException::getKey))
			.as("Exception ID is for token ID")
			.returns(idForString(tokenId), from(RateLimitExceededException::getId))
			;

		and.then(res.getHeader(RateLimitingFilter.X_SN_RATE_LIMIT_RETRY_AFTER))
			.as("Retry after header provided")
			.isNotNull()
			;

		List<Map<String, Object>> rows = CommonDbTestUtils.allTableData(log, jdbcTemplate,
				"solarcommon.bucket", "id");

		and.then(rows)
			.as("Bucket row created")
			.hasSize(1)
			.element(0, map(String.class, Object.class))
			.as("ID for token ID")
			.containsEntry("id", idForString(tokenId))
			;
		// @formatter:on
	}

	@Test
	public void tokenAuth_overLimit_noResolver() throws ServletException, IOException {
		// GIVEN
		filter.setExceptionResolver(null);

		final String tokenId = randomString();
		final Long userId = randomLong();
		SecurityUtils.becomeToken(tokenId, SecurityTokenType.ReadNodeData, userId, null);

		// WHEN
		for ( int i = 0; i < TEST_CAPACITY; i++ ) {
			final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");

			final MockHttpServletResponse res = new MockHttpServletResponse();

			final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

			filter.doFilter(req, res, chain);
		}

		final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

		filter.doFilter(req, res, chain);

		// THEN
		// @formatter:off
		then(handlerExceptionResolver).shouldHaveNoInteractions();

		and.then(res)
			.as("Too many requests status returned")
			.returns(HttpStatus.TOO_MANY_REQUESTS.value(), from(MockHttpServletResponse::getStatus))
			;

		and.then(res.getHeader(RateLimitingFilter.X_SN_RATE_LIMIT_RETRY_AFTER))
			.as("Retry after header provided")
			.isNotNull()
			;

		List<Map<String, Object>> rows = CommonDbTestUtils.allTableData(log, jdbcTemplate,
				"solarcommon.bucket", "id");

		and.then(rows)
			.as("Bucket row created")
			.hasSize(1)
			.element(0, map(String.class, Object.class))
			.as("ID for token ID")
			.containsEntry("id", idForString(tokenId))
			;
		// @formatter:on
	}

	@Test
	public void anonymous_forwardedFor_keyedByRemoteAddress() throws ServletException, IOException {
		// GIVEN
		final String remoteAddr = randomIpAddress();

		// WHEN
		final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
		req.setRemoteAddr(remoteAddr);
		req.addHeader(X_FORWARDED_FOR_HEADER, randomIpAddress());

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

		filter.doFilter(req, res, chain);

		List<Map<String, Object>> rows = CommonDbTestUtils.allTableData(log, jdbcTemplate,
				"solarcommon.bucket", "id");

		// THEN
		// @formatter:off
		then(nextFilter).should().doFilter(any(), any(), any());

		and.then(res.getHeader(RateLimitingFilter.X_SN_RATE_LIMIT_REMAINING_HEADER))
			.as("Rate limit remaining header deducted from capacity")
			.isEqualTo(String.valueOf(TEST_CAPACITY - 1))
			;

		and.then(rows)
			.as("Bucket row created")
			.hasSize(1)
			.element(0, map(String.class, Object.class))
			.as("ID for remote address, not X-Forwarded-For header value")
			.containsEntry("id", idForString(remoteAddr))
			;
		// @formatter:on
	}

	@Test
	public void anonymous_overLimit_varyingForwardedFor() throws ServletException, IOException {
		// GIVEN
		final String remoteAddr = randomIpAddress();

		// WHEN
		// claim to be a different client on every request
		for ( int i = 0; i < TEST_CAPACITY + 1; i++ ) {
			final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
			req.setRemoteAddr(remoteAddr);
			req.addHeader(X_FORWARDED_FOR_HEADER, randomIpAddress());

			final MockHttpServletResponse res = new MockHttpServletResponse();

			final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

			filter.doFilter(req, res, chain);
		}

		// THEN
		// @formatter:off
		then(nextFilter).should(times(TEST_CAPACITY)).doFilter(any(), any(), any());

		then(handlerExceptionResolver).should().resolveException(any(), any(), any(), exceptionCaptor.capture());
		and.then(exceptionCaptor.getValue())
			.as("Thrown exception is rate limit")
			.isInstanceOf(RateLimitExceededException.class)
			.asInstanceOf(type(RateLimitExceededException.class))
			.as("Exception key is remote address")
			.returns(remoteAddr, from(RateLimitExceededException::getKey))
			.as("Exception ID is for remote address")
			.returns(idForString(remoteAddr), from(RateLimitExceededException::getId))
			;

		List<Map<String, Object>> rows = CommonDbTestUtils.allTableData(log, jdbcTemplate,
				"solarcommon.bucket", "id");

		and.then(rows)
			.as("Single bucket row created")
			.hasSize(1)
			.element(0, map(String.class, Object.class))
			.as("ID for remote address")
			.containsEntry("id", idForString(remoteAddr))
			;
		// @formatter:on
	}

	@Test
	public void anonymous_forwardedForOtherClient_otherClientOk() throws ServletException, IOException {
		// GIVEN
		final String remoteAddr = randomIpAddress();
		final String otherRemoteAddr = randomIpAddress();

		// WHEN
		// claim to be the other client, until over the limit
		for ( int i = 0; i < TEST_CAPACITY + 1; i++ ) {
			final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
			req.setRemoteAddr(remoteAddr);
			req.addHeader(X_FORWARDED_FOR_HEADER, otherRemoteAddr);

			final MockHttpServletResponse res = new MockHttpServletResponse();

			final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

			filter.doFilter(req, res, chain);
		}

		final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
		req.setRemoteAddr(otherRemoteAddr);

		final MockHttpServletResponse res = new MockHttpServletResponse();

		final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

		filter.doFilter(req, res, chain);

		// THEN
		// @formatter:off
		then(nextFilter).should(times(TEST_CAPACITY + 1)).doFilter(any(), any(), any());

		then(handlerExceptionResolver).should().resolveException(any(), any(), any(), exceptionCaptor.capture());
		and.then(exceptionCaptor.getValue())
			.as("Thrown exception is rate limit")
			.isInstanceOf(RateLimitExceededException.class)
			.asInstanceOf(type(RateLimitExceededException.class))
			.as("Exception key is remote address of client over the limit")
			.returns(remoteAddr, from(RateLimitExceededException::getKey))
			;

		and.then(res.getHeader(RateLimitingFilter.X_SN_RATE_LIMIT_REMAINING_HEADER))
			.as("Other client request allowed, deducted from its own capacity")
			.isEqualTo(String.valueOf(TEST_CAPACITY - 1))
			;

		List<Map<String, Object>> rows = CommonDbTestUtils.allTableData(log, jdbcTemplate,
				"solarcommon.bucket", "id");

		and.then(rows)
			.as("Bucket row created for each remote address")
			.extracting(row -> row.get("id"))
			.containsExactlyInAnyOrder(idForString(remoteAddr), idForString(otherRemoteAddr))
			;
		// @formatter:on
	}

}
