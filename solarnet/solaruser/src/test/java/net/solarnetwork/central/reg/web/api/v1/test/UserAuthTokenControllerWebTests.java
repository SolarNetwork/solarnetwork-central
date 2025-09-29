/* ==================================================================
 * UserAuthTokenControllerWebTests.java - 30/09/2025 7:05:03 am
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import net.solarnetwork.central.reg.test.WithMockSecurityUser;
import net.solarnetwork.central.reg.web.api.v1.UserAuthTokenController;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.domain.UserAuthToken;

/**
 * Web API level integration tests for the {@link UserAuthTokenController}
 * class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class UserAuthTokenControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final Long TEST_USER_ID = 1L;
	private static final String TEST_EMAIL = "test1@localhost";
	private static final String TEST_TOKEN_SECRET = "secret";

	@Autowired
	private UserAuthTokenDao userAuthTokenDao;

	@Autowired
	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		setupTestUser(TEST_USER_ID, TEST_EMAIL);
	}

	@Test
	@WithMockSecurityUser
	public void createToken_withoutPolicy() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(post("/api/v1/sec/user/auth-tokens/generate/User")
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":{
						"userId":1,
						"status":"Active",
						"type":"User",
						"expired":false
					}}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void createToken_withPolicy() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(post("/api/v1/sec/user/auth-tokens/generate/ReadNodeData")
				.content("""
						{
							"sourceIds": ["test/**"],
							"refreshAllowed": true
						}
						""")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":{
						"userId":1,
						"status":"Active",
						"type":"ReadNodeData",
						"policy":{
							"sourceIds":["test/**"],
							"refreshAllowed":true
						},
						"expired":false
					}}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void mergePolicy() throws Exception {
		// GIVEN
		final String testTokenId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
		insertSecurityToken(jdbcTemplate, testTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");
		final UserAuthToken testToken = userAuthTokenDao.get(testTokenId);

		// WHEN
		// @formatter:off
		mvc.perform(patch("/api/v1/sec/user/auth-tokens/policy")
				.param("tokenId", testTokenId)
				.content("""
						{
							"sourceIds": ["test/**"],
							"refreshAllowed": true
						}
						""")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":{
						"id":"%s",
						"created":"%s",
						"userId":1,
						"status":"Active",
						"type":"ReadNodeData",
						"policy":{
							"nodeMetadataPaths": ["/pm/foo/**"],
							"sourceIds":["test/**"],
							"refreshAllowed":true
						},
						"expired":false
					}}
					""".formatted(
							testTokenId,
							ISO_DATE_TIME_ALT_UTC.format(testToken.getCreated())
						),
					JsonCompareMode.STRICT))
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void replacePolicy() throws Exception {
		// GIVEN
		final String testTokenId = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 20);
		insertSecurityToken(jdbcTemplate, testTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");
		final UserAuthToken testToken = userAuthTokenDao.get(testTokenId);

		// WHEN
		// @formatter:off
		mvc.perform(put("/api/v1/sec/user/auth-tokens/policy")
				.param("tokenId", testTokenId)
				.content("""
						{
							"sourceIds": ["test/**"],
							"refreshAllowed": true
						}
						""")
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":{
						"id":"%s",
						"created":"%s",
						"userId":1,
						"status":"Active",
						"type":"ReadNodeData",
						"policy":{
							"sourceIds":["test/**"],
							"refreshAllowed":true
						},
						"expired":false
					}}
					""".formatted(
							testTokenId,
							ISO_DATE_TIME_ALT_UTC.format(testToken.getCreated())
						),
					JsonCompareMode.STRICT))
			;
		// @formatter:on
	}

}
