/* ==================================================================
 * MyNodesControllerWebTests.java - 24/03/2026 2:40:35 pm
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

package net.solarnetwork.central.reg.web.test;

import static java.util.Map.entry;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertNode;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.reg.web.MyNodesController;
import net.solarnetwork.central.test.security.WithMockSecurityUser;

/**
 * Web integration tests for the {@link MyNodesController} class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("logging-user-event-appender")
public class MyNodesControllerWebTests {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private MockMvc mvc;

	@WithMockSecurityUser
	@Test
	public void updateNodeName() throws Exception {
		// GIVEN
		final String zoneId = "Pacific/Auckland";
		final String country = "NZ";
		final Long locId = insertLocation(jdbcOperations, country, zoneId);

		final Long userId = DEFAULT_USER_ID;
		final String username = randomString();
		final String userDisplayName = randomString();
		insertUser(jdbcOperations, userId, username, randomString(), userDisplayName);

		final Long nodeId = insertNode(jdbcOperations, locId);
		insertUserNode(jdbcOperations, userId, nodeId);

		final String name = randomString();
		final String desc = randomString();
		final boolean reqAuth = true;

		// WHEN
		// @formatter:off
		var response = mvc.perform(post("/u/sec/my-nodes/updateNode")
				.accept(MediaType.APPLICATION_JSON)
				.param("name", name)
				.param("description", desc)
				.param("requiresAuthorization", String.valueOf(reqAuth))
				.param("node.location.zone", zoneId)
				.param("node.location.country", country)
				.param("node.location.postalCode", "")
				.param("node.location.locality", "")
				.param("node.location.stateOrProvince", "")
				.param("node.location.region", "")
				.param("node.location.street", "")
				.param("node.location.lat", "")
				.param("node.location.lon", "")
				.param("node.location.el", "")
				.param("node.id", nodeId.toString())
				.param("user.id", userId.toString())
				.param("node.locationId", locId.toString())
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		/*-
		{
			"created":"2026-03-24 02:06:55.20377Z",
			"description":"32ce31f5d98045",
			"id":927073525524430549,
			"idAndName":"927073525524430549 - d94954eaf65a40",
			"name":"d94954eaf65a40",
			"node":{
				"id":927073525524430549,
				"locationId":3774925088062166984,
				"created":"2026-03-24 02:06:55.20377Z",
				"timeZone":"Pacific/Auckland"
			},
			"nodeLocation":{
				"id":3774925088062166984,
				"country":"NZ",
				"zone":"Pacific/Auckland"
			},
			"requiresAuthorization":true,
			"user":{
				"created":"2026-03-24 02:06:55.20377Z",
				"email":"f75817ab3c6c45",
				"enabled":true,
				"id":1,
				"name":"e9b84b8a861b44"
			},
			"userId":1}
		 */
		then(response)
			.isNotNull()
			.asInstanceOf(JSON)
			.isObject()
			.contains(
				entry("id", nodeId),
				entry("name", name),
				entry("description", desc),
				entry("userId", userId),
				entry("requiresAuthorization", reqAuth)
			)
			;

		then(response).asInstanceOf(JSON).node("node")
			.isObject()
			.contains(
				entry("id", nodeId),
				entry("locationId", locId),
				entry("timeZone", zoneId)
			)
			;

		then(response).asInstanceOf(JSON).node("nodeLocation")
			.isObject()
			.contains(
				entry("id", locId),
				entry("country", country),
				entry("zone", zoneId)
			)
			;

		then(response).asInstanceOf(JSON).node("user")
			.isObject()
			.contains(
				entry("id", userId),
				entry("email", username),
				entry("name", userDisplayName)
			)
			;

		// @formatter:on
	}

}
