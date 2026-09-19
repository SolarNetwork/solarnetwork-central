/* ==================================================================
 * NodeMetadataControllerWebTests.java - 16/12/2025 7:15:28 am
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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.security.SecurityTokenStatus.Active;
import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.codec.jackson.JsonUtils.getJSONString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.reg.web.api.v1.NodeMetadataController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the {@link NodeMetadataController} class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class NodeMetadataControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private SolarNodeMetadataDao solarNodeMetadataDao;

	private Long userId;

	@BeforeEach
	public void setup() {
		userId = randomLong();
		setupTestUser(userId, randomString() + "@localhost");
		setupTestLocation();
	}

	private void createTestNode(Long nodeId, boolean requireAuth) {
		setupTestNode(nodeId);
		insertUserNode(jdbcTemplate, userId, nodeId, requireAuth);
	}

	private List<Long> setupTestNodes(final int count, boolean requireAuth) {
		List<Long> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			Long nodeId = randomLong();
			createTestNode(nodeId, requireAuth);
			result.add(nodeId);
		}
		return result;
	}

	@Test
	public void find_unrestrictedTokenActor() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta")
				.queryParams(Map.of("nodeIds", commaDelimitedStringFromCollection(nodeIds)))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/nodes/meta")
				.queryParam("nodeIds", commaDelimitedStringFromCollection(nodeIds))
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data.results")
			.isArray()
			.extracting("nodeId")
			.as("Metadata for all given node IDs returned using unrestricted policy")
			.containsExactlyInAnyOrderElementsOf(nodeIds)
			;
		// @formatter:on
	}

	@Test
	public void find_restrictedTokenActor() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		final List<Long> tokenNodeIds = List.of(nodeIds.get(0), nodeIds.get(2));
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				getJSONString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(tokenNodeIds.toArray(Long[]::new))).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta")
				.queryParams(Map.of("nodeIds", commaDelimitedStringFromCollection(nodeIds)))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/nodes/meta")
				.queryParam("nodeIds", commaDelimitedStringFromCollection(nodeIds))
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data.results")
			.isArray()
			.extracting("nodeId")
			.as("Metadata for all allowed node IDs returned using restricted policy")
			.containsExactlyInAnyOrderElementsOf(tokenNodeIds)
			;
		// @formatter:on
	}

	@Test
	public void find_restrictedTokenActor_policyHasInvalidNodeId() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		final List<Long> tokenNodeIds = List.of(nodeIds.get(0), nodeIds.get(2), -1L);
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				getJSONString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(tokenNodeIds.toArray(Long[]::new))).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final List<Long> requestNodeIds = List.of(nodeIds.get(0), -1L);

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta")
				.queryParams(Map.of("nodeIds", commaDelimitedStringFromCollection(requestNodeIds)))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/nodes/meta")
				.queryParam("nodeIds", commaDelimitedStringFromCollection(requestNodeIds))
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data.results")
			.isArray()
			.extracting("nodeId")
			.as("Metadata for just allowed and valid node IDs returned using restricted policy")
			.containsExactlyInAnyOrderElementsOf(List.of(nodeIds.get(0)))
			;
		// @formatter:on
	}

	@Test
	public void get_unrestrictedTokenActor() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(1, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.isObject()
			.extracting("nodeId")
			.as("Metadata allowed for unrestricted policy")
			.isEqualTo(requestNodeId)
			;
		// @formatter:on
	}

	@Test
	public void get_restrictedTokenActor_allowedByPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				getJSONString(BasicSecurityPolicy.builder().withNodeIds(Set.copyOf(nodeIds)).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.isObject()
			.extracting("nodeId")
			.as("Metadata allowed for node ID in restricted policy")
			.isEqualTo(requestNodeId)
			;
		// @formatter:on
	}

	@Test
	public void get_restrictedTokenActor_notAllowedByPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				getJSONString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.get(0), nodeIds.get(2))).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(1);

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No metadata returned for node ID not allowed by policy")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void get_restrictedTokenActor_notValidByAllowedByPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, getJSONString(
				BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeIds.get(0), -1L)).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = -1L;

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No metadata returned for node ID not allowed by policy")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void delete_unrestrictedTokenActor() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(1, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				delete("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("No data returned on delete")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void delete_restrictedTokenActor_allowedByPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				getJSONString(BasicSecurityPolicy.builder().withNodeIds(Set.copyOf(nodeIds)).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				delete("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("No data returned on delete")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void delete_restrictedTokenActor_notAllowedByPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, getJSONString(
				BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeIds.get(0))).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(2);

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				delete("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data returned on delete")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void delete_restrictedTokenActor_notAllowedByValiation() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, getJSONString(
				BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeIds.get(0), -1L)).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = -1L;

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				delete("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data returned on delete")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void add_unrestrictedTokenActor() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(1, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));
		final String reqJson = """
				{
					"m": {
						"foo": true
					}
				}
				""";

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/nodes/meta/" + requestNodeId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("No data returned on metadata update")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void add_restrictedTokenActor_allowedByPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				getJSONString(BasicSecurityPolicy.builder().withNodeIds(Set.copyOf(nodeIds)).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));
		final String reqJson = """
				{
					"m": {
						"foo": true
					}
				}
				""";

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/nodes/meta/" + requestNodeId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("No data returned on metadata update")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void add_restrictedTokenActor_notAllowedByPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, getJSONString(
				BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeIds.get(0))).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(2);
		final String reqJson = """
				{
					"m": {
						"foo": true
					}
				}
				""";

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data returned on metadata update")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void add_restrictedTokenActor_notAllowedByValidation() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, getJSONString(
				BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeIds.get(0), -1L)).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = -1L;
		final String reqJson = """
				{
					"m": {
						"foo": true
					}
				}
				""";

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data returned on metadata update")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void replace_unrestrictedTokenActor() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(1, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));
		final String reqJson = """
				{
					"m": {
						"foo": true
					}
				}
				""";

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PUT.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				put("/api/v1/sec/nodes/meta/" + requestNodeId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("No data returned on metadata update")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void replace_restrictedTokenActor_allowedByPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				getJSONString(BasicSecurityPolicy.builder().withNodeIds(Set.copyOf(nodeIds)).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));
		final String reqJson = """
				{
					"m": {
						"foo": true
					}
				}
				""";

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PUT.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				put("/api/v1/sec/nodes/meta/" + requestNodeId)
				.header(HttpHeaders.AUTHORIZATION, authHeader)
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON)
				.content(reqJson)
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("No data returned on metadata replace")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void replace_restrictedTokenActor_notAllowedByPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, getJSONString(
				BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeIds.get(0))).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = nodeIds.get(2);
		final String reqJson = """
				{
					"m": {
						"foo": true
					}
				}
				""";

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PUT.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				put("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data returned on metadata replace")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void replace_restrictedTokenActor_notAllowedByValidation() throws Exception {
		// GIVEN
		final List<Long> nodeIds = setupTestNodes(3, true);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, getJSONString(
				BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeIds.get(0), -1L)).build()));

		final Instant metaTs = Instant.now().truncatedTo(ChronoUnit.SECONDS);

		// create metadata for each
		for ( Long nodeId : nodeIds ) {
			SolarNodeMetadata nodeMeta = new SolarNodeMetadata(nodeId);
			nodeMeta.setCreated(metaTs);
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.putInfoValue("foo", nodeId);
			nodeMeta.setMeta(meta);
			solarNodeMetadataDao.save(nodeMeta);
		}

		// WHEN
		final Instant now = Instant.now();
		final Long requestNodeId = -1L;
		final String reqJson = """
				{
					"m": {
						"foo": true
					}
				}
				""";

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PUT.name())
				.host("localhost")
				.path("/api/v1/sec/nodes/meta/" +requestNodeId)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				put("/api/v1/sec/nodes/meta/" + requestNodeId)
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

		// THEN
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", false)
			.node("data")
			.as("No data returned on metadata replace")
			.isAbsent()
			;
		// @formatter:on
	}

}
