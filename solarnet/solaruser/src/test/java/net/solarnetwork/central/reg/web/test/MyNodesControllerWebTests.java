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
import static net.solarnetwork.central.test.CommonTestUtils.randomEmail;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import java.util.Map;
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
 * @version 1.1
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

	private void createActorUser() {
		insertUser(jdbcOperations, DEFAULT_USER_ID, DEFAULT_USERNAME, randomString(), randomString());
	}

	private Long createUser(String email) {
		final Long userId = randomLong();
		insertUser(jdbcOperations, userId, email, randomString(), randomString());
		return userId;
	}

	private Long createUserNode(Long userId) {
		final Long locId = insertLocation(jdbcOperations, "NZ", "Pacific/Auckland");
		final Long nodeId = insertNode(jdbcOperations, locId);
		insertUserNode(jdbcOperations, userId, nodeId, true);
		return nodeId;
	}

	private void insertNodeTransfer(Long userId, Long nodeId, String recipient) {
		jdbcOperations.update(
				"INSERT INTO solaruser.user_node_xfer (user_id, node_id, recipient) VALUES (?, ?, ?)",
				userId, nodeId, recipient);
	}

	private Long nodeOwnerId(Long nodeId) {
		return jdbcOperations.queryForObject("SELECT user_id FROM solaruser.user_node WHERE node_id = ?",
				Long.class, nodeId);
	}

	private List<Map<String, Object>> nodeTransfers(Long nodeId) {
		return jdbcOperations.queryForList("""
				SELECT user_id, node_id, recipient::text AS recipient
				FROM solaruser.user_node_xfer
				WHERE node_id = ?
				""", nodeId);
	}

	@WithMockSecurityUser
	@Test
	public void requestNodeTransfer() throws Exception {
		// GIVEN
		createActorUser();
		final Long nodeId = createUserNode(DEFAULT_USER_ID);
		final String recipient = randomEmail();

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/requestNodeTransfer")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", DEFAULT_USER_ID.toString())
				.param("nodeId", nodeId.toString())
				.param("recipient", recipient)
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(nodeTransfers(nodeId))
			.as("Transfer request created for own node")
			.containsExactly(Map.of("user_id", DEFAULT_USER_ID, "node_id", nodeId, "recipient", recipient))
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void requestNodeTransfer_otherUserNode() throws Exception {
		// GIVEN
		createActorUser();
		final Long ownerId = createUser(randomEmail());
		final Long nodeId = createUserNode(ownerId);

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/requestNodeTransfer")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", DEFAULT_USER_ID.toString())
				.param("nodeId", nodeId.toString())
				.param("recipient", DEFAULT_USERNAME)
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(nodeTransfers(nodeId))
			.as("Transfer request not created for node owned by another user")
			.isEmpty()
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void requestNodeTransfer_asOtherUser() throws Exception {
		// GIVEN
		createActorUser();
		final Long ownerId = createUser(randomEmail());
		final Long nodeId = createUserNode(ownerId);

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/requestNodeTransfer")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", ownerId.toString())
				.param("nodeId", nodeId.toString())
				.param("recipient", DEFAULT_USERNAME)
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(nodeTransfers(nodeId))
			.as("Transfer request not created on behalf of another user")
			.isEmpty()
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void confirmNodeTransfer() throws Exception {
		// GIVEN
		createActorUser();
		final Long ownerId = createUser(randomEmail());
		final Long nodeId = createUserNode(ownerId);
		insertNodeTransfer(ownerId, nodeId, DEFAULT_USERNAME);

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/confirmNodeTransferRequest")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", ownerId.toString())
				.param("nodeId", nodeId.toString())
				.param("accept", "true")
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(nodeOwnerId(nodeId))
			.as("Node transferred to recipient")
			.isEqualTo(DEFAULT_USER_ID)
			;
		then(nodeTransfers(nodeId))
			.as("Transfer request removed")
			.isEmpty()
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void confirmNodeTransfer_forgedRequest() throws Exception {
		// GIVEN
		createActorUser();
		final Long ownerId = createUser(randomEmail());
		final Long nodeId = createUserNode(ownerId);

		// transfer request that claims the actor owns the node
		insertNodeTransfer(DEFAULT_USER_ID, nodeId, DEFAULT_USERNAME);

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/confirmNodeTransferRequest")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", DEFAULT_USER_ID.toString())
				.param("nodeId", nodeId.toString())
				.param("accept", "true")
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(nodeOwnerId(nodeId))
			.as("Node ownership unchanged")
			.isEqualTo(ownerId)
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void confirmNodeTransfer_notRecipient() throws Exception {
		// GIVEN
		createActorUser();
		final Long ownerId = createUser(randomEmail());
		final Long nodeId = createUserNode(ownerId);
		final String recipient = randomEmail();
		createUser(recipient);
		insertNodeTransfer(ownerId, nodeId, recipient);

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/confirmNodeTransferRequest")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", ownerId.toString())
				.param("nodeId", nodeId.toString())
				.param("accept", "true")
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(nodeOwnerId(nodeId))
			.as("Node ownership unchanged")
			.isEqualTo(ownerId)
			;
		then(nodeTransfers(nodeId))
			.as("Transfer request unchanged")
			.containsExactly(Map.of("user_id", ownerId, "node_id", nodeId, "recipient", recipient))
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void cancelNodeTransfer() throws Exception {
		// GIVEN
		createActorUser();
		final Long nodeId = createUserNode(DEFAULT_USER_ID);
		insertNodeTransfer(DEFAULT_USER_ID, nodeId, randomEmail());

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/cancelNodeTransferRequest")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", DEFAULT_USER_ID.toString())
				.param("nodeId", nodeId.toString())
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(nodeTransfers(nodeId))
			.as("Transfer request removed")
			.isEmpty()
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void cancelNodeTransfer_otherUserTransfer() throws Exception {
		// GIVEN
		createActorUser();
		final Long ownerId = createUser(randomEmail());
		final Long nodeId = createUserNode(ownerId);
		final String recipient = randomEmail();
		insertNodeTransfer(ownerId, nodeId, recipient);

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/cancelNodeTransferRequest")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", ownerId.toString())
				.param("nodeId", nodeId.toString())
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(nodeTransfers(nodeId))
			.as("Transfer request unchanged")
			.containsExactly(Map.of("user_id", ownerId, "node_id", nodeId, "recipient", recipient))
			;
		// @formatter:on
	}

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
