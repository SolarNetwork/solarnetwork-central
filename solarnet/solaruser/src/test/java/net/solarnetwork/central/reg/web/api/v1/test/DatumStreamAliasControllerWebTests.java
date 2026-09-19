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
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
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
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.reg.web.api.v1.DatumStreamAliasController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.security.Snws2AuthorizationBuilder;
import tools.jackson.databind.ObjectMapper;

/**
 * Web API level integration tests for the {@link DatumStreamAliasController}
 * class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc

public class DatumStreamAliasControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

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
		List<Long> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			Long nodeId = randomLong();
			setupTestNode(nodeId);
			insertUserNode(jdbcTemplate, userId, nodeId, true);
			result.add(nodeId);
		}
		return result;
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

}
