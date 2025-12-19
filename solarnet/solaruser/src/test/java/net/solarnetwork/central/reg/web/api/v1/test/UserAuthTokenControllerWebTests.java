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
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the {@link UserAuthTokenController}
 * class.
 *
 * @author matt
 * @version 1.1
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

	@Test
	public void createToken_asActorWithUnrestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		// WHEN
		final Instant now = Instant.now();

		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/generate/User")
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/user/auth-tokens/generate/User")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
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
	public void createToken_asActorWithRestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withRefreshAllowed(false).build();
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), getJSONString(policy));

		// WHEN
		final Instant now = Instant.now();

		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/generate/User")
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/user/auth-tokens/generate/User")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":false}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void mergePolicy_asActorWithUnrestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");
		final UserAuthToken updateToken = userAuthTokenDao.get(updateTokenId);

		// WHEN
		final Instant now = Instant.now();
		final String reqJson = """
					{
					"sourceIds": ["test/**"],
					"refreshAllowed": true
				}
				""";

		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PATCH.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/policy")
				.queryParams(Map.of("tokenId", updateTokenId))
				.useSnDate(true).date(now)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(patch("/api/v1/sec/user/auth-tokens/policy")
				.param("tokenId", updateTokenId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
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
							updateTokenId,
							ISO_DATE_TIME_ALT_UTC.format(updateToken.getCreated())
						),
					JsonCompareMode.STRICT))
			;
		// @formatter:on
	}

	@Test
	public void mergePolicy_asActorWithRestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withRefreshAllowed(false).build();
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), getJSONString(policy));

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");

		// WHEN
		final Instant now = Instant.now();
		final String reqJson = """
				{
					"sourceIds": ["test/**"],
					"refreshAllowed": true
				}
				""";

		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PATCH.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/policy")
				.queryParams(Map.of("tokenId", updateTokenId))
				.useSnDate(true).date(now)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(patch("/api/v1/sec/user/auth-tokens/policy")
				.param("tokenId", updateTokenId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":false}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void replacePolicy_asActorWithUnrestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");
		final UserAuthToken updateToken = userAuthTokenDao.get(updateTokenId);

		// WHEN
		final Instant now = Instant.now();
		final String reqJson = """
				{
					"sourceIds": ["test/**"],
					"refreshAllowed": true
				}
				""";

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PUT.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/policy")
				.queryParams(Map.of("tokenId", updateTokenId))
				.useSnDate(true).date(now)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(put("/api/v1/sec/user/auth-tokens/policy")
				.param("tokenId", updateTokenId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
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
							updateTokenId,
							ISO_DATE_TIME_ALT_UTC.format(updateToken.getCreated())
						),
					JsonCompareMode.STRICT))
			;
		// @formatter:on
	}

	@Test
	public void replacePolicy_asActorWithRestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withRefreshAllowed(false).build();
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), getJSONString(policy));

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");

		// WHEN
		final Instant now = Instant.now();
		final String reqJson = """
				{
					"sourceIds": ["test/**"],
					"refreshAllowed": true
				}
				""";

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PUT.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/policy")
				.queryParams(Map.of("tokenId", updateTokenId))
				.useSnDate(true).date(now)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(put("/api/v1/sec/user/auth-tokens/policy")
				.param("tokenId", updateTokenId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":false}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void updateStatus_asActorWithUnrestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/status")
				.queryParams(Map.of("tokenId", updateTokenId, "status", SecurityTokenStatus.Disabled.name()))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/user/auth-tokens/status")
				.param("tokenId", updateTokenId)
				.param("status", SecurityTokenStatus.Disabled.name())
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true}
					""",
					JsonCompareMode.STRICT))
			;
		// @formatter:on
	}

	@Test
	public void updateStatus_asActorWithRestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withRefreshAllowed(false).build();
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), getJSONString(policy));

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/status")
				.queryParams(Map.of("tokenId", updateTokenId, "status", SecurityTokenStatus.Disabled.name()))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/user/auth-tokens/status")
				.param("tokenId", updateTokenId)
				.param("status", SecurityTokenStatus.Disabled.name())
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":false}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void delete_asActorWithUnrestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens")
				.queryParams(Map.of("tokenId", updateTokenId))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(delete("/api/v1/sec/user/auth-tokens")
				.param("tokenId", updateTokenId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true}
					""",
					JsonCompareMode.STRICT))
			;
		// @formatter:on
	}

	@Test
	public void delete_asActorWithRestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final SecurityPolicy policy = BasicSecurityPolicy.builder().withRefreshAllowed(false).build();
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), getJSONString(policy));

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), """
						{
							"nodeMetadataPaths": ["/pm/foo/**"]
						}
						""");

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens")
				.queryParams(Map.of("tokenId", updateTokenId))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(delete("/api/v1/sec/user/auth-tokens")
				.param("tokenId", updateTokenId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":false}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void listAll_asActorWithUnrestrictedPolicy() throws Exception {
		// GIVEN
		final String actorTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, actorTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final String otherTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, otherTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), null);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(actorTokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens")
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(get("/api/v1/sec/user/auth-tokens")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":[
						{"id":"%s"},
						{"id":"%s"}
					]}
					""".formatted(actorTokenId, otherTokenId),
					JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void listAll_asActorWithRestrictedPolicy() throws Exception {
		// GIVEN
		final String actorTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, actorTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder().withRefreshAllowed(false).build()));

		final String otherTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, otherTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), null);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(actorTokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens")
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		// only actor token ID returned when restricted
		mvc.perform(get("/api/v1/sec/user/auth-tokens")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":[
						{"id":"%s"}
					]}
					""".formatted(actorTokenId),
					JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void find_asActorWithUnrestrictedPolicy() throws Exception {
		// GIVEN
		final String actorTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, actorTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final String otherTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, otherTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), null);

		final String otherTokenId2 = randomString(20);
		insertSecurityToken(jdbcTemplate, otherTokenId2, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Disabled.name(), SecurityTokenType.ReadNodeData.name(), null);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(actorTokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/find")
				.queryParams(Map.of("active", "true"))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(get("/api/v1/sec/user/auth-tokens/find")
				.queryParam("active", "true")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data": {"results":[
						{"id":"%s"},
						{"id":"%s"}
					]}}
					""".formatted(actorTokenId, otherTokenId),
					JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void find_asActorWithRestrictedPolicy() throws Exception {
		// GIVEN
		final String actorTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, actorTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder().withRefreshAllowed(false).build()));

		final String otherTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, otherTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), null);

		final String otherTokenId2 = randomString(20);
		insertSecurityToken(jdbcTemplate, otherTokenId2, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Disabled.name(), SecurityTokenType.ReadNodeData.name(), null);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(actorTokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/find")
				.queryParams(Map.of("active", "true"))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(get("/api/v1/sec/user/auth-tokens/find")
				.queryParam("active", "true")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data": {"results":[
						{"id":"%s"}
					]}}
					""".formatted(actorTokenId),
					JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void find_asActorWithRestrictedPolicy_noMatch() throws Exception {
		// GIVEN
		final String actorTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, actorTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder().withRefreshAllowed(false).build()));

		final String otherTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, otherTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), null);

		final String otherTokenId2 = randomString(20);
		insertSecurityToken(jdbcTemplate, otherTokenId2, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Disabled.name(), SecurityTokenType.ReadNodeData.name(), null);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(actorTokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/find")
				.queryParams(Map.of("active", "false"))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(get("/api/v1/sec/user/auth-tokens/find")
				.queryParam("active", "false")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data": {"results":[
					]}}
					""".formatted(actorTokenId),
					JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void updateInfo_asActorWithUnestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), null);

		// WHEN
		final Instant now = Instant.now();
		final String newName = randomString();
		final String newDescription = randomString();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/info")
				.queryParams(Map.of("tokenId", updateTokenId, "name", newName, "description", newDescription))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/user/auth-tokens/info")
				.param("tokenId", updateTokenId)
				.param("name", newName)
				.param("description", newDescription)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void updateInfo_asActorWithRestrictedPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder().withRefreshAllowed(false).build()));

		final String updateTokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, updateTokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), null);

		// WHEN
		final Instant now = Instant.now();
		final String newName = randomString();
		final String newDescription = randomString();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/auth-tokens/info")
				.queryParams(Map.of("tokenId", updateTokenId, "name", newName, "description", newDescription))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/user/auth-tokens/info")
				.param("tokenId", updateTokenId)
				.param("name", newName)
				.param("description", newDescription)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":false}
					""", JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

}
