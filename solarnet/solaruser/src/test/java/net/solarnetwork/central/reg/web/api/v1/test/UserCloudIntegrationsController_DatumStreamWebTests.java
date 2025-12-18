/* ==================================================================
 * UserCloudIntegrationsController_DatumStreamWebTests.java - 17/12/2025 1:26:41 pm
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
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.reg.config.WebSecurityConfig.CLOUD_INTEGRATIONS_AUTHORITY;
import static net.solarnetwork.central.security.SecurityTokenStatus.Active;
import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserRoles;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static net.solarnetwork.util.StringUtils.commaDelimitedStringFromCollection;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.reg.web.api.v1.UserCloudIntegrationsControlsController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamConfigurationInput;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the
 * {@link UserCloudIntegrationsControlsController} datum stream actions.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS)
public class UserCloudIntegrationsController_DatumStreamWebTests
		extends AbstractJUnit5CentralTransactionalTest {

	private static final Clock clock = Clock.tickMillis(ZoneOffset.UTC);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private CloudIntegrationConfigurationDao integrationDao;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamDao;

	@Autowired
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;

	@Autowired
	private ObjectMapper objectMapper;

	private Long userId;

	@BeforeEach
	public void setup() {
		userId = randomLong();
		setupTestUser(userId, userId + "@localhost");
		insertUserRoles(jdbcTemplate, userId, "ROLE_USER", CLOUD_INTEGRATIONS_AUTHORITY);
		setupTestLocation();
	}

	private List<Long> createUserNodes(final int count) {
		List<Long> result = new ArrayList<>(count);
		for ( int i = 0; i < count; i++ ) {
			Long nodeId = randomLong();
			setupTestNode(nodeId);
			insertUserNode(jdbcTemplate, userId, nodeId, true);
			result.add(nodeId);
		}
		return result;
	}

	private CloudIntegrationConfiguration createIntegration(Long userId) {
		CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(
				unassignedEntityIdKey(userId), clock.instant());
		conf.setModified(conf.getCreated());
		conf.setName(randomString());
		conf.setServiceIdentifier(randomString());
		conf.setEnabled(true);

		return integrationDao.get(integrationDao.save(conf));
	}

	private CloudDatumStreamMappingConfiguration createDatumStreamMapping(Long userId,
			Long integrationId) {
		CloudDatumStreamMappingConfiguration conf = new CloudDatumStreamMappingConfiguration(
				unassignedEntityIdKey(userId), clock.instant());
		conf.setModified(conf.getCreated());
		conf.setName(randomString());
		conf.setEnabled(true);
		conf.setIntegrationId(integrationId);

		return datumStreamMappingDao.get(datumStreamMappingDao.save(conf));
	}

	private CloudDatumStreamConfiguration createDatumStream(Long userId, Long nodeId) {
		CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(
				unassignedEntityIdKey(userId), clock.instant());
		conf.setModified(conf.getCreated());
		conf.setName(randomString());
		conf.setServiceIdentifier(randomString());
		conf.setEnabled(true);
		conf.setKind(ObjectDatumKind.Node);
		conf.setObjectId(nodeId);
		conf.setSchedule("600");
		conf.setSourceId(randomString());

		return datumStreamDao.get(datumStreamDao.save(conf));
	}

	@Test
	public void create_asUnrestrictedToken() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final List<Long> nodeIds = createUserNodes(3);
		final Long reqNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		final CloudIntegrationConfiguration integration = createIntegration(userId);
		final CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId());

		final CloudDatumStreamConfigurationInput input = new CloudDatumStreamConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setServiceIdentifier(randomString());
		input.setDatumStreamMappingId(mapping.getConfigId());
		input.setKind(ObjectDatumKind.Node);
		input.setObjectId(reqNodeId);
		input.setSchedule("600");
		input.setSourceId(randomString());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-streams")
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
			.as("ID assigned")
			.containsKey("configId")
			.as("Datum stream created for given node ID")
			.contains(
				entry("kind", "n"),
				entry("objectId", reqNodeId),
				entry("userId", userId),
				entry("datumStreamMappingId", mapping.getConfigId())
			)
			;
		// @formatter:on
	}

	@Test
	public void create_asUnrestrictedToken_invalidNodeId() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		createUserNodes(3);
		final Long reqNodeId = -1L;

		final CloudIntegrationConfiguration integration = createIntegration(userId);
		final CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId());

		final CloudDatumStreamConfigurationInput input = new CloudDatumStreamConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setServiceIdentifier(randomString());
		input.setDatumStreamMappingId(mapping.getConfigId());
		input.setKind(ObjectDatumKind.Node);
		input.setObjectId(reqNodeId);
		input.setSchedule("600");
		input.setSourceId(randomString());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-streams")
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
			.node("data")
			.as("No data returned on forbidden response")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void create_asRestrictedToken_allowed() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.toArray(Long[]::new))).build()));

		final Long reqNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		final CloudIntegrationConfiguration integration = createIntegration(userId);
		final CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId());

		final CloudDatumStreamConfigurationInput input = new CloudDatumStreamConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setServiceIdentifier(randomString());
		input.setDatumStreamMappingId(mapping.getConfigId());
		input.setKind(ObjectDatumKind.Node);
		input.setObjectId(reqNodeId);
		input.setSchedule("600");
		input.setSourceId(randomString());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-streams")
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
			.as("ID assigned")
			.containsKey("configId")
			.as("Datum stream created for given node ID")
			.contains(
				entry("kind", "n"),
				entry("objectId", reqNodeId),
				entry("userId", userId),
				entry("datumStreamMappingId", mapping.getConfigId())
			)
			;
		// @formatter:on
	}

	@Test
	public void create_asRestrictedToken_denied_nodeIdNotInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.get(0), nodeIds.get(2))).build()));

		final Long reqNodeId = nodeIds.get(1); // not in policy

		final CloudIntegrationConfiguration integration = createIntegration(userId);
		final CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId());

		final CloudDatumStreamConfigurationInput input = new CloudDatumStreamConfigurationInput();
		input.setEnabled(true);
		input.setName(randomString());
		input.setServiceIdentifier(randomString());
		input.setDatumStreamMappingId(mapping.getConfigId());
		input.setKind(ObjectDatumKind.Node);
		input.setObjectId(reqNodeId);
		input.setSchedule("600");
		input.setSourceId(randomString());

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-streams")
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
			.node("data")
			.as("No data returned on forbidden response")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void list_asRestrictedToken_emptyCriteria_nodeIdsInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);
		final List<Long> allowedNodeIds = List.of(nodeIds.getFirst(), nodeIds.getLast());

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(allowedNodeIds.toArray(Long[]::new))).build()));

		final List<Long> allowedDatumStreamIds = new ArrayList<>(allowedNodeIds.size());
		for ( Long nodeId : nodeIds ) {
			var datumStream = createDatumStream(userId, nodeId);
			if ( allowedNodeIds.contains(nodeId) ) {
				allowedDatumStreamIds.add(datumStream.getConfigId());
			}
		}

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams")
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/user/c2c/datum-streams")
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
			.node("data.results")
			.as("Result is array of entity objects")
			.isArray()
			.as("Result array count equals allowed node IDs count")
			.hasSize(allowedNodeIds.size())
			.extracting("configId")
			.as("All allowed datum streams returned")
			.containsExactlyInAnyOrderElementsOf(allowedDatumStreamIds)
			;
		// @formatter:on
	}

	@Test
	public void list_asRestrictedToken_nodeCriteria_allowed_nodeIdsInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);
		final List<Long> allowedNodeIds = List.of(nodeIds.getFirst(), nodeIds.getLast());

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(allowedNodeIds.toArray(Long[]::new))).build()));

		final List<Long> reqNodeIds = List.of(allowedNodeIds.getFirst());
		final List<Long> expectedDatumStreamIds = new ArrayList<>(reqNodeIds.size());
		for ( Long nodeId : nodeIds ) {
			var datumStream = createDatumStream(userId, nodeId);
			if ( reqNodeIds.contains(nodeId) ) {
				expectedDatumStreamIds.add(datumStream.getConfigId());
			}
		}

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams")
				.queryParams(Map.of("nodeIds", commaDelimitedStringFromCollection(reqNodeIds)))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/user/c2c/datum-streams")
				.param("nodeIds", commaDelimitedStringFromCollection(reqNodeIds))
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
			.node("data.results")
			.as("Result is array of entity objects")
			.isArray()
			.as("Result array count equals allowed node IDs count")
			.hasSize(reqNodeIds.size())
			.extracting("configId")
			.as("All allowed datum streams returned")
			.containsExactlyInAnyOrderElementsOf(expectedDatumStreamIds)
			;
		// @formatter:on
	}

	@Test
	public void list_asRestrictedToken_nodeCriteria_restricted_nodeIdsInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);
		final List<Long> allowedNodeIds = List.of(nodeIds.getFirst(), nodeIds.getLast());

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(allowedNodeIds.toArray(Long[]::new))).build()));

		final List<Long> expectedNodeIds = allowedNodeIds;
		final List<Long> expectedDatumStreamIds = new ArrayList<>(expectedNodeIds.size());
		for ( Long nodeId : nodeIds ) {
			var datumStream = createDatumStream(userId, nodeId);
			if ( expectedNodeIds.contains(nodeId) ) {
				expectedDatumStreamIds.add(datumStream.getConfigId());
			}
		}

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams")
				.queryParams(Map.of("nodeIds", commaDelimitedStringFromCollection(nodeIds)))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/user/c2c/datum-streams")
				.param("nodeIds", commaDelimitedStringFromCollection(nodeIds))
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
			.node("data.results")
			.as("Result is array of entity objects")
			.isArray()
			.as("Result array count equals allowed node IDs count")
			.hasSize(expectedNodeIds.size())
			.extracting("configId")
			.as("All allowed datum streams returned")
			.containsExactlyInAnyOrderElementsOf(expectedDatumStreamIds)
			;
		// @formatter:on
	}

	@Test
	public void get_asRestrictedToken_allowed_nodeIdInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.toArray(Long[]::new))).build()));

		final Long reqNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams/%d".formatted(datumStream.getConfigId()))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/user/c2c/datum-streams/%d".formatted(datumStream.getConfigId()))
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
			.as("Datum stream returned for given ID")
			.contains(
				entry("userId", userId),
				entry("configId", datumStream.getConfigId())
			)
			;
		// @formatter:on
	}

	@Test
	public void get_asRestrictedToken_denied_nodeIdNotInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(
						BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeIds.get(0))).build()));

		final Long reqNodeId = nodeIds.get(1);

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.GET.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams/%d".formatted(datumStream.getConfigId()))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				get("/api/v1/sec/user/c2c/datum-streams/%d".formatted(datumStream.getConfigId()))
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
	public void delete_asRestrictedToken_allowed_nodeIdInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.toArray(Long[]::new))).build()));

		final Long reqNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams/%d".formatted(datumStream.getConfigId()))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				delete("/api/v1/sec/user/c2c/datum-streams/%d".formatted(datumStream.getConfigId()))
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
			.as("No data on delete response")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void delete_asRestrictedToken_deined_nodeIdNotInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(
						BasicSecurityPolicy.builder().withNodeIds(Set.of(nodeIds.get(0))).build()));

		final Long reqNodeId = nodeIds.get(1); // not in policy

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-streams/%d".formatted(datumStream.getConfigId()))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				delete("/api/v1/sec/user/c2c/datum-streams/%d".formatted(datumStream.getConfigId()))
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

}
