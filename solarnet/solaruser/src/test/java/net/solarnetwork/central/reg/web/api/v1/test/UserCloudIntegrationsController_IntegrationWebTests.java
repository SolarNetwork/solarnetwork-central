/* ==================================================================
 * UserCloudIntegrationsController_IntegrationWebTests.java - 21/07/2026 2:37:13 pm
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
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.reg.config.WebSecurityConfig.CLOUD_INTEGRATIONS_AUTHORITY;
import static net.solarnetwork.central.security.SecurityTokenStatus.Active;
import static net.solarnetwork.central.security.SecurityTokenType.User;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserRoles;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.dao.ModifiableServicePropertiesDao.MergeMode;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.security.Snws2AuthorizationBuilder;
import tools.jackson.databind.ObjectMapper;

/**
 * FIXME
 *
 * <p>
 * TODO
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS)
public class UserCloudIntegrationsController_IntegrationWebTests
		extends AbstractJUnit5CentralTransactionalTest {

	private static final Clock clock = Clock.tickMillis(ZoneOffset.UTC);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private CloudIntegrationConfigurationDao integrationDao;

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

	private CloudIntegrationConfiguration createIntegration(Long userId,
			Map<String, Object> serviceProps) {
		CloudIntegrationConfiguration conf = new CloudIntegrationConfiguration(
				unassignedEntityIdKey(userId), clock.instant(), randomString(), randomString());
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		conf.setServiceProps(serviceProps);
		return integrationDao.get(integrationDao.save(conf));
	}

	@Test
	public void mergeServiceProperties_simple() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		// @formatter:off
		final Map<String, Object> initialProps = Map.of(
				"foo", "f",
				"bar", "b",
				"obj", Map.of(
						"n1", 1
					),
				"ary", List.of("a1")
			);
		// @formatter:on

		final CloudIntegrationConfiguration integration = createIntegration(userId, initialProps);

		// @formatter:off
		final Map<String, Object> input = Map.of(
				"bar", "B",
				"baz", "Z",
				"obj", Map.of(
						"N1", 1
					),
				"ary", List.of("A1")
			);
		// @formatter:on

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// THEN
		final String uriPath = "/api/v1/sec/user/c2c/integrations/%d/serviceProperties"
				.formatted(integration.getConfigId());
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PATCH.name())
				.host("localhost")
				.path(uriPath)
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				patch(uriPath)
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
			.isObject()
			.hasSize(5)
			.contains(
				entry("foo", "f"),
				entry("bar", "B"),
				entry("baz", "Z")
			)
			.hasEntrySatisfying("obj", obj -> {
				then(obj)
					.asInstanceOf(JSON)
					.isObject()
					.hasSize(1)
					.containsEntry("N1", 1)
					;
			})
			.hasEntrySatisfying("ary", obj -> {
				then(obj)
					.asInstanceOf(JSON)
					.isArray()
					.hasSize(1)
					.containsExactly("A1")
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void mergeServiceProperties_recursive() throws Exception {
		// GIVEN
		final MergeMode mode = MergeMode.RecursiveObjects;
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		// @formatter:off
		final Map<String, Object> initialProps = Map.of(
				"foo", "f",
				"bar", "b",
				"obj", Map.of(
						"n1", 1
					),
				"ary", List.of("a1")
			);
		// @formatter:on

		final CloudIntegrationConfiguration integration = createIntegration(userId, initialProps);

		// @formatter:off
		final Map<String, Object> input = Map.of(
				"bar", "B",
				"baz", "Z",
				"obj", Map.of(
						"N1", 1
					),
				"ary", List.of("A1")
			);
		// @formatter:on

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// THEN
		final String uriPath = "/api/v1/sec/user/c2c/integrations/%d/serviceProperties"
				.formatted(integration.getConfigId());
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PATCH.name())
				.host("localhost")
				.path(uriPath)
				.queryParams(Map.of("mode", mode.name()))
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				patch(uriPath)
				.queryParam("mode", mode.name())
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
			.isObject()
			.hasSize(5)
			.contains(
				entry("foo", "f"),
				entry("bar", "B"),
				entry("baz", "Z")
			)
			.hasEntrySatisfying("obj", obj -> {
				then(obj)
					.asInstanceOf(JSON)
					.isObject()
					.hasSize(2)
					.as("Object is recursively merged")
					.containsOnly(
						entry("n1", 1),
						entry("N1", 1)
					)
					;
			})
			.hasEntrySatisfying("ary", obj -> {
				then(obj)
					.asInstanceOf(JSON)
					.isArray()
					.hasSize(1)
					.as("Array is replaced")
					.containsExactly("A1")
					;
			})
			;
		// @formatter:on
	}

	@Test
	public void mergeServiceProperties_recursiveArrays() throws Exception {
		// GIVEN
		final MergeMode mode = MergeMode.RecursiveObjectsAndArrays;
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		// @formatter:off
		final Map<String, Object> initialProps = Map.of(
				"foo", "f",
				"bar", "b",
				"obj", Map.of(
						"n1", 1
					),
				"ary", List.of("a1")
			);
		// @formatter:on

		final CloudIntegrationConfiguration integration = createIntegration(userId, initialProps);

		// @formatter:off
		final Map<String, Object> input = Map.of(
				"bar", "B",
				"baz", "Z",
				"obj", Map.of(
						"N1", 1
					),
				"ary", List.of("A1")
			);
		// @formatter:on

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// THEN
		final String uriPath = "/api/v1/sec/user/c2c/integrations/%s/serviceProperties"
				.formatted(integration.getConfigId());
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PATCH.name())
				.host("localhost")
				.path(uriPath)
				.queryParams(Map.of("mode", mode.name()))
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				patch(uriPath)
				.queryParam("mode", mode.name())
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
			.isObject()
			.hasSize(5)
			.contains(
				entry("foo", "f"),
				entry("bar", "B"),
				entry("baz", "Z")
			)
			.hasEntrySatisfying("obj", obj -> {
				then(obj)
					.asInstanceOf(JSON)
					.isObject()
					.hasSize(2)
					.as("Object is recursively merged")
					.containsOnly(
						entry("n1", 1),
						entry("N1", 1)
					)
					;
			})
			.hasEntrySatisfying("ary", obj -> {
				then(obj)
					.asInstanceOf(JSON)
					.isArray()
					.hasSize(2)
					.as("Array is recursively merged")
					.containsExactly("a1", "A1")
					;
			})
			;
		// @formatter:on
	}

}
