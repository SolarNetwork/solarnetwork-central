/* ==================================================================
 * NodeInstructionControllerTests.java - 31/05/2025 2:23:16 pm
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

import static java.util.Map.entry;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.reg.test.WithMockSecurityUser;
import net.solarnetwork.central.reg.web.api.v1.NodeInstructionController;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the {@link NodeInstructionController}
 * class.
 *
 * @author matt
 * @version 1.1
 */
@SpringBootTest
@AutoConfigureMockMvc
public class NodeInstructionControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final Long TEST_USER_ID = 1L;
	private static final String TEST_EMAIL = "test1@localhost";
	private static final String TEST_TOKEN_SECRET = randomString();

	private static final Clock clock = Clock.tickMillis(ZoneOffset.UTC);

	@Autowired
	private NodeInstructionDao nodeInstructionDao;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MockMvc mvc;

	private Long nodeId;

	@BeforeEach
	public void setup() {
		setupTestUser(TEST_USER_ID, TEST_EMAIL);
		nodeId = randomLong();
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(TEST_USER_ID, nodeId);
	}

	private Long setupTestUserNode(final Long userId) {
		final Long nodeId = randomLong();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);
		return nodeId;
	}

	private UserLongCompositePK setupTestUserAndNode() {
		final Long userId = randomLong();
		setupTestUser(userId, userId + "@localhost");
		final Long nodeId = setupTestUserNode(userId);
		return new UserLongCompositePK(userId, nodeId);
	}

	@Test
	@WithMockSecurityUser
	public void viewInstruction() throws Exception {
		// GIVEN
		NodeInstruction ni = new NodeInstruction(randomString(), clock.instant(), nodeId);
		ni.setCreated(ni.getInstruction().getInstructionDate());
		ni.getInstruction().setParams(Map.of("a", "one"));
		ni = nodeInstructionDao.get(nodeInstructionDao.save(ni));

		// WHEN
		// @formatter:off
		mvc.perform(get("/api/v1/sec/instr/view")
				.param("id", ni.getId().toString())
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"success":true, "data":%s}
					""".formatted(objectMapper.writeValueAsString(ni)), JsonCompareMode.STRICT))
			;
		// @formatter:on
	}

	@Test
	public void view_asUnrestrictedToken_allowed_nodeIdInAccount() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		NodeInstruction ni = new NodeInstruction(randomString(), clock.instant(), nodeId);
		ni.setCreated(ni.getInstruction().getInstructionDate());
		ni.getInstruction().setParams(Map.of("a", "one"));
		ni = nodeInstructionDao.get(nodeInstructionDao.save(ni));

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/instr/view")
				.queryParams(Map.of("id", ni.getId().toString()))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/instr/view")
				.param("id", ni.getId().toString())
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("Data is entity object")
			.isObject()
			.as("ID assigned")
			.as("Instruction returned for given ID")
			.contains(
				entry("id", ni.getId())
			)
			;
		// @formatter:on
	}

	@Test
	public void view_asUnrestrictedToken_deined_nodeIdNotInAccount() throws Exception {
		// GIVEN
		final UserLongCompositePK otherUserNode = setupTestUserAndNode();

		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		NodeInstruction ni = new NodeInstruction(randomString(), clock.instant(),
				otherUserNode.getEntityId());
		ni.setCreated(ni.getInstruction().getInstructionDate());
		ni.getInstruction().setParams(Map.of("a", "one"));
		ni = nodeInstructionDao.get(nodeInstructionDao.save(ni));

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/instr/view")
				.queryParams(Map.of("id", ni.getId().toString()))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/instr/view")
				.param("id", ni.getId().toString())
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data on forbidden response")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void view_asRestrictedToken_allowed_nodeIdInPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeId)).build()));

		NodeInstruction ni = new NodeInstruction(randomString(), clock.instant(), nodeId);
		ni.setCreated(ni.getInstruction().getInstructionDate());
		ni.getInstruction().setParams(Map.of("a", "one"));
		ni = nodeInstructionDao.get(nodeInstructionDao.save(ni));

		final Long nodeId2 = randomLong();
		setupTestNode(nodeId2);
		setupTestUserNode(TEST_USER_ID, nodeId2);

		NodeInstruction ni2 = new NodeInstruction(randomString(), clock.instant(), nodeId2);
		ni2.setCreated(ni2.getInstruction().getInstructionDate());
		ni2.getInstruction().setParams(Map.of("a", "one"));
		ni2 = nodeInstructionDao.get(nodeInstructionDao.save(ni2));

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/instr/view")
				.queryParams(Map.of("id", ni.getId().toString()))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/instr/view")
				.param("id", ni.getId().toString())
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("Data is entity object")
			.isObject()
			.as("Instruction returned for given ID")
			.contains(
				entry("id", ni.getId())
			)
			;
	}

	@Test
	public void view_asRestrictedToken_deined_nodeIdNotInPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeId)).build()));

		NodeInstruction ni = new NodeInstruction(randomString(), clock.instant(), nodeId);
		ni.setCreated(ni.getInstruction().getInstructionDate());
		ni.getInstruction().setParams(Map.of("a", "one"));
		ni = nodeInstructionDao.get(nodeInstructionDao.save(ni));

		final Long nodeId2 = setupTestUserNode(TEST_USER_ID);

		NodeInstruction ni2 = new NodeInstruction(randomString(), clock.instant(), nodeId2);
		ni2.setCreated(ni2.getInstruction().getInstructionDate());
		ni2.getInstruction().setParams(Map.of("a", "one"));
		ni2 = nodeInstructionDao.get(nodeInstructionDao.save(ni2));

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/instr/view")
				.queryParams(Map.of("id", ni2.getId().toString()))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/instr/view")
				.param("id", ni2.getId().toString())
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data on forbidden response")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	@WithMockSecurityUser
	public void addInstruction() throws Exception {
		// GIVEN
		NodeInstruction ni = new NodeInstruction(randomString(), clock.instant(), nodeId);
		ni.setCreated(ni.getInstruction().getInstructionDate());
		ni.getInstruction().setParams(Map.of("a", "one"));
		ni = nodeInstructionDao.get(nodeInstructionDao.save(ni));

		// WHEN
		// @formatter:off
		mvc.perform(post("/api/v1/sec/instr/add/Test")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nodeId":%d,"params":{"a":"one"}}
						""".formatted(nodeId))
				.accept(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.success", is(true)))
			.andExpect(jsonPath("$.data.id", is(notNullValue())))
			.andExpect(jsonPath("$.data.nodeId", is(nodeId)))
			.andExpect(jsonPath("$.data.topic", is("Test")))
			.andExpect(jsonPath("$.data.parameters.length()", is(1)))
			.andExpect(jsonPath("$.data.parameters[0].name", is("a")))
			.andExpect(jsonPath("$.data.parameters[0].value", is("one")))
			;
		// @formatter:on
	}

	@Test
	public void add_asUnrestrictedToken_allowed_nodeIdInAccount() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		// WHEN
		final Instant now = Instant.now();
		final String reqJson = """
				{"nodeId":%d,"params":{"a":"one"}}
				""".formatted(nodeId);

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/instr/add/Test")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/instr/add/Test")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8")
				.content(reqJson)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("Data is entity object")
			.isObject()
			.as("ID assigned")
			.containsKey("id")
			.contains(
				entry("nodeId", nodeId),
				entry("topic", "Test")
			)
			.node("parameters")
			.as("Parameters encoded as array of objects")
			.isArray()
			.as("One parameter encoded")
			.hasSize(1)
			.element(0)
			.as("Parameter is object")
			.isObject()
			.as("Parameter values from input")
			.containsExactly(
				entry("name", "a"),
				entry("value", "one")
			)
			;
		// @formatter:on
	}

	@Test
	public void add_asUnrestrictedToken_deined_nodeIdNodeInAccount() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(), null);

		final UserLongCompositePK otherUserNode = setupTestUserAndNode();

		// WHEN
		final Instant now = Instant.now();
		final String reqJson = """
				{"nodeId":%d,"params":{"a":"one"}}
				""".formatted(otherUserNode.getEntityId());

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/instr/add/Test")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/instr/add/Test")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8")
				.content(reqJson)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data on forbidden response")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void add_restrictedToken_allowed_nodeIdInPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeId)).build()));

		// WHEN
		final Instant now = Instant.now();
		final String reqJson = """
				{"nodeId":%d,"params":{"a":"one"}}
				""".formatted(nodeId);

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/instr/add/Test")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/instr/add/Test")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8")
				.content(reqJson)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("Data is entity object")
			.isObject()
			.as("ID assigned")
			.containsKey("id")
			.contains(
				entry("nodeId", nodeId),
				entry("topic", "Test")
			)
			.node("parameters")
			.as("Parameters encoded as array of objects")
			.isArray()
			.as("One parameter encoded")
			.hasSize(1)
			.element(0)
			.as("Parameter is object")
			.isObject()
			.as("Parameter values from input")
			.containsExactly(
				entry("name", "a"),
				entry("value", "one")
			)
			;
		// @formatter:on
	}

	@Test
	public void add_restrictedToken_deined_nodeIdNotInPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		insertSecurityToken(jdbcTemplate, tokenId, TEST_TOKEN_SECRET, TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.User.name(),
				getJSONString(BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeId)).build()));

		final Long otherNodeId = setupTestUserNode(TEST_USER_ID);

		// WHEN
		final Instant now = Instant.now();
		final String reqJson = """
				{"nodeId":%d,"params":{"a":"one"}}
				""".formatted(otherNodeId);

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/instr/add/Test")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(TEST_TOKEN_SECRET);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/instr/add/Test")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON + ";charset=UTF-8")
				.content(reqJson)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data on forbidden response")
			.isAbsent()
			;
		// @formatter:on
	}

}
