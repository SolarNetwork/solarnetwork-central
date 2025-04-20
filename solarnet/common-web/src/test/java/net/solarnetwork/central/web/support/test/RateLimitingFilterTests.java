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

import static net.solarnetwork.central.web.support.RateLimitingFilter.idForString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
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
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.central.web.RateLimitExceededException;
import net.solarnetwork.central.web.support.RateLimitingFilter;

/**
 * Test cases for the {@link RateLimitingFilter} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class RateLimitingFilterTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final int TEST_CAPACITY = 3;
	private static final Duration TEST_DURATION = Duration.ofSeconds(1);

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
	}

	private static String snws2Cred(String tokenId) {
		return "SNWS2 Credential=%s,SignedHeaders=date;host,Signature=abc123".formatted(tokenId);
	}

	@Test
	public void tokenAuth_initialCapacityRequests_ok() throws ServletException, IOException {
		// GIVEN
		final List<MockHttpServletResponse> responses = new ArrayList<>(TEST_CAPACITY);

		final String tokenId = CommonTestUtils.randomString();

		// WHEN
		for ( int i = 0; i < TEST_CAPACITY; i++ ) {
			final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
			req.addHeader(AUTHORIZATION, snws2Cred(tokenId));

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
		final String tokenId = CommonTestUtils.randomString();

		// WHEN
		for ( int i = 0; i < TEST_CAPACITY; i++ ) {
			final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
			req.addHeader(AUTHORIZATION, snws2Cred(tokenId));

			final MockHttpServletResponse res = new MockHttpServletResponse();

			final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

			filter.doFilter(req, res, chain);
		}

		final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
		req.addHeader(AUTHORIZATION, snws2Cred(tokenId));

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

		final String tokenId = CommonTestUtils.randomString();

		// WHEN
		for ( int i = 0; i < TEST_CAPACITY; i++ ) {
			final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
			req.addHeader(AUTHORIZATION, snws2Cred(tokenId));

			final MockHttpServletResponse res = new MockHttpServletResponse();

			final MockFilterChain chain = new MockFilterChain(servlet, nextFilter);

			filter.doFilter(req, res, chain);
		}

		final MockHttpServletRequest req = new MockHttpServletRequest(GET.toString(), "/foo");
		req.addHeader(AUTHORIZATION, snws2Cred(tokenId));

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

}
