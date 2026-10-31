/* ==================================================================
 * DatumStreamAliasControllerWebTests.java - 2/04/2026 5:45:10 am
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

import static java.util.Map.entry;
import static java.util.UUID.randomUUID;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.security.SecurityTokenStatus.Active;
import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityTokenWithPolicy;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.reg.web.api.v1.DatumStreamAliasController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.security.Snws2AuthorizationBuilder;
import tools.jackson.databind.ObjectMapper;

/**
 * Web API level integration tests for the {@link DatumStreamAliasController}
 * class.
 *
 * @author matt
 * @version 1.3
 */
@SpringBootTest
@AutoConfigureMockMvc

public class DatumStreamAliasControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final String ALIAS_PATH = "/api/v1/sec/datum/stream/alias";
	private static final String GRANTS_PATH = ALIAS_PATH + "/grants";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectDatumStreamAliasEntityDao aliasDao;

	@Autowired
	private ObjectMapper objectMapper;

	private Long userId;

	@BeforeEach
	public void setup() {
		userId = randomLong();
		setupTestUser(userId, userId + "@localhost");
		setupTestLocation();
	}

	private List<Long> createUserNodes(final int count) {
		return createUserNodes(userId, count);
	}

	private List<Long> createUserNodes(final Long userId, final int count) {
		return createUserNodes(userId, count, true);
	}

	private List<Long> createUserNodes(final Long userId, final int count, final boolean requiresAuth) {
		List<Long> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			Long nodeId = randomLong();
			setupTestNode(nodeId);
			insertUserNode(jdbcTemplate, userId, nodeId, requiresAuth);
			result.add(nodeId);
		}
		return result;
	}

	private ResultActions saveAlias(final HttpMethod method, final String path, final String tokenId,
			final String tokenSecret, final ObjectDatumStreamAliasEntityInput input) throws Exception {
		final String reqJson = objectMapper.writeValueAsString(input);
		final Instant now = Instant.now();
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(method.name())
				.host("localhost")
				.path(path)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		return mvc.perform(
				request(method, path)
				.header(HttpHeaders.AUTHORIZATION, auth.build())
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			);
		// @formatter:on
	}

	private record Token(String id, String secret) {

	}

	private Token createToken(final Long userId) {
		final var token = new Token(randomString(20), randomString());
		insertSecurityToken(jdbcTemplate, token.id(), token.secret(), userId, Active, User, null);
		return token;
	}

	private ResultActions api(final Token token, final HttpMethod method, final String path,
			final @Nullable Object body) throws Exception {
		final Instant now = Instant.now();
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(token.id())
				.method(method.name())
				.host("localhost")
				.path(path)
				.useSnDate(true).date(now);
		final MockHttpServletRequestBuilder req = request(method, path)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON);
		if ( body != null ) {
			final String reqJson = objectMapper.writeValueAsString(body);
			auth.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson));
			req.content(reqJson).contentType(MediaType.APPLICATION_JSON);
		}
		return mvc.perform(req.header(HttpHeaders.AUTHORIZATION,
				auth.saveSigningKey(token.secret()).build()));
		// @formatter:on
	}

	private ResultActions apiGet(final Token token, final String path, final Map<String, String> params)
			throws Exception {
		final Instant now = Instant.now();
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(token.id())
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path(path)
				.queryParams(params)
				.useSnDate(true).date(now)
				.saveSigningKey(token.secret());
		final MockHttpServletRequestBuilder req = request(HttpMethod.GET, path)
				.header(HttpHeaders.AUTHORIZATION, auth.build())
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON);
		// @formatter:on
		params.forEach(req::param);
		return mvc.perform(req);
	}

	private Long createGrant(final Token token, final Map<String, Object> body) throws Exception {
		final String json = api(token, HttpMethod.POST, GRANTS_PATH, body)
				.andExpect(status().isCreated()).andExpect(header().exists(HttpHeaders.LOCATION))
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(json).path("data").path("grantId").asLong();
	}

	private static Map<String, Object> grantBody(final Long granteeId, final Long nodeId) {
		return Map.of("granteeId", granteeId, "nodeIds", List.of(nodeId), "enabled", true);
	}

	private static Map<String, Object> aliasBody(final Long originalNodeId,
			final String originalSourceId, final Long aliasNodeId) {
		return Map.of("originalObjectId", originalNodeId, "originalSourceId", originalSourceId,
				"objectId", aliasNodeId, "sourceId", randomSourceId());
	}

	private int grantCount(final Long userId) {
		return nonnull(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM solaruser.da_datm_alias_grant WHERE user_id = ?", Integer.class,
				userId), "Count");
	}

	private int aliasCountForAliasNode(Long nodeId) {
		return nonnull(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM solardatm.da_datm_alias WHERE alias_node_id = ?", Integer.class,
				nodeId), "Count");
	}

	@Test
	public void createAlias_asUnrestrictedToken() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final List<Long> nodeIds = createUserNodes(1);
		final Long reqNodeId = nodeIds.getFirst();

		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, reqNodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(meta.getObjectId());
		input.setOriginalSourceId(meta.getSourceId());
		input.setObjectId(reqNodeId);
		input.setSourceId(randomSourceId());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/datum/stream/alias")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/datum/stream/alias")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isCreated())
			.andExpect(header().exists(HttpHeaders.LOCATION))
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
			.isObject()
			.as("Stream ID, created, modified generated")
			.containsKeys("streamId", "created", "modified")
			.as("Alias details returned")
			.contains(
				entry("originalObjectId", input.getOriginalObjectId()),
				entry("originalSourceId", input.getOriginalSourceId()),
				entry("objectId", input.getObjectId()),
				entry("sourceId", input.getSourceId())
			)
			;
		// @formatter:on
	}

	@Test
	public void createAlias_asUnrestrictedToken_originalMetaDoesNotExist() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final List<Long> nodeIds = createUserNodes(1);
		final Long reqNodeId = nodeIds.getFirst();

		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(reqNodeId);
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(reqNodeId);
		input.setSourceId(randomSourceId());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/datum/stream/alias")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/datum/stream/alias")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Failure result")
			.containsEntry("success", false)
			.as("Foreign key violation because original node/source meta does not exist")
			.containsEntry("code", "DAO.00105")
			.as("No data provided")
			.doesNotContainKey("data")
			;
		// @formatter:on
	}

	@Test
	public void createAlias_asUnrestrictedToken_aliasAlreadyExists() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final List<Long> nodeIds = createUserNodes(2);
		final Long origNodeId = nodeIds.getFirst();
		final Long aliasNodeId = nodeIds.getLast();

		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, origNodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final var alias = new ObjectDatumStreamAliasEntity(randomUUID(), now, now, Node, aliasNodeId,
				randomSourceId(), meta.getObjectId(), meta.getSourceId());
		aliasDao.save(alias);

		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(meta.getObjectId());
		input.setOriginalSourceId(meta.getSourceId());
		input.setObjectId(alias.getObjectId());
		input.setSourceId(alias.getSourceId());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/datum/stream/alias")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/datum/stream/alias")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Failure result")
			.containsEntry("success", false)
			.as("Duplicate key violation because alias node/source already exists")
			.containsEntry("code", "DAO.00101")
			.as("No data provided")
			.doesNotContainKey("data")
			;
		// @formatter:on
	}

	@Test
	public void createAlias_asUnrestrictedToken_originalNodeDoesNotExist() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(input.getOriginalObjectId());
		input.setSourceId(randomSourceId());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/datum/stream/alias")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/datum/stream/alias")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
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
			.as("Unknown object message because original node ID does not exist")
			.containsEntry("message", "UNKNOWN_OBJECT")
			.as("No data provided")
			.doesNotContainKey("data")
			;
		// @formatter:on
	}

	@Test
	public void createAlias_asUnrestrictedToken_aliasNodeDoesNotExist() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final List<Long> nodeIds = createUserNodes(1);
		final Long reqNodeId = nodeIds.getFirst();

		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(reqNodeId);
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/datum/stream/alias")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/datum/stream/alias")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
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
			.as("Unknown object message because alias node ID does not exist")
			.containsEntry("message", "UNKNOWN_OBJECT")
			.as("No data provided")
			.doesNotContainKey("data")
			;
		// @formatter:on
	}

	@Test
	public void createAlias_asUnrestrictedToken_originalNodeOwnedByAnotherUser() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");

		final List<Long> nodeIds = createUserNodes(userId2, 1);
		final Long reqNodeId = nodeIds.getFirst();

		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(reqNodeId);
		input.setOriginalSourceId(randomSourceId());
		input.setObjectId(reqNodeId);
		input.setSourceId(randomSourceId());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/datum/stream/alias")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/datum/stream/alias")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
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
			.as("Unknown object message")
			.containsEntry("message", "ACCESS_DENIED")
			.as("No data provided")
			.doesNotContainKey("data")
			;
		// @formatter:on
	}

	@Test
	public void createAlias_asUnrestrictedToken_aliasNodeOwnedByAnotherUser() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final List<Long> nodeIds = createUserNodes(1);
		final Long origNodeId = nodeIds.getFirst();

		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, origNodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");

		final List<Long> user2NodeIds = createUserNodes(userId2, 1);
		final Long aliasNodeId = user2NodeIds.getFirst();

		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(meta.getObjectId());
		input.setOriginalSourceId(meta.getSourceId());
		input.setObjectId(aliasNodeId);
		input.setSourceId(randomSourceId());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/datum/stream/alias")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/datum/stream/alias")
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
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
			.as("Unknown object message")
			.containsEntry("message", "ACCESS_DENIED")
			.as("No data provided")
			.doesNotContainKey("data")
			;
		// @formatter:on
	}

	@Test
	public void createAlias_asUnrestrictedToken_originalNodeOwnedByAnotherUser_public()
			throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Long aliasNodeId = createUserNodes(1).getFirst();

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");
		final Long origNodeId = createUserNodes(userId2, 1, false).getFirst();

		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, origNodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(meta.getObjectId());
		input.setOriginalSourceId(meta.getSourceId());
		input.setObjectId(aliasNodeId);
		input.setSourceId(randomSourceId());

		// WHEN
		// @formatter:off
		saveAlias(HttpMethod.POST, "/api/v1/sec/datum/stream/alias", tokenId, tokenSecret, input)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(aliasCountForAliasNode(aliasNodeId))
			.as("Alias of another user's public node stream not created")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void createAlias_asUnrestrictedToken_aliasNodeOwnedByAnotherUser_public() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Long origNodeId = createUserNodes(1).getFirst();

		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, origNodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");
		final Long aliasNodeId = createUserNodes(userId2, 1, false).getFirst();

		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(meta.getObjectId());
		input.setOriginalSourceId(meta.getSourceId());
		input.setObjectId(aliasNodeId);
		input.setSourceId(randomSourceId());

		// WHEN
		// @formatter:off
		saveAlias(HttpMethod.POST, "/api/v1/sec/datum/stream/alias", tokenId, tokenSecret, input)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(aliasCountForAliasNode(aliasNodeId))
			.as("Alias on another user's public node not created")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void updateAlias_asUnrestrictedToken_aliasOwnedByAnotherUser() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final List<Long> nodeIds = createUserNodes(2);
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, nodeIds.getFirst(), randomSourceId());

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");
		final List<Long> user2NodeIds = createUserNodes(userId2, 2);
		final var meta2 = emptyMeta(randomUUID(), TEST_TZ, Node, user2NodeIds.getFirst(),
				randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta, meta2));

		final var alias2 = new ObjectDatumStreamAliasEntity(randomUUID(), now, now, Node,
				user2NodeIds.getLast(), randomSourceId(), meta2.getObjectId(), meta2.getSourceId());
		aliasDao.save(alias2);

		// update another user's alias to use this user's streams
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(meta.getObjectId());
		input.setOriginalSourceId(meta.getSourceId());
		input.setObjectId(nodeIds.getLast());
		input.setSourceId(randomSourceId());

		// WHEN
		// @formatter:off
		final String path = "/api/v1/sec/datum/stream/alias/" + alias2.getStreamId();
		saveAlias(HttpMethod.PUT, path, tokenId, tokenSecret, input)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(aliasDao.get(alias2.getStreamId()))
			.as("Other user alias unchanged")
			.isNotNull()
			.returns(alias2.getOriginalObjectId(), from(ObjectDatumStreamAliasEntity::getOriginalObjectId))
			.returns(alias2.getOriginalSourceId(), from(ObjectDatumStreamAliasEntity::getOriginalSourceId))
			.returns(alias2.getObjectId(), from(ObjectDatumStreamAliasEntity::getObjectId))
			.returns(alias2.getSourceId(), from(ObjectDatumStreamAliasEntity::getSourceId))
			;
		// @formatter:on
	}

	@Test
	public void deleteAliases_asUnrestrictedToken_otherUserAliasesKept() throws Exception {
		// GIVEN
		final Instant now = Instant.now();
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final List<Long> nodeIds = createUserNodes(1);
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, nodeIds.getFirst(), randomSourceId());

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");
		final List<Long> user2NodeIds = createUserNodes(userId2, 1);
		final var meta2 = emptyMeta(randomUUID(), TEST_TZ, Node, user2NodeIds.getFirst(),
				randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta, meta2));

		final var alias = new ObjectDatumStreamAliasEntity(randomUUID(), now, now, Node,
				nodeIds.getFirst(), randomSourceId(), meta.getObjectId(), meta.getSourceId());
		aliasDao.save(alias);
		final var alias2 = new ObjectDatumStreamAliasEntity(randomUUID(), now, now, Node,
				user2NodeIds.getFirst(), randomSourceId(), meta2.getObjectId(), meta2.getSourceId());
		aliasDao.save(alias2);

		// WHEN
		// @formatter:off
		final String path = "/api/v1/sec/datum/stream/alias";
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path(path)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		mvc.perform(delete(path)
				.header(HttpHeaders.AUTHORIZATION, auth.build())
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			;

		// THEN
		then(aliasDao.get(alias.getStreamId()))
			.as("User's alias deleted")
			.isNull()
			;
		then(aliasDao.get(alias2.getStreamId()))
			.as("Other user's alias not deleted")
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void createAlias_otherAccount_followsGrant() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long ownerId = randomLong();
		setupTestUser(ownerId, ownerId + "@localhost");
		final Token ownerToken = createToken(ownerId);

		final Long hostNodeId = createUserNodes(1).getFirst();
		final Long ownerNodeId = createUserNodes(ownerId, 1).getFirst();
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final var alias = aliasBody(ownerNodeId, meta.getSourceId(), hostNodeId);

		// WHEN
		// @formatter:off
		// no grant
		api(token, HttpMethod.POST, ALIAS_PATH, alias)
			.andExpect(status().isForbidden())
			;

		final Long grantId = createGrant(ownerToken, grantBody(userId, ownerNodeId));

		api(ownerToken, HttpMethod.POST, GRANTS_PATH + "/" + grantId + "/enabled/false", null)
			.andExpect(status().isOk())
			;
		// disabled grant
		api(token, HttpMethod.POST, ALIAS_PATH, alias)
			.andExpect(status().isForbidden())
			;
		then(aliasCountForAliasNode(hostNodeId))
			.as("Alias not created without an enabled grant")
			.isZero()
			;

		api(ownerToken, HttpMethod.POST, GRANTS_PATH + "/" + grantId + "/enabled/true", null)
			.andExpect(status().isOk())
			;
		api(token, HttpMethod.POST, ALIAS_PATH, alias)
			.andExpect(status().isCreated())
			;

		// THEN
		then(aliasCountForAliasNode(hostNodeId))
			.as("Alias created with enabled grant")
			.isOne()
			;
		// @formatter:on
	}

	@Test
	public void createAlias_otherAccount_publicAliasNode() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long ownerId = randomLong();
		setupTestUser(ownerId, ownerId + "@localhost");
		final Token ownerToken = createToken(ownerId);

		final Long hostNodeId = createUserNodes(userId, 1, false).getFirst();
		final Long ownerNodeId = createUserNodes(ownerId, 1).getFirst();
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		createGrant(ownerToken, grantBody(userId, ownerNodeId));

		// WHEN
		api(token, HttpMethod.POST, ALIAS_PATH, aliasBody(ownerNodeId, meta.getSourceId(), hostNodeId))
				.andExpect(status().isForbidden());

		// THEN
		// @formatter:off
		then(aliasCountForAliasNode(hostNodeId))
			.as("A public node would expose the other account's stream to everyone")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void createAlias_otherAccount_publicAliasNode_allowPublic() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long ownerId = randomLong();
		setupTestUser(ownerId, ownerId + "@localhost");
		final Token ownerToken = createToken(ownerId);

		final Long hostNodeId = createUserNodes(userId, 1, false).getFirst();
		final Long ownerNodeId = createUserNodes(ownerId, 1).getFirst();
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		createGrant(ownerToken, Map.of("granteeId", userId, "nodeIds", List.of(ownerNodeId), "enabled",
				true, "allowPublic", true));

		// WHEN
		api(token, HttpMethod.POST, ALIAS_PATH, aliasBody(ownerNodeId, meta.getSourceId(), hostNodeId))
				.andExpect(status().isCreated());

		// THEN
		// @formatter:off
		then(aliasCountForAliasNode(hostNodeId))
			.as("Stream owner agreed to public alias")
			.isEqualTo(1)
			;
		// @formatter:on
	}

	@Test
	public void grant_allowPublic_defaultsOff() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long ownerId = randomLong();
		setupTestUser(ownerId, ownerId + "@localhost");
		final Token ownerToken = createToken(ownerId);
		final Long ownerNodeId = createUserNodes(ownerId, 1).getFirst();

		createGrant(ownerToken, grantBody(userId, ownerNodeId));
		createGrant(ownerToken, Map.of("granteeId", userId, "sourceIds", List.of("/a/**"), "enabled",
				true, "allowPublic", true));

		// WHEN
		// @formatter:off
		final String json = api(token, HttpMethod.GET, GRANTS_PATH + "/received", null)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(json)
			.asInstanceOf(JSON)
			.node("data.results[0].allowPublic")
			.as("Off when not provided")
			.isEqualTo(false)
			;
		then(json)
			.asInstanceOf(JSON)
			.node("data.results[1].allowPublic")
			.as("Grantee can see grant allows public")
			.isEqualTo(true)
			;
		// @formatter:on
	}

	@Test
	public void createGrant_otherUserNode() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");
		final Long otherNodeId = createUserNodes(userId2, 1).getFirst();

		// WHEN
		api(token, HttpMethod.POST, GRANTS_PATH, grantBody(userId2, otherNodeId))
				.andExpect(status().isForbidden());

		// THEN
		// @formatter:off
		then(grantCount(userId))
			.as("A grant cannot name a node the user does not own")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void createGrant_noNodesOrSources() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");

		// WHEN
		api(token, HttpMethod.POST, GRANTS_PATH, Map.of("granteeId", userId2, "enabled", true))
				.andExpect(status().is4xxClientError());

		// THEN
		// @formatter:off
		then(grantCount(userId))
			.as("A grant of everything is rejected")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void createGrant_blankSource() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");

		// WHEN
		api(token, HttpMethod.POST, GRANTS_PATH,
				Map.of("granteeId", userId2, "sourceIds", List.of(" "), "enabled", true))
						.andExpect(status().is4xxClientError());

		// THEN
		// @formatter:off
		then(grantCount(userId))
			.as("Source IDs are validated individually")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void createGrant_sourcesOnly() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");

		final String sourceId = "/a/**";

		// WHEN
		final Long grantId = createGrant(token,
				Map.of("granteeId", userId2, "sourceIds", List.of(sourceId), "enabled", true));

		// THEN
		// @formatter:off
		final String json = api(token, HttpMethod.GET, GRANTS_PATH + "/" + grantId, null)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;
		then(json)
			.asInstanceOf(JSON)
			.node("data")
			.isObject()
			.as("Grant details returned")
			.containsEntry("granteeId", userId2)
			.containsEntry("enabled", true)
			.containsEntry("sourceIds", List.of(sourceId))
			;
		// @formatter:on
	}

	@Test
	public void createGrant_restrictedToken() throws Exception {
		// GIVEN
		final Long nodeId = createUserNodes(1).getFirst();
		final var token = new Token(randomString(20), randomString());
		insertSecurityTokenWithPolicy(jdbcTemplate, token.id(), token.secret(), userId, Active.name(),
				User.name(), BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeId)).build());

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");

		// WHEN
		api(token, HttpMethod.POST, GRANTS_PATH, grantBody(userId2, nodeId))
				.andExpect(status().isForbidden());

		// THEN
		// @formatter:off
		then(grantCount(userId))
			.as("A restricted token cannot grant access")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void grant_otherUser() throws Exception {
		// GIVEN
		final Token token = createToken(userId);
		final Long nodeId = createUserNodes(1).getFirst();

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");
		final Token token2 = createToken(userId2);

		final Long grantId = createGrant(token, grantBody(userId2, nodeId));
		final String path = GRANTS_PATH + "/" + grantId;

		// WHEN
		api(token2, HttpMethod.GET, path, null).andExpect(status().isForbidden());
		api(token2, HttpMethod.PUT, path, grantBody(userId2, nodeId)).andExpect(status().isForbidden());
		api(token2, HttpMethod.POST, path + "/enabled/false", null).andExpect(status().isForbidden());
		api(token2, HttpMethod.DELETE, path, null).andExpect(status().isOk());

		// THEN
		// @formatter:off
		then(grantCount(userId))
			.as("Grant cannot be deleted by another user, even the grantee")
			.isOne()
			;
		final String json = api(token, HttpMethod.GET, GRANTS_PATH, null)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;
		then(json)
			.asInstanceOf(JSON)
			.node("data.results[0]")
			.isObject()
			.as("Grant cannot be disabled by another user, even the grantee")
			.containsEntry("enabled", true)
			;
		// @formatter:on
	}

	@Test
	public void listGrants_emptyGrantIds() throws Exception {
		// GIVEN
		final Token token = createToken(userId);
		final Long nodeId = createUserNodes(1).getFirst();

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");

		createGrant(token, grantBody(userId2, nodeId));

		// WHEN
		final Instant now = Instant.now();
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(token.id())
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path(GRANTS_PATH)
				.queryParams(Map.of("grantIds", ""))
				.useSnDate(true).date(now)
				.saveSigningKey(token.secret());
		final String json = mvc.perform(request(HttpMethod.GET, GRANTS_PATH)
				.param("grantIds", "")
				.header(HttpHeaders.AUTHORIZATION, auth.build())
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(json)
			.asInstanceOf(JSON)
			.node("data.results")
			.as("Empty criteria is ignored")
			.isArray()
			.hasSize(1)
			;
		// @formatter:on
	}

	@Test
	public void createGrant_noNodesOrSources_message() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");

		// WHEN
		final String json = api(token, HttpMethod.POST, GRANTS_PATH,
				Map.of("granteeId", userId2, "enabled", true)).andReturn().getResponse()
						.getContentAsString();

		// THEN
		// @formatter:off
		then(json)
			.as("Validation message resolved")
			.contains("At least one node ID or source ID is required.")
			;
		// @formatter:on
	}

	@Test
	public void receivedGrants_onlyEnabled() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long ownerId = randomLong();
		setupTestUser(ownerId, ownerId + "@localhost");
		final Token ownerToken = createToken(ownerId);
		final Long ownerNodeId = createUserNodes(ownerId, 1).getFirst();

		final Long grantId = createGrant(ownerToken, grantBody(userId, ownerNodeId));
		api(ownerToken, HttpMethod.POST, GRANTS_PATH + "/" + grantId + "/enabled/false", null)
				.andExpect(status().isOk());

		final Long grantId2 = createGrant(ownerToken,
				Map.of("granteeId", userId, "sourceIds", List.of("/a/**"), "enabled", true));

		// WHEN
		// @formatter:off
		final String json = api(token, HttpMethod.GET, GRANTS_PATH + "/received", null)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(json)
			.asInstanceOf(JSON)
			.node("data.results")
			.as("Disabled grant not revealed to grantee")
			.isArray()
			.hasSize(1)
			;
		then(json)
			.asInstanceOf(JSON)
			.node("data.results[0]")
			.isObject()
			.as("Owner and details of enabled grant returned")
			.containsEntry("userId", ownerId)
			.containsEntry("grantId", grantId2)
			.containsEntry("sourceIds", List.of("/a/**"))
			;
		then(api(ownerToken, HttpMethod.GET, GRANTS_PATH + "/received", null)
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString())
			.asInstanceOf(JSON)
			.node("data.results")
			.as("Grants made by a user are not grants received")
			.isArray()
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void availableStreams_thenBulkAliases() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long ownerId = randomLong();
		setupTestUser(ownerId, ownerId + "@localhost");
		final Token ownerToken = createToken(ownerId);

		final Long hostNodeId = createUserNodes(1).getFirst();
		final Long ownerNodeId = createUserNodes(ownerId, 1).getFirst();
		final var meta1 = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeId, "/shared/1");
		final var meta2 = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeId, "/shared/2");
		final var secret = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeId, "/secret/1");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta1, meta2, secret));

		createGrant(ownerToken, Map.of("granteeId", userId, "nodeIds", List.of(ownerNodeId), "sourceIds",
				List.of("/shared/*"), "enabled", true));

		// WHEN
		// @formatter:off
		final String json = apiGet(token, GRANTS_PATH + "/received/streams",
				Map.of("objectId", hostNodeId.toString()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(json)
			.asInstanceOf(JSON)
			.node("data.results")
			.as("Only granted streams listed")
			.isArray()
			.hasSize(2)
			;
		then(json)
			.asInstanceOf(JSON)
			.node("data.results[0]")
			.isObject()
			.as("Stream details returned")
			.containsOnlyKeys("userId", "objectId", "sourceId")
			.containsEntry("userId", ownerId)
			.containsEntry("objectId", ownerNodeId)
			.containsEntry("sourceId", "/shared/1")
			;

		// WHEN
		final String bulkJson = api(token, HttpMethod.POST, ALIAS_PATH + "/bulk",
				List.of(aliasBody(ownerNodeId, "/shared/1", hostNodeId),
						aliasBody(ownerNodeId, "/shared/2", hostNodeId)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(bulkJson)
			.asInstanceOf(JSON)
			.node("data")
			.as("Saved aliases returned")
			.isArray()
			.hasSize(2)
			;
		then(aliasCountForAliasNode(hostNodeId))
			.as("Aliases created")
			.isEqualTo(2)
			;
		// @formatter:on
	}

	@Test
	public void availableStreams_otherUserNode() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long userId2 = randomLong();
		setupTestUser(userId2, userId2 + "@localhost");
		final Long otherNodeId = createUserNodes(userId2, 1).getFirst();

		// THEN
		apiGet(token, GRANTS_PATH + "/received/streams", Map.of("objectId", otherNodeId.toString()))
				.andExpect(status().isForbidden());
	}

	@Test
	public void availableStreams_publicNode() throws Exception {
		// GIVEN
		final Token token = createToken(userId);
		final Long hostNodeId = createUserNodes(userId, 1, false).getFirst();

		final Long ownerId = randomLong();
		setupTestUser(ownerId, ownerId + "@localhost");
		final Token ownerToken = createToken(ownerId);
		final Long ownerNodeId = createUserNodes(ownerId, 1).getFirst();
		final var shared = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeId, "/shared/1");
		final var secret = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeId, "/secret/1");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(shared, secret));

		createGrant(ownerToken, grantBody(userId, ownerNodeId));
		createGrant(ownerToken, Map.of("granteeId", userId, "sourceIds", List.of("/shared/*"), "enabled",
				true, "allowPublic", true));

		// WHEN
		// @formatter:off
		final String json = apiGet(token, GRANTS_PATH + "/received/streams",
				Map.of("objectId", hostNodeId.toString()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(json)
			.asInstanceOf(JSON)
			.node("data.results")
			.as("Only streams of grants that allow public listed")
			.isArray()
			.hasSize(1)
			;
		then(json)
			.asInstanceOf(JSON)
			.node("data.results[0].sourceId")
			.isEqualTo("/shared/1")
			;
		// @formatter:on
	}

	@Test
	public void availableStreams_noNode() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		// THEN
		apiGet(token, GRANTS_PATH + "/received/streams", Map.of())
				.andExpect(status().is4xxClientError());
	}

	@Test
	public void availableStreams_restrictedToken() throws Exception {
		// GIVEN
		final Long hostNodeId = createUserNodes(1).getFirst();
		final var token = new Token(randomString(20), randomString());
		insertSecurityTokenWithPolicy(jdbcTemplate, token.id(), token.secret(), userId, Active.name(),
				User.name(), BasicSecurityPolicy.builder().withNodeIds(Set.of(hostNodeId)).build());

		// THEN
		apiGet(token, GRANTS_PATH + "/received/streams", Map.of("objectId", hostNodeId.toString()))
				.andExpect(status().isForbidden());
		api(token, HttpMethod.GET, GRANTS_PATH + "/received", null).andExpect(status().isForbidden());
	}

	@Test
	public void bulkAliases_oneNotGranted() throws Exception {
		// GIVEN
		final Token token = createToken(userId);

		final Long ownerId = randomLong();
		setupTestUser(ownerId, ownerId + "@localhost");
		final Token ownerToken = createToken(ownerId);

		final Long hostNodeId = createUserNodes(1).getFirst();
		final List<Long> ownerNodeIds = createUserNodes(ownerId, 2);
		final var meta1 = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeIds.get(0), randomSourceId());
		final var meta2 = emptyMeta(randomUUID(), TEST_TZ, Node, ownerNodeIds.get(1), randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta1, meta2));

		createGrant(ownerToken, grantBody(userId, ownerNodeIds.get(0)));

		// WHEN
		api(token, HttpMethod.POST, ALIAS_PATH + "/bulk",
				List.of(aliasBody(ownerNodeIds.get(0), meta1.getSourceId(), hostNodeId),
						aliasBody(ownerNodeIds.get(1), meta2.getSourceId(), hostNodeId)))
								.andExpect(status().isForbidden());

		// THEN
		// @formatter:off
		then(aliasCountForAliasNode(hostNodeId))
			.as("No aliases created when any is denied")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void bulkAliases_invalid() throws Exception {
		// GIVEN
		final Token token = createToken(userId);
		final Long nodeId = createUserNodes(1).getFirst();
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, nodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		// WHEN
		final String json = api(token, HttpMethod.POST, ALIAS_PATH + "/bulk",
				List.of(aliasBody(nodeId, meta.getSourceId(), nodeId),
						Map.of("objectId", nodeId, "sourceId", " ", "originalObjectId", nodeId,
								"originalSourceId", meta.getSourceId())))
										.andExpect(status().isUnprocessableContent()).andReturn()
										.getResponse().getContentAsString();

		// THEN
		// @formatter:off
		then(json)
			.as("Validation message resolved")
			.contains("The alias source ID must not be empty.")
			;
		then(aliasCountForAliasNode(nodeId))
			.as("No aliases created when any is invalid")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void bulkAliases_tooMany() throws Exception {
		// GIVEN
		final Token token = createToken(userId);
		final Long nodeId = createUserNodes(1).getFirst();

		final var inputs = Collections.nCopies(1001, aliasBody(nodeId, randomSourceId(), nodeId));

		// THEN
		api(token, HttpMethod.POST, ALIAS_PATH + "/bulk", inputs).andExpect(status().is4xxClientError());
	}

}
