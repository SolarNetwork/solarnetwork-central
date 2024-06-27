/* ==================================================================
 * DbDatumFluxAggPubSettingsStoredProcedureTests.java - 26/06/2024 8:47:44 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;

/**
 * Test cases for datum SolarFlux aggregate publish settings database stored
 * procedures.
 *
 * @author matt
 * @version 1.0
 */
public class DbDatumFluxAggPubSettingsStoredProcedureTests extends AbstractJUnit5JdbcDaoTestSupport {

	private Long userId;
	private Long nodeId;

	@BeforeEach
	public void setup() {
		userId = randomLong();
		nodeId = randomLong();
		setupTestUser(userId);
		setupTestLocation();
		setupTestNode(nodeId);
		setupTestUserNode(userId, nodeId);
	}

	@Test
	public void defaultSettings() {
		// GIVEN
		final String sourceId = randomString();

		// WHEN
		var results = jdbcTemplate.queryForList("""
				SELECT *
				FROM solardatm.flux_agg_pub_settings(?, ?)
				""", nodeId, sourceId);

		// THEN
		// @formatter:off
		then(results)
			.as("Result returned")
			.hasSize(1).element(0, map(String.class, Object.class))
			.as("Node ID returned")
			.containsEntry("node_id", nodeId)
			.as("Source ID returned")
			.containsEntry("source_id", sourceId)
			.as("Default publish setting returned")
			.containsEntry("publish", false)
			.as("Default publish setting returned")
			.containsEntry("retain", false)
			;
		// @formatter:on
	}

	@Test
	public void userDefaultSettings() {
		// GIVEN
		jdbcTemplate.update("""
				INSERT INTO solaruser.user_flux_default_agg_pub_settings (user_id, publish, retain)
				VALUES (?, ?, ?)
				""", userId, true, true);

		final String sourceId = randomString();

		// WHEN
		var results = jdbcTemplate.queryForList("""
				SELECT *
				FROM solardatm.flux_agg_pub_settings(?, ?)
				""", nodeId, sourceId);

		// THEN
		// @formatter:off
		then(results)
			.as("Result returned")
			.hasSize(1).element(0, map(String.class, Object.class))
			.as("Node ID returned")
			.containsEntry("node_id", nodeId)
			.as("Source ID returned")
			.containsEntry("source_id", sourceId)
			.as("User default publish setting returned")
			.containsEntry("publish", true)
			.as("User default publish setting returned")
			.containsEntry("retain", true)
			;
		// @formatter:on
	}

	@Test
	public void streamSettings() {
		// GIVEN
		final String sourceId = randomString();

		jdbcTemplate.update(
				"""
						INSERT INTO solaruser.user_flux_agg_pub_settings (user_id, node_ids, source_ids, publish, retain)
						VALUES (?, ARRAY[?::BIGINT], ARRAY[?::CHARACTER VARYING], ?, ?)
						""",
				userId, nodeId, sourceId, true, true);

		// WHEN
		var results = jdbcTemplate.queryForList("""
				SELECT *
				FROM solardatm.flux_agg_pub_settings(?, ?)
				""", nodeId, sourceId);

		// THEN
		// @formatter:off
		then(results)
			.as("Result returned")
			.hasSize(1).element(0, map(String.class, Object.class))
			.as("Node ID returned")
			.containsEntry("node_id", nodeId)
			.as("Source ID returned")
			.containsEntry("source_id", sourceId)
			.as("User publish setting returned")
			.containsEntry("publish", true)
			.as("User publish setting returned")
			.containsEntry("retain", true)
			;
		// @formatter:on
	}

	@Test
	public void streamSettings_overrideUserDefault() {
		// GIVEN
		final String sourceId = randomString();

		jdbcTemplate.update("""
				INSERT INTO solaruser.user_flux_default_agg_pub_settings (user_id, publish, retain)
				VALUES (?, ?, ?)
				""", userId, true, false);

		jdbcTemplate.update(
				"""
						INSERT INTO solaruser.user_flux_agg_pub_settings (user_id, node_ids, source_ids, publish, retain)
						VALUES (?, ARRAY[?::BIGINT], ARRAY[?::CHARACTER VARYING], ?, ?)
						""",
				userId, nodeId, sourceId, false, true);

		// WHEN
		var results = jdbcTemplate.queryForList("""
				SELECT *
				FROM solardatm.flux_agg_pub_settings(?, ?)
				""", nodeId, sourceId);

		// THEN
		// @formatter:off
		then(results)
			.as("Result returned")
			.hasSize(1).element(0, map(String.class, Object.class))
			.as("Node ID returned")
			.containsEntry("node_id", nodeId)
			.as("Source ID returned")
			.containsEntry("source_id", sourceId)
			.as("User publish setting returned, overriding user default")
			.containsEntry("publish", false)
			.as("User publish setting returned, overriding user default")
			.containsEntry("retain", true)
			;
		// @formatter:on
	}

	@Test
	public void streamSettings_merged() {
		// GIVEN
		final String sourceId = randomString();

		jdbcTemplate.update(
				"""
						INSERT INTO solaruser.user_flux_agg_pub_settings (user_id, node_ids, source_ids, publish, retain)
						VALUES (?, ARRAY[?::BIGINT], ARRAY[?::CHARACTER VARYING], ?, ?)
						""",
				userId, nodeId, sourceId, false, false);

		jdbcTemplate.update(
				"""
						INSERT INTO solaruser.user_flux_agg_pub_settings (user_id, node_ids, source_ids, publish, retain)
						VALUES (?, ARRAY[?::BIGINT], ARRAY[?::CHARACTER VARYING], ?, ?)
						""",
				userId, nodeId, sourceId, true, true);

		// WHEN
		var results = jdbcTemplate.queryForList("""
				SELECT *
				FROM solardatm.flux_agg_pub_settings(?, ?)
				""", nodeId, sourceId);

		// THEN
		// @formatter:off
		then(results)
			.as("Result returned")
			.hasSize(1).element(0, map(String.class, Object.class))
			.as("Node ID returned")
			.containsEntry("node_id", nodeId)
			.as("Source ID returned")
			.containsEntry("source_id", sourceId)
			.as("Highest user publish setting returned")
			.containsEntry("publish", true)
			.as("Hightest user publish setting returned")
			.containsEntry("retain", true)
			;
		// @formatter:on
	}

}
