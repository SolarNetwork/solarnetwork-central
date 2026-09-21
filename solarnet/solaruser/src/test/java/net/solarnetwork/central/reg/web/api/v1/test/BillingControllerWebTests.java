/* ==================================================================
 * BillingControllerWebTests.java - 22/09/2026 9:14:21 am
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
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserRoles;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.Map;
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
import net.solarnetwork.central.reg.config.WebSecurityConfig;
import net.solarnetwork.central.reg.web.api.v1.BillingController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the {@link BillingController} class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class BillingControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final String PREVIEW_PATH = "/api/v1/sec/user/billing/invoices/preview";

	@Autowired
	private MockMvc mvc;

	private Long userId;
	private String tokenId;
	private String tokenSecret;
	private Long otherUserId;

	@BeforeEach
	public void setup() {
		userId = randomLong();
		setupTestUser(userId, randomString() + "@localhost");
		insertUserRoles(jdbcTemplate, userId, WebSecurityConfig.BILLING_AUTHORITY);
		tokenId = randomString(20);
		tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		otherUserId = randomLong();
		setupTestUser(otherUserId, randomString() + "@localhost");
	}

	private ResultActions previewInvoice(Long userId) throws Exception {
		final Instant now = Instant.now();
		final Map<String, String> params = Map.of("userId", userId.toString());
		// @formatter:off
		final String authHeader = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path(PREVIEW_PATH)
				.queryParams(params)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret)
				.build();
		return mvc.perform(get(PREVIEW_PATH)
				.queryParam("userId", userId.toString())
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			);
		// @formatter:on
	}

	@Test
	public void previewInvoice() throws Exception {
		// WHEN
		previewInvoice(userId).andExpect(status().isOk());
	}

	@Test
	public void previewInvoice_otherUser() throws Exception {
		// WHEN
		previewInvoice(otherUserId).andExpect(status().isForbidden());
	}

}
