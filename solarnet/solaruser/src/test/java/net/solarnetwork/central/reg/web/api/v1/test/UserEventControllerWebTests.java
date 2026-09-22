/* ==================================================================
 * UserEventControllerWebTests.java - 23/09/2026 9:58:41 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static net.solarnetwork.central.security.SecurityTokenStatus.Active;
import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.codec.jackson.JsonUtils.getJSONString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import net.solarnetwork.central.reg.web.api.v1.UserEventController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the {@link UserEventController} class.
 *
 * <p>
 * Events have unstructured data, so a security policy cannot be applied to
 * them: only tokens with an unrestricted policy can query them.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class UserEventControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final String EVENTS_PATH = "/api/v1/sec/user/events";

	private static final String START_DATE = "2026-01-01T00:00:00Z";
	private static final String END_DATE = "2026-01-02T00:00:00Z";

	@Autowired
	private MockMvc mvc;

	private Long userId;

	@BeforeEach
	public void setup() {
		userId = randomLong();
		setupTestUser(userId, randomString() + "@localhost");
	}

	private ResultActions performSigned(String tokenId, String tokenSecret) throws Exception {
		final Instant now = Instant.now();
		final Map<String, String> params = Map.of("startDate", START_DATE, "endDate", END_DATE);

		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path(EVENTS_PATH)
				.queryParams(params)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);

		return mvc.perform(get(EVENTS_PATH)
				.param("startDate", START_DATE)
				.param("endDate", END_DATE)
				.header(HttpHeaders.AUTHORIZATION, auth.build())
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			);
		// @formatter:on
	}

	@Test
	public void listEvents_asUnrestrictedToken() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		// THEN
		performSigned(tokenId, tokenSecret).andExpect(status().isOk());
	}

	@Test
	public void listEvents_asRestrictedToken_denied() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, getJSONString(
				BasicSecurityPolicy.builder().withNodeIds(Set.of(randomLong())).build(), null));

		// THEN
		performSigned(tokenId, tokenSecret).andExpect(status().isForbidden());
	}

}
