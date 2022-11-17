/* ==================================================================
 * JdbcChargePointActionStatusDaoTests.java - 18/11/2022 6:15:48 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao.jdbc.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.ocpp.dao.jdbc.JdbcChargePointActionStatusDao;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;

/**
 * Test cases for the {@link JdbcChargePointActionStatusDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcChargePointActionStatusDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private Long TEST_USER_ID = UUID.randomUUID().getMostSignificantBits();
	private Long TEST_NODE_ID = UUID.randomUUID().getMostSignificantBits();
	private Long TEST_CHARGER_ID = UUID.randomUUID().getMostSignificantBits();
	private String TEST_CHARGER_IDENT = UUID.randomUUID().toString();

	private JdbcChargePointActionStatusDao dao;

	@BeforeEach
	public void setup() {
		dao = new JdbcChargePointActionStatusDao(jdbcTemplate);
		setupTestUser(TEST_USER_ID);
		setupTestLocation();
		setupTestNode(TEST_NODE_ID);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID);
		insertCharger(TEST_USER_ID, TEST_NODE_ID, TEST_CHARGER_ID, TEST_CHARGER_IDENT);
	}

	private void insertCharger(Long userId, Long nodeId, Long cpId, String cpIdent) {
		jdbcTemplate.update("""
				INSERT INTO solarev.ocpp_charge_point (id, user_id, node_id, ident, vendor, model)
				VALUES (?, ?, ?, ?, 'Test', 'Test')
				""", cpId, userId, nodeId, cpIdent);
	}

	private void insertChargerActionStatus(Long userId, Long cpId, int connectorId, String action,
			String messageId, Instant ts) {
		jdbcTemplate.update(
				"""
						INSERT INTO solarev.ocpp_charge_point_action_status (user_id, cp_id, conn_id, action, msg_id, ts)
						VALUES (?, ?, ?, ?, ?, ?)
						""",
				userId, cpId, connectorId, action, messageId, Timestamp.from(ts));
	}

	private List<Map<String, Object>> allChargePointActionStatusData() {
		List<Map<String, Object>> data = jdbcTemplate.queryForList(
				"select * from solarev.ocpp_charge_point_action_status ORDER BY user_id, cp_id, conn_id, action");
		log.debug("solarev.ocpp_charge_point_action_status table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	@Test
	public void insert_nullConnector() {
		// GIVEN
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, null, action, messageId, ts);

		// THEN
		List<Map<String, Object>> data = allChargePointActionStatusData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
		assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
		assertThat("Row connector ID matches", row, hasEntry("conn_id", 0));
		assertThat("Row action matches", row.get("action"), is(equalTo(action)));
		assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
		assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));
	}

	@Test
	public void update_nullConnector() {
		// GIVEN
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// insert earlier record
		insertChargerActionStatus(TEST_USER_ID, TEST_CHARGER_ID, 0, action, UUID.randomUUID().toString(),
				ts.minusSeconds(1));

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, null, action, messageId, ts);

		// THEN
		List<Map<String, Object>> data = allChargePointActionStatusData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
		assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
		assertThat("Row connector ID matches", row, hasEntry("conn_id", 0));
		assertThat("Row action matches", row.get("action"), is(equalTo(action)));
		assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
		assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));
	}

	@Test
	public void insert() {
		// GIVEN
		final var connId = 1;
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, connId, action, messageId, ts);

		// THEN
		List<Map<String, Object>> data = allChargePointActionStatusData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
		assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
		assertThat("Row connector ID matches", row, hasEntry("conn_id", connId));
		assertThat("Row action matches", row.get("action"), is(equalTo(action)));
		assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
		assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));
	}

	@Test
	public void update() {
		// GIVEN
		final var connId = 1;
		final var action = "foo";
		final var messageId = UUID.randomUUID().toString();
		final var ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		// insert earlier record
		insertChargerActionStatus(TEST_USER_ID, TEST_CHARGER_ID, connId, action,
				UUID.randomUUID().toString(), ts.minusSeconds(1));

		// WHEN
		dao.updateActionTimestamp(TEST_USER_ID, TEST_CHARGER_IDENT, connId, action, messageId, ts);

		// THEN
		List<Map<String, Object>> data = allChargePointActionStatusData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", TEST_USER_ID));
		assertThat("Row charger ID matches", row, hasEntry("cp_id", TEST_CHARGER_ID));
		assertThat("Row connector ID matches", row, hasEntry("conn_id", connId));
		assertThat("Row action matches", row.get("action"), is(equalTo(action)));
		assertThat("Row message ID matches", row.get("msg_id"), is(equalTo(messageId)));
		assertThat("Row timestamp matches", row.get("ts"), is(equalTo(Timestamp.from(ts))));
	}

}
