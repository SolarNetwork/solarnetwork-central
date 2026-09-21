/* ==================================================================
 * MyNodesController_UserNodeWebTests.java - 22/09/2026 8:48:15 am
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
import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertNode;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.central.test.CommonTestUtils.randomEmail;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_NAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USERNAME;
import static net.solarnetwork.central.test.security.WithMockSecurityUser.DEFAULT_USER_ID;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.reg.web.MyNodesController;
import net.solarnetwork.central.test.security.WithMockSecurityUser;

/**
 * Web integration tests for the {@link MyNodesController} class user node
 * support.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MyNodesController_UserNodeWebTests {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private MockMvc mvc;

	private Long nodeId;
	private Long otherUserId;
	private Long otherNodeId;
	private String otherNodeName;

	@BeforeEach
	public void setup() {
		final Long locId = insertLocation(jdbcOperations, "NZ", "Pacific/Auckland");

		insertUser(jdbcOperations, DEFAULT_USER_ID, DEFAULT_USERNAME, randomString(), DEFAULT_NAME);
		nodeId = insertNode(jdbcOperations, locId);
		insertUserNode(jdbcOperations, DEFAULT_USER_ID, nodeId, true);

		// another user's public node
		otherUserId = randomLong();
		insertUser(jdbcOperations, otherUserId, randomEmail(), randomString(), randomString());
		otherNodeId = insertNode(jdbcOperations, locId);
		otherNodeName = randomString();
		insertUserNode(jdbcOperations, otherUserId, otherNodeId, otherNodeName, null, false, false);
	}

	private Map<String, Object> userNodeRow(Long nodeId) {
		return jdbcOperations.queryForMap("""
				SELECT user_id, disp_name, private
				FROM solaruser.user_node
				WHERE node_id = ?
				""", nodeId);
	}

	@WithMockSecurityUser
	@Test
	public void getUserNode() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/my-nodes/node")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", DEFAULT_USER_ID.toString())
				.param("nodeId", nodeId.toString())
			)
			.andExpect(status().isOk())
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void getUserNode_otherUserPublicNode() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/my-nodes/node")
				.accept(MediaType.APPLICATION_JSON)
				.param("userId", otherUserId.toString())
				.param("nodeId", otherNodeId.toString())
			)
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void updateNode() throws Exception {
		// GIVEN
		final String name = randomString();

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/updateNode")
				.accept(MediaType.APPLICATION_JSON)
				.param("node.id", nodeId.toString())
				.param("user.id", DEFAULT_USER_ID.toString())
				.param("name", name)
				.param("requiresAuthorization", "true")
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(userNodeRow(nodeId))
			.as("Own node updated")
			.contains(entry("user_id", DEFAULT_USER_ID), entry("disp_name", name))
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void updateNode_otherUserPublicNode() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/updateNode")
				.accept(MediaType.APPLICATION_JSON)
				.param("node.id", otherNodeId.toString())
				.param("user.id", DEFAULT_USER_ID.toString())
				.param("name", randomString())
				.param("requiresAuthorization", "true")
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(userNodeRow(otherNodeId))
			.as("Other user public node unchanged")
			.containsOnly(entry("user_id", otherUserId), entry("disp_name", otherNodeName),
					entry("private", false))
			;
		// @formatter:on
	}

}
