/* ==================================================================
 * BasicHttpOperationsTests.java - 19/11/2025 10:51:26 am
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

package net.solarnetwork.central.common.http.test;

import static net.solarnetwork.central.common.http.HttpOperations.uri;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.common.http.BasicHttpOperations;
import net.solarnetwork.central.common.http.CachableRequestEntity;
import net.solarnetwork.central.common.http.HttpUserEvents;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.support.SimpleCache;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.Result;

/**
 * Test cases for the {@link BasicHttpOperations} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class BasicHttpOperationsTests implements HttpUserEvents {

	private static final Logger log = LoggerFactory.getLogger(BasicHttpOperationsTests.class);

	private static final List<String> ERROR_TAGS = List.of("test", "error", "http");
	private static final List<String> OK_TAGS = List.of("test", "http");
	private static final String SERVICE_KEY = "test";

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private RestOperations restOps;

	@Mock
	private UserServiceAuditor userServiceAuditor;

	@Captor
	private ArgumentCaptor<RequestEntity<?>> requestEntityCaptor;

	@Captor
	private ArgumentCaptor<LogEventInfo> logEventInfoCaptor;

	private ConcurrentMap<CachableRequestEntity, SimpleCache<CachableRequestEntity, Result<?>>.CachedValue> cacheStore;
	private BasicHttpOperations ops;

	@BeforeEach
	public void setup() {
		ops = new BasicHttpOperations(log, userEventAppenderBiz, restOps, ERROR_TAGS);
		ops.setUserServiceAuditor(userServiceAuditor);
		ops.setUserServiceKey(SERVICE_KEY);

		cacheStore = new ConcurrentHashMap<>(8);
	}

	@Test
	public void httpGet() {
		// GIVEN
		final Long userId = randomLong();
		final String url = "http://example.com/foo";

		final var res = new ResponseEntity<String>(randomString(), HttpStatus.OK);
		given(restOps.exchange(any(), eq(String.class))).willReturn(res);

		// WHEN
		Result<String> result = ops.httpGet(url, null, null, String.class, userId, null);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(requestEntityCaptor.capture(), eq(String.class));
		and.then(requestEntityCaptor.getValue())
			.as("Request URL as provided")
			.returns(uri(url, null), from(RequestEntity::getUrl))
			;
		
		then(userServiceAuditor).shouldHaveNoInteractions();
		
		then(userEventAppenderBiz).should().addEvent(eq(userId), logEventInfoCaptor.capture());
		and.then(logEventInfoCaptor.getValue())
			.as("Event tags from ERROR_TAGS minus 'error'")
			.returns(OK_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(Map.of(
						HTTP_METHOD_DATA_KEY, HttpMethod.GET.toString(),
						HTTP_URI_DATA_KEY, url
					))
			;
		then(userEventAppenderBiz).shouldHaveNoMoreInteractions();

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Result data from HTTP response body")
			.returns(res.getBody(), from(Result::getData))
			;
		// @formatter:on
	}

	@Test
	public void httpGet_cacheMiss() {
		// GIVEN
		ops.setHttpCache(new SimpleCache<>("test", cacheStore));

		final Long userId = randomLong();
		final String url = "http://example.com/foo";

		final var res = new ResponseEntity<String>(randomString(), HttpStatus.OK);
		given(restOps.exchange(any(), eq(String.class))).willReturn(res);

		// WHEN
		Result<String> result = ops.httpGet(url, null, null, String.class, userId, null);

		// THEN
		// @formatter:off
		then(restOps).should().exchange(requestEntityCaptor.capture(), eq(String.class));
		and.then(requestEntityCaptor.getValue())
			.as("Request URL as provided")
			.returns(uri(url, null), from(RequestEntity::getUrl))
			;

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Result data from HTTP response body")
			.returns(res.getBody(), from(Result::getData))
			;
		
		and.then(cacheStore)
			.as("Response was cached")
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void httpGet_cacheHit() {
		// GIVEN
		ops.setHttpCache(new SimpleCache<>("test", cacheStore));

		final Long userId = randomLong();
		final String url = "http://example.com/foo";

		final var res = new ResponseEntity<String>(randomString(), HttpStatus.OK);
		given(restOps.exchange(any(), eq(String.class))).willReturn(res);

		// WHEN
		Result<String> result = ops.httpGet(url, null, null, String.class, userId, null); // cache miss
		Result<String> result2 = ops.httpGet(url, null, null, String.class, userId, null); // should be cache hit

		// THEN
		// @formatter:off
		then(restOps).should().exchange(requestEntityCaptor.capture(), eq(String.class));
		and.then(requestEntityCaptor.getValue())
			.as("Request URL as provided")
			.returns(uri(url, null), from(RequestEntity::getUrl))
			;
		
		then(restOps).shouldHaveNoMoreInteractions(); // because of cache hit

		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("Result data from HTTP response body")
			.returns(res.getBody(), from(Result::getData))
			;
		
		and.then(result2)
			.as("2nd result returned from cache")
			.isSameAs(result)
			;
		
		and.then(cacheStore)
			.as("Response was cached")
			.hasSize(1)
			;
		// @formatter:on
	}

}
