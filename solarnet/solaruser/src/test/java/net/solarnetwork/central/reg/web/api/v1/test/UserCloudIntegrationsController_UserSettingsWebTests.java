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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import net.solarnetwork.central.c2c.dao.UserSettingsEntityDao;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;
import net.solarnetwork.central.reg.web.api.v1.UserCloudIntegrationsControlsController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.c2c.domain.UserSettingsEntityInput;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the
 * {@link UserCloudIntegrationsControlsController} settings actions.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS)
public class UserCloudIntegrationsController_UserSettingsWebTests
		extends AbstractJUnit5CentralTransactionalTest {

	private static final Clock clock = Clock.tickMillis(ZoneOffset.UTC);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private UserSettingsEntityDao settingsDao;

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

	private UserSettingsEntity createSettings(boolean solarFlux, boolean solarIn) {
		var entity = new UserSettingsEntity(userId, clock.instant());
		entity.setPublishToSolarFlux(solarFlux);
		entity.setPublishToSolarIn(solarIn);
		return settingsDao.get(settingsDao.save(entity));
	}

	@Test
	public void saveSettings_asUnrestrictedToken() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		final UserSettingsEntityInput input = new UserSettingsEntityInput();
		input.setPublishToSolarFlux(true);
		input.setPublishToSolarIn(true);

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PUT.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/settings")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				put("/api/v1/sec/user/c2c/settings")
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
			.as("Settings created")
			.contains(
				entry("userId", userId),
				entry("publishToSolarFlux", true),
				entry("publishToSolarIn", true)
			)
			;
		// @formatter:on
	}

	@Test
	public void saveSettings_asRestrictedToken_deined() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, objectMapper
				.writeValueAsString(BasicSecurityPolicy.builder().withRefreshAllowed(false).build()));

		final UserSettingsEntityInput input = new UserSettingsEntityInput();
		input.setPublishToSolarFlux(true);
		input.setPublishToSolarIn(true);

		final String reqJson = objectMapper.writeValueAsString(input);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.PUT.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/settings")
				.contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
				.contentSha256(DigestUtils.sha256(reqJson))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				put("/api/v1/sec/user/c2c/settings")
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
	public void deleteSettings_asUnrestrictedToken() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, null);

		createSettings(true, true);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/settings")
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				delete("/api/v1/sec/user/c2c/settings")
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
			.as("No data returned on delete")
			.isAbsent()
			;
		// @formatter:on
	}

	@Test
	public void deleteSettings_asRestrictedToken_denied() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId, Active, User, objectMapper
				.writeValueAsString(BasicSecurityPolicy.builder().withRefreshAllowed(false).build()));

		createSettings(true, true);

		// WHEN
		final Instant now = Instant.now();

		// WHEN
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.method(HttpMethod.DELETE.name())
				.host("localhost")
				.path("/api/v1/sec/user/c2c/settings")
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		final String authHeader = auth.build();

		final String result = mvc.perform(
				delete("/api/v1/sec/user/c2c/settings")
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
			.as("No data returned on forbidden response")
			.isAbsent()
			;
		// @formatter:on
	}

}
