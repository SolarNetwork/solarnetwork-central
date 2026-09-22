/* ==================================================================
 * MyNodesController_InvitationWebTests.java - 22/09/2026 8:19:44 am
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

import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNodeConfirmation;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.reg.web.MyNodesController;
import net.solarnetwork.central.test.security.WithMockSecurityUser;

/**
 * Web integration tests for the {@link MyNodesController} class node invitation
 * support.
 *
 * @author matt
 * @version 1.1
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class MyNodesController_InvitationWebTests {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private MockMvc mvc;

	private Long otherUserId;
	private Long otherInvitationId;

	@BeforeEach
	public void setup() {
		insertUser(jdbcOperations, DEFAULT_USER_ID, DEFAULT_USERNAME, randomString(), DEFAULT_NAME);

		otherUserId = randomLong();
		insertUser(jdbcOperations, otherUserId, randomEmail(), randomString(), randomString());
		otherInvitationId = insertUserNodeConfirmation(jdbcOperations, otherUserId);
	}

	private List<Long> invitationIds(Long userId) {
		return jdbcOperations.queryForList(
				"SELECT id FROM solaruser.user_node_conf WHERE user_id = ? ORDER BY id", Long.class,
				userId);
	}

	@WithMockSecurityUser
	@Test
	public void newInvitation() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/new")
				.param("phrase", randomString())
				.param("timeZone", "Pacific/Auckland")
				.param("country", "NZ")
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(invitationIds(DEFAULT_USER_ID))
			.as("Invitation created for active user")
			.hasSize(1)
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void newInvitation_otherUser() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/my-nodes/new")
				.param("userId", otherUserId.toString())
				.param("phrase", randomString())
				.param("timeZone", "Pacific/Auckland")
				.param("country", "NZ")
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(invitationIds(otherUserId))
			.as("Invitation not created for other user")
			.containsExactly(otherInvitationId)
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void viewInvitation() throws Exception {
		// GIVEN
		final Long invitationId = insertUserNodeConfirmation(jdbcOperations, DEFAULT_USER_ID);

		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/my-nodes/invitation")
				.param("id", invitationId.toString())
			)
			.andExpect(status().isOk())
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void viewInvitation_otherUser() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/my-nodes/invitation")
				.param("id", otherInvitationId.toString())
			)
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void cancelInvitation() throws Exception {
		// GIVEN
		final Long invitationId = insertUserNodeConfirmation(jdbcOperations, DEFAULT_USER_ID);

		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/my-nodes/cancelInvitation")
				.param("id", invitationId.toString())
			)
			.andExpect(status().is3xxRedirection())
			;

		// THEN
		then(invitationIds(DEFAULT_USER_ID))
			.as("Invitation cancelled")
			.isEmpty()
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void cancelInvitation_otherUser() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/my-nodes/cancelInvitation")
				.param("id", otherInvitationId.toString())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(invitationIds(otherUserId))
			.as("Other user invitation not cancelled")
			.containsExactly(otherInvitationId)
			;
		// @formatter:on
	}

}
