/* ==================================================================
 * WebUtilsTests.java - 28/05/2024 10:43:38 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import net.solarnetwork.central.web.WebUtils;

/**
 * Test cases for the {@link WebUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class WebUtilsTests {

	@Mock
	private Logger log;

	@Captor
	private ArgumentCaptor<String> msgCaptor;

	@Test
	public void requestUriWithQueryParameters_params() {
		// GIVEN
		MockHttpServletRequest req = MockMvcRequestBuilders.get("http://example.com/foo/bar?a=1&b=2")
				.buildRequest(null);

		// WHEN
		String uri = WebUtils.requestUriWithQueryParameters(req);

		// THEN
		and.then(uri).as("Result includes query parameters").isEqualTo("/foo/bar?a=1&b=2");
	}

	@Test
	public void requestUriWithQueryParameters_noParams() {
		// GIVEN
		MockHttpServletRequest req = MockMvcRequestBuilders.get("http://example.com/foo/bar")
				.buildRequest(null);

		// WHEN
		String uri = WebUtils.requestUriWithQueryParameters(req);

		// THEN
		and.then(uri).as("Result just path without any query parameters").isEqualTo("/foo/bar");
	}

	@Test
	public void doWithTransientDataAccessExceptionRetry_ok() {
		// GIVEN
		var req = MockMvcRequestBuilders.get("http://example.com/foo/bar").buildRequest(null);
		var actionResult = "ok";

		// WHEN

		String result = WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			return actionResult;
		}, req, 3, 100L, log);

		// THEN
		then(log).shouldHaveNoInteractions();
		and.then(result).as("Action result returned").isEqualTo(actionResult);
	}

	@Test
	public void doWithTransientDataAccessExceptionRetry_failThenOk() {
		// GIVEN
		var req = MockMvcRequestBuilders.get("http://example.com/foo/bar").buildRequest(null);
		var actionResult = "ok";

		// WHEN
		var count = new AtomicInteger(0);
		String result = WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			if ( count.getAndIncrement() == 0 ) {
				throw new TransientDataAccessResourceException("429");
			}
			return actionResult;
		}, req, 3, 100L, log);

		// THEN
		then(log).should().warn(msgCaptor.capture(), any(Object[].class));
		and.then(result).as("Action result returned").isEqualTo(actionResult);
		and.then(count.intValue()).as("Action called twice").isEqualTo(2);
	}

	@Test
	public void doWithTransientDataAccessExceptionRetry_fail() {
		// GIVEN
		var req = MockMvcRequestBuilders.get("http://example.com/foo/bar").buildRequest(null);

		// WHEN
		var count = new AtomicInteger(0);
		thenThrownBy(() -> {
			WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
				count.incrementAndGet();
				throw new TransientDataAccessResourceException("429");
			}, req, 3, 100L, log);
		}, "TransientDataAccessResourceException thrown after retries exhausted")
				.isInstanceOf(TransientDataAccessResourceException.class);

		// THEN
		then(log).should(times(2)).warn(msgCaptor.capture(), any(Object[].class));
		and.then(count.intValue()).as("Action called 3 times").isEqualTo(3);
	}

}
