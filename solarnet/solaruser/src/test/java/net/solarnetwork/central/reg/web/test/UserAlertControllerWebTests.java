/* ==================================================================
 * UserAlertControllerWebTests.java - 22/09/2026 8:02:41 am
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
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
import net.solarnetwork.central.reg.web.UserAlertController;
import net.solarnetwork.central.test.security.WithMockSecurityUser;

/**
 * Web integration tests for the {@link UserAlertController} class.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserAlertControllerWebTests {

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private MockMvc mvc;

	private Long nodeId;
	private Long otherUserId;
	private Long otherNodeId;
	private Long otherAlertId;

	@BeforeEach
	public void setup() {
		final Long locId = insertLocation(jdbcOperations, "NZ", "Pacific/Auckland");

		insertUser(jdbcOperations, DEFAULT_USER_ID, DEFAULT_USERNAME, randomString(), DEFAULT_NAME);
		nodeId = insertNode(jdbcOperations, locId);
		insertUserNode(jdbcOperations, DEFAULT_USER_ID, nodeId, true);

		otherUserId = randomLong();
		insertUser(jdbcOperations, otherUserId, randomEmail(), randomString(), randomString());
		otherNodeId = insertNode(jdbcOperations, locId);
		insertUserNode(jdbcOperations, otherUserId, otherNodeId, true);
		otherAlertId = insertAlert(otherUserId, otherNodeId);
		insertAlertSituation(otherAlertId);
	}

	private Long insertAlert(Long userId, Long nodeId) {
		return jdbcOperations.queryForObject("""
				INSERT INTO solaruser.user_alert (user_id, node_id, alert_type, status, alert_opt)
				VALUES (?, ?, 'NodeStaleData'::solaruser.user_alert_type
					, 'Active'::solaruser.user_alert_status, '{"age":1800}'::json)
				RETURNING id
				""", Long.class, userId, nodeId);
	}

	private void insertAlertSituation(Long alertId) {
		jdbcOperations.update("""
				INSERT INTO solaruser.user_alert_sit (alert_id, status)
				VALUES (?, 'Active'::solaruser.user_alert_sit_status)
				""", alertId);
	}

	private List<Map<String, Object>> alerts(Long nodeId) {
		return jdbcOperations.queryForList("""
				SELECT id, user_id, node_id
				FROM solaruser.user_alert
				WHERE node_id = ?
				""", nodeId);
	}

	private List<String> alertSituationStatuses(Long alertId) {
		return jdbcOperations.queryForList(
				"SELECT status::text FROM solaruser.user_alert_sit WHERE alert_id = ?", String.class,
				alertId);
	}

	private static String alertJson(Long alertId, Long nodeId) {
		return """
				{
					%s"nodeId":%d,
					"type":"NodeStaleData",
					"status":"Active",
					"options":{"ageMinutes":30}
				}
				""".formatted(alertId != null ? "\"id\":%d,".formatted(alertId) : "", nodeId);
	}

	@WithMockSecurityUser
	@Test
	public void viewSituation() throws Exception {
		// GIVEN
		final Long alertId = insertAlert(DEFAULT_USER_ID, nodeId);
		insertAlertSituation(alertId);

		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/alerts/situation/{alertId}", alertId)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void viewSituation_otherUserAlert() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/alerts/situation/{alertId}", otherAlertId)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void resolveSituation() throws Exception {
		// GIVEN
		final Long alertId = insertAlert(DEFAULT_USER_ID, nodeId);
		insertAlertSituation(alertId);

		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/alerts/situation/{alertId}/resolve", alertId)
				.accept(MediaType.APPLICATION_JSON)
				.param("status", "Resolved")
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(alertSituationStatuses(alertId))
			.as("Situation resolved")
			.containsExactly("Resolved")
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void resolveSituation_otherUserAlert() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/alerts/situation/{alertId}/resolve", otherAlertId)
				.accept(MediaType.APPLICATION_JSON)
				.param("status", "Resolved")
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(alertSituationStatuses(otherAlertId))
			.as("Other user situation unchanged")
			.containsExactly("Active")
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void deleteAlert() throws Exception {
		// GIVEN
		final Long alertId = insertAlert(DEFAULT_USER_ID, nodeId);
		final Long keepAlertId = insertAlert(DEFAULT_USER_ID, nodeId);

		// WHEN
		// @formatter:off
		mvc.perform(delete("/u/sec/alerts/{alertId}", alertId)
				.accept(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(alerts(nodeId))
			.as("Only the given alert deleted")
			.extracting(m -> m.get("id"))
			.containsExactly(keepAlertId)
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void deleteAlert_otherUserAlert() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(delete("/u/sec/alerts/{alertId}", otherAlertId)
				.accept(MediaType.APPLICATION_JSON)
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(alerts(otherNodeId))
			.as("Other user alert not deleted")
			.containsExactly(Map.of("id", otherAlertId, "user_id", otherUserId, "node_id", otherNodeId))
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void nodeSituations() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/alerts/node/{nodeId}/situations", nodeId)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void nodeSituations_otherUserNode() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(get("/u/sec/alerts/node/{nodeId}/situations", otherNodeId)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isForbidden())
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void saveAlert() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/alerts/save")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(alertJson(null, nodeId))
				.with(csrf())
			)
			.andExpect(status().isOk())
			;

		// THEN
		then(alerts(nodeId))
			.as("Alert created for own node")
			.singleElement()
			.satisfies(m -> then(m).contains(entry("user_id", DEFAULT_USER_ID), entry("node_id", nodeId)))
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void saveAlert_otherUserAlert() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/alerts/save")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(alertJson(otherAlertId, nodeId))
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(alerts(otherNodeId))
			.as("Other user alert unchanged")
			.containsExactly(Map.of("id", otherAlertId, "user_id", otherUserId, "node_id", otherNodeId))
			;
		then(alerts(nodeId))
			.as("Other user alert not moved to own node")
			.isEmpty()
			;
		// @formatter:on
	}

	@WithMockSecurityUser
	@Test
	public void saveAlert_otherUserNode() throws Exception {
		// WHEN
		// @formatter:off
		mvc.perform(post("/u/sec/alerts/save")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(alertJson(null, otherNodeId))
				.with(csrf())
			)
			.andExpect(status().isForbidden())
			;

		// THEN
		then(alerts(otherNodeId))
			.as("No alert created for other user node")
			.containsExactly(Map.of("id", otherAlertId, "user_id", otherUserId, "node_id", otherNodeId))
			;
		// @formatter:on
	}

}
