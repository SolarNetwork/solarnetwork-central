/* ==================================================================
 * NodesControllerWebTests.java - 15/12/2025 11:20:05 am
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

package net.solarnetwork.central.reg.web.test;

import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import net.solarnetwork.central.reg.web.UserAlertController;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.domain.NewNodeRequest;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.security.Snws2AuthorizationBuilder;
import net.solarnetwork.util.StringUtils;

/**
 * Test cases for the {@link UserAlertController} class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "NodesControllerWebTests.properties")
public class NodesControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private RegistrationBiz registrationBiz;

	private Long userId;

	@BeforeEach
	public void setup() {
		userId = randomLong();
		setupTestUser(userId);
		setupTestLocation();

	}

	private void createTestNode(Long nodeId) {
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);
	}

	private List<Long> setupTestNodes(final int count) {
		List<Long> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			Long nodeId = randomLong();
			createTestNode(nodeId);
			result.add(nodeId);
		}
		return result;
	}

	@Test
	public void listAllNodes_allReturnedWithUnrestrictedToken() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final int nodeCount = 3;
		final List<Long> nodeIds = setupTestNodes(nodeCount);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes")
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		String authHeader = auth.build();

		final StringBuilder expectedNodesJson = new StringBuilder();
		for (Long nodeId : nodeIds) {
			if (!expectedNodesJson.isEmpty()) {
				expectedNodesJson.append(',');
			}
			expectedNodesJson.append("""
					{"id":%d}
					""".formatted(nodeId));
		}

		mvc.perform(get("/api/v1/sec/nodes")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":{"results":[
						%s
					]}}
					""".formatted(expectedNodesJson), JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void listAllNodes_policyOverlapReturnedWithRestrictedToken() throws Exception {
		// GIVEN
		final int nodeCount = 3;
		final List<Long> nodeIds = setupTestNodes(nodeCount);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.get(1), nodeIds.get(2))).build()));

		// WHEN
		final Instant now = Instant.now();

		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes")
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		String authHeader = auth.build();

		mvc.perform(get("/api/v1/sec/nodes")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":{"results":[
						{"id":%d},
						{"id":%d}
					]}}
					""".formatted(nodeIds.get(1), nodeIds.get(2)), JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void listArchivedNodes_allReturnedWithUnrestrictedToken() throws Exception {
		// GIVEN
		final int nodeCount = 3;
		final List<Long> nodeIds = setupTestNodes(nodeCount);

		// change all nodes to archived
		jdbcTemplate.update("update solaruser.user_node SET archived = true");

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		// WHEN
		final Instant now = Instant.now();

		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/archived")
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final StringBuilder expectedNodesJson = new StringBuilder();
		for (Long nodeId : nodeIds) {
			if (!expectedNodesJson.isEmpty()) {
				expectedNodesJson.append(',');
			}
			expectedNodesJson.append("""
					{"id":%d}
					""".formatted(nodeId));
		}

		mvc.perform(get("/api/v1/sec/nodes/archived")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":[
						%s
					]}
					""".formatted(expectedNodesJson), JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void listArchivedNodes_policyOverlapReturnedWithRestrictedToken() throws Exception {
		// GIVEN
		final int nodeCount = 3;
		final List<Long> nodeIds = setupTestNodes(nodeCount);

		// change all nodes to archived
		jdbcTemplate.update("update solaruser.user_node SET archived = true");

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.get(1), nodeIds.get(2))).build()));

		// WHEN
		final Instant now = Instant.now();

		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/archived")
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		mvc.perform(get("/api/v1/sec/nodes/archived")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":[
						{"id":%d},
						{"id":%d}
					]}
					""".formatted(nodeIds.get(1), nodeIds.get(2)), JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void createNode_allowedWithUnrestrictedToken() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		// WHEN
		final Instant now = Instant.now();
		final String tz = "Pacific/Auckland";
		final String country = "NZ";
		final String certPassword = randomString();

		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/create-cert")
				.queryParams(Map.of("timeZone", tz, "country", country, "keystorePassword", certPassword))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/nodes/create-cert")
				.param("timeZone", tz)
				.param("country", country)
				.param("keystorePassword", certPassword)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data": {"user": {
						"id":%d
					}, "node": {
						"timeZone": "%s"
					}, "certificate": {
						"status": "v"
					}, "nodeLocation": {
						"country": "%s"
					}}}
					""".formatted(userId, tz, country), JsonCompareMode.LENIENT))
			;
		// @formatter:on
	}

	@Test
	public void createNode_deniedWithRestrictedToken() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder().withRefreshAllowed(false).build()));

		// WHEN
		final Instant now = Instant.now();
		final String tz = "Pacific/Auckland";
		final String country = "NZ";
		final String certPassword = randomString();

		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/create-cert")
				.queryParams(Map.of("timeZone", tz, "country", country, "keystorePassword", certPassword))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/nodes/create-cert")
				.param("timeZone", tz)
				.param("country", country)
				.param("keystorePassword", certPassword)
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
	public void renewNodeCertificate_onlyWithUnrestrictedToken() throws Exception {
		// GIVEN
		final String tz = "Pacific/Auckland";
		final String country = "NZ";
		final String certPassword = randomString();

		final int nodeCount = 2;
		final List<UserNode> nodes = new ArrayList<>(nodeCount);
		for ( int i = 0; i < nodeCount; i++ ) {
			NewNodeRequest req = new NewNodeRequest(userId, certPassword, TimeZone.getTimeZone(tz),
					Locale.forLanguageTag("en-" + country));
			UserNode node = registrationBiz.createNodeManually(req);
			nodes.add(node);
		}

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final String restrictedTokenId = randomString(20);
		final String restrictedTokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, restrictedTokenId, restrictedTokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodes.get(0).getNode().getId())).build()));

		final String restrictedTokenId2 = randomString(20);
		final String restrictedTokenSecret2 = randomString();
		insertSecurityToken(jdbcTemplate, restrictedTokenId2, restrictedTokenSecret2, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodes.get(1).getNode().getId())).build()));

		// WHEN
		final Instant now = Instant.now();
		final Long nodeIdToRenew = nodes.get(0).getNode().getId();

		// @formatter:off

		// first with unrestricted token: ALLOWED

		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/cert/renew/" +nodeIdToRenew)
				.queryParams(Map.of("password", certPassword))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/nodes/cert/renew/" +nodeIdToRenew)
				.param("password", certPassword)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{
						"userId": %d,
						"nodeId": %d
					}
					""".formatted(userId, nodeIdToRenew), JsonCompareMode.LENIENT))
			;

		// try with restricted token: ALLOWED

		final Snws2AuthorizationBuilder auth2 = new Snws2AuthorizationBuilder(restrictedTokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/cert/renew/" +nodeIdToRenew)
				.queryParams(Map.of("password", certPassword))
				.useSnDate(true).date(now)
				.saveSigningKey(restrictedTokenSecret);
		final String authHeader2 = auth2.build();

		mvc.perform(post("/api/v1/sec/nodes/cert/renew/" +nodeIdToRenew)
				.param("password", certPassword)
				.header(HttpHeaders.AUTHORIZATION, authHeader2)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{
						"userId": %d,
						"nodeId": %d
					}
					""".formatted(userId, nodeIdToRenew), JsonCompareMode.LENIENT))
				;

		// try with restricted token for other node : DEINED

		final Snws2AuthorizationBuilder auth3 = new Snws2AuthorizationBuilder(restrictedTokenId2)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/cert/renew/" +nodeIdToRenew)
				.queryParams(Map.of("password", certPassword))
				.useSnDate(true).date(now)
				.saveSigningKey(restrictedTokenSecret2);
		final String authHeader3 = auth3.build();

		mvc.perform(post("/api/v1/sec/nodes/cert/renew/" +nodeIdToRenew)
				.param("password", certPassword)
				.header(HttpHeaders.AUTHORIZATION, authHeader3)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success": false}
					""", JsonCompareMode.LENIENT))
			;

		// @formatter:on
	}

	@Test
	public void updateArchivedStatus() throws Exception {
		// GIVEN
		final int nodeCount = 3;
		final List<Long> nodeIds = setupTestNodes(nodeCount);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final String restrictedTokenId = randomString(20);
		final String restrictedTokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, restrictedTokenId, restrictedTokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.get(0), nodeIds.get(2))).build()));

		// WHEN
		final Instant now = Instant.now();

		// unrestricted: allow all

		// @formatter:off
		Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/archived")
				.queryParams(Map.of(
						"nodeIds", StringUtils.commaDelimitedStringFromCollection(nodeIds),
						"archived", "true"))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		String authHeader = auth.build();

		mvc.perform(post("/api/v1/sec/nodes/archived")
				.param("nodeIds", StringUtils.commaDelimitedStringFromCollection(nodeIds))
				.param("archived", "true")
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

		// restricted: allow policy IDs

		// @formatter:off
		Snws2AuthorizationBuilder auth2 = new Snws2AuthorizationBuilder(restrictedTokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/archived")
				.queryParams(Map.of(
						"nodeIds", StringUtils.commaDelimitedStringFromCollection(
								List.of(nodeIds.get(0), nodeIds.get(2))),
						"archived", "false"))
				.useSnDate(true).date(now)
				.saveSigningKey(restrictedTokenSecret);
		String authHeader2 = auth2.build();

		mvc.perform(post("/api/v1/sec/nodes/archived")
				.param("nodeIds", StringUtils.commaDelimitedStringFromCollection(
						List.of(nodeIds.get(0), nodeIds.get(2))))
				.param("archived", "false")
				.header(HttpHeaders.AUTHORIZATION, authHeader2)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true}
					""", JsonCompareMode.LENIENT))
			;

		// restricted: deny outside policy IDs

		// @formatter:off
		Snws2AuthorizationBuilder auth3 = new Snws2AuthorizationBuilder(restrictedTokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/archived")
				.queryParams(Map.of(
						"nodeIds", StringUtils.commaDelimitedStringFromCollection(nodeIds),
						"archived", "true"))
				.useSnDate(true).date(now)
				.saveSigningKey(restrictedTokenSecret);
		String authHeader3 = auth3.build();

		mvc.perform(post("/api/v1/sec/nodes/archived")
				.param("nodeIds", StringUtils.commaDelimitedStringFromCollection(nodeIds))
				.param("archived", "true")
				.header(HttpHeaders.AUTHORIZATION, authHeader3)
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
