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
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Clock;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.reg.web.api.v1.UserCloudIntegrationsControlsController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamRakeTaskEntityBaseInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamRakeTaskEntityInput;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the
 * {@link UserCloudIntegrationsControlsController} rake task actions.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS)
public class UserCloudIntegrationsController_RakeTaskWebTests
		extends AbstractJUnit5CentralTransactionalTest {

	private static final Clock clock = Clock.tickMillis(ZoneOffset.UTC);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamDao;

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
	public void createRakeTask_asUnrestrictedToken() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Long reqNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		final CloudDatumStreamRakeTaskEntityInput input = new CloudDatumStreamRakeTaskEntityInput();
		input.setDatumStreamId(datumStream.getConfigId());
		input.setExecuteAt(clock.instant().truncatedTo(ChronoUnit.DAYS).plus(24, ChronoUnit.HOURS));
		input.setOffset(Period.ofDays(1));
		input.setState(BasicClaimableJobState.Completed);

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-stream-rake-tasks")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-stream-rake-tasks")
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
			.as("Rake task created for given node ID")
			.contains(
				entry("userId", userId),
				entry("datumStreamId", datumStream.getConfigId())
			)
			;
		// @formatter:on
	}

	@Test
	public void createRakeTask_asRestrictedToken_allowed_datumStreamNodeIdInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.get(0), nodeIds.get(2))).build()));

		final Long reqNodeId = nodeIds.get(0); // allowed in policy

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		final CloudDatumStreamRakeTaskEntityInput input = new CloudDatumStreamRakeTaskEntityInput();
		input.setDatumStreamId(datumStream.getConfigId());
		input.setExecuteAt(clock.instant().truncatedTo(ChronoUnit.DAYS).plus(24, ChronoUnit.HOURS));
		input.setOffset(Period.ofDays(1));
		input.setState(BasicClaimableJobState.Completed);

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-stream-rake-tasks")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-stream-rake-tasks")
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
			.as("Rake task created for given node ID")
			.contains(
				entry("userId", userId),
				entry("datumStreamId", datumStream.getConfigId())
			)
			;
		// @formatter:on
	}

	@Test
	public void createRakeTask_asRestrictedToken_denied_datumStreamNodeIdNotInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.get(0), nodeIds.get(2))).build()));

		final Long reqNodeId = nodeIds.get(1); // not in policy

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		final CloudDatumStreamRakeTaskEntityInput input = new CloudDatumStreamRakeTaskEntityInput();
		input.setDatumStreamId(datumStream.getConfigId());
		input.setExecuteAt(clock.instant().truncatedTo(ChronoUnit.DAYS).plus(24, ChronoUnit.HOURS));
		input.setOffset(Period.ofDays(1));
		input.setState(BasicClaimableJobState.Completed);

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-stream-rake-tasks")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-stream-rake-tasks")
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

	//

	@Test
	public void replace_asUnrestrictedToken() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final Long reqNodeId = nodeIds.get(RNG.nextInt(nodeIds.size()));

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		final List<CloudDatumStreamRakeTaskEntityBaseInput> inputs = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			final CloudDatumStreamRakeTaskEntityInput input = new CloudDatumStreamRakeTaskEntityInput();
			input.setDatumStreamId(datumStream.getConfigId());
			input.setExecuteAt(clock.instant().truncatedTo(ChronoUnit.DAYS).plus(24, ChronoUnit.HOURS));
			input.setOffset(Period.ofDays(i + 1));
			input.setState(BasicClaimableJobState.Completed);
			inputs.add(input);
		}

		final String reqJson = objectMapper.writeValueAsString(inputs);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-stream-rake-tasks/%d/tasks".formatted(datumStream.getConfigId()))
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-stream-rake-tasks/%d/tasks".formatted(datumStream.getConfigId()))
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

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("Persisted entity array returned for data")
			.isArray()
			.as("Array has items for every entity")
			.hasSize(3)
			.allSatisfy(item -> {
				then(item)
					.asInstanceOf(JSON)
					.as("Data array elements are entity objects")
					.isObject()
					.as("ID assigned")
					.containsKey("configId")
					.contains(
						entry("userId", userId),
						entry("datumStreamId", datumStream.getConfigId())
					)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void replace_asRestrictedToken_allowed_datumStreamNodeIdInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.get(0), nodeIds.get(2))).build()));

		final Long reqNodeId = nodeIds.get(0); // in policy

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		final List<CloudDatumStreamRakeTaskEntityBaseInput> inputs = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			final CloudDatumStreamRakeTaskEntityInput input = new CloudDatumStreamRakeTaskEntityInput();
			input.setDatumStreamId(datumStream.getConfigId());
			input.setExecuteAt(clock.instant().truncatedTo(ChronoUnit.DAYS).plus(24, ChronoUnit.HOURS));
			input.setOffset(Period.ofDays(i + 1));
			input.setState(BasicClaimableJobState.Completed);
			inputs.add(input);
		}

		final String reqJson = objectMapper.writeValueAsString(inputs);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-stream-rake-tasks/%d/tasks".formatted(datumStream.getConfigId()))
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-stream-rake-tasks/%d/tasks".formatted(datumStream.getConfigId()))
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

		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("Persisted entity array returned for data")
			.isArray()
			.as("Array has items for every entity")
			.hasSize(3)
			.allSatisfy(item -> {
				then(item)
					.asInstanceOf(JSON)
					.as("Data array elements are entity objects")
					.isObject()
					.as("ID assigned")
					.containsKey("configId")
					.contains(
						entry("userId", userId),
						entry("datumStreamId", datumStream.getConfigId())
					)
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void replace_asRestrictedToken_denied_datumStreamNodeIdNotInPolicy() throws Exception {
		// GIVEN
		final List<Long> nodeIds = createUserNodes(3);

		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User,
				objectMapper.writeValueAsString(BasicSecurityPolicy.builder()
						.withNodeIds(Set.of(nodeIds.get(0), nodeIds.get(2))).build()));

		final Long reqNodeId = nodeIds.get(1); // not in policy

		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId, reqNodeId);

		final List<CloudDatumStreamRakeTaskEntityBaseInput> inputs = new ArrayList<>(3);
		for ( int i = 0; i < 3; i++ ) {
			final CloudDatumStreamRakeTaskEntityInput input = new CloudDatumStreamRakeTaskEntityInput();
			input.setDatumStreamId(datumStream.getConfigId());
			input.setExecuteAt(clock.instant().truncatedTo(ChronoUnit.DAYS).plus(24, ChronoUnit.HOURS));
			input.setOffset(Period.ofDays(i + 1));
			input.setState(BasicClaimableJobState.Completed);
			inputs.add(input);
		}

		final String reqJson = objectMapper.writeValueAsString(inputs);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.POST.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/datum-stream-rake-tasks/%d/tasks".formatted(datumStream.getConfigId()))
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				post("/api/v1/sec/user/c2c/datum-stream-rake-tasks/%d/tasks".formatted(datumStream.getConfigId()))
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

}
