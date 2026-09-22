/* ==================================================================
 * ReportableIntervalControllerWebTests.java - 22/09/2026 5:23:14 pm
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

package net.solarnetwork.central.query.web.api.test;

import static java.util.UUID.randomUUID;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertNode;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.codec.jackson.JsonUtils.getJSONString;
import static net.solarnetwork.security.AuthorizationUtils.AUTHORIZATION_DATE_HEADER_FORMATTER;
import static net.solarnetwork.security.AuthorizationUtils.SN_DATE_HEADER;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.query.web.api.ReportableIntervalController;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.security.Snws2AuthorizationBuilder;

/**
 * Web API level integration tests for the {@link ReportableIntervalController}
 * class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ReportableIntervalControllerWebTests extends AbstractJUnit5CentralTransactionalTest {

	private static final String SOURCES_PATH = "/api/v1/sec/range/sources";

	@Autowired
	private MockMvc mvc;

	private Long userId;
	private Long nodeId;
	private List<String> sourceIds;

	@BeforeEach
	public void setup() {
		userId = insertUser(jdbcTemplate);
		final Long locId = insertLocation(jdbcTemplate, TEST_LOC_COUNTRY, TEST_TZ);
		nodeId = randomLong();
		insertNode(jdbcTemplate, nodeId, locId);
		insertUserNode(jdbcTemplate, userId, nodeId, true);
		sourceIds = List.of("/s/1", "/s/2", "/s/3");
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, sourceIds.stream()
				.map(s -> emptyMeta(randomUUID(), TEST_TZ, ObjectDatumKind.Node, nodeId, s)).toList());
	}

	private String findSources(String tokenId, String tokenSecret) throws Exception {
		final Instant now = Instant.now();
		// @formatter:off
		final Snws2AuthorizationBuilder auth = new Snws2AuthorizationBuilder(tokenId)
				.host("localhost")
				.path(SOURCES_PATH)
				.queryParams(Map.of("nodeId", nodeId.toString()))
				.useSnDate(true).date(now)
				.saveSigningKey(tokenSecret);
		return mvc.perform(get(SOURCES_PATH)
				.queryParam("nodeId", nodeId.toString())
				.header(HttpHeaders.AUTHORIZATION, auth.build())
				.header(SN_DATE_HEADER, AUTHORIZATION_DATE_HEADER_FORMATTER.format(now))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;
		// @formatter:on
	}

	@Test
	public void findSources_restrictedToken_sourcesRestrictedToPolicy() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		final String policyJson = getJSONString(BasicSecurityPolicy.builder()
				.withNodeIds(Set.of(nodeId)).withSourceIds(Set.of(sourceIds.getFirst())).build(),
				null);
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), policyJson);

		// WHEN
		final String result = findSources(tokenId, tokenSecret);

		// THEN
		// @formatter:off
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("Only the policy source returned")
			.node("data")
			.isArray()
			.containsExactly(sourceIds.getFirst())
			;
		// @formatter:on
	}

	@Test
	public void findSources_unrestrictedToken_allSources() throws Exception {
		// GIVEN
		final String tokenId = randomString(20);
		final String tokenSecret = randomString();
		insertSecurityToken(jdbcTemplate, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(), null);

		// WHEN
		final String result = findSources(tokenId, tokenSecret);

		// THEN
		// @formatter:off
		then(result)
			.asInstanceOf(JSON)
			.isObject()
			.as("All sources returned")
			.node("data")
			.isArray()
			.containsExactlyInAnyOrderElementsOf(sourceIds)
			;
		// @formatter:on
	}

}
