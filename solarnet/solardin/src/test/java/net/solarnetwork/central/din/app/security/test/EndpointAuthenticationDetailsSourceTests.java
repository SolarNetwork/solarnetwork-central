/* ==================================================================
 * EndpointAuthenticationDetailsSourceTests.java - 23/02/2024 4:41:15 pm
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

package net.solarnetwork.central.din.app.security.test;

import static java.time.Instant.now;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import net.solarnetwork.central.din.app.security.EndpointAuthenticationDetails;
import net.solarnetwork.central.din.app.security.EndpointAuthenticationDetailsSource;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.domain.EndpointConfiguration;

/**
 * Test cases for the {@link EndpointAuthenticationDetailsSource} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class EndpointAuthenticationDetailsSourceTests {

	@Mock
	private EndpointConfigurationDao endpointDao;

	private EndpointAuthenticationDetailsSource service;

	@BeforeEach
	public void setup() {
		service = new EndpointAuthenticationDetailsSource(endpointDao,
				EndpointAuthenticationDetailsSource.DEFAULT_ENDPOINT_ID_PATTERN);
	}

	@Test
	public void ok() {
		// GIVEN
		final Long userId = randomLong();
		final UUID endpointId = UUID.randomUUID();

		final String path = "/foo/endpoint/%s/datum".formatted(endpointId);
		final String url = "http://localhost".concat(path);
		final MockHttpServletRequest req = new MockHttpServletRequest("GET", url);
		req.setServletPath(path);

		final EndpointConfiguration endpoint = new EndpointConfiguration(userId, endpointId, now());
		given(endpointDao.getForEndpointId(endpointId)).willReturn(endpoint);

		// WHEN
		EndpointAuthenticationDetails result = service.buildDetails(req);

		// @formatter:off
		then(result)
			.as("Details created")
			.isNotNull()
			.as("User ID populated")
			.returns(userId, EndpointAuthenticationDetails::getUserId)
			.as("Endpoint ID populated")
			.returns(endpointId, EndpointAuthenticationDetails::getEndpointId)
			;
		// @formatter:on
	}

	@Test
	public void noMatch() {
		// GIVEN
		final String path = "/some/path";
		final String url = "http://localhost".concat(path);
		final MockHttpServletRequest req = new MockHttpServletRequest("GET", url);
		req.setPathInfo(path);

		// WHEN
		EndpointAuthenticationDetails result = service.buildDetails(req);

		// @formatter:off
		then(result)
			.as("Details created")
			.isNotNull()
			.as("Endpoint ID not found")
			.returns(null, EndpointAuthenticationDetails::getEndpointId)
			;
		// @formatter:on
	}

}
